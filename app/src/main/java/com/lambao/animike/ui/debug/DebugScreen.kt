package com.lambao.animike.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.animike.data.repository.TableStat
import com.lambao.animike.debug.AppLogEntry
import com.lambao.animike.debug.AppLogLevel
import com.lambao.animike.debug.NetworkLogEntry
import com.lambao.animike.ui.components.BackButton
import com.lambao.animike.ui.theme.Dimens
import com.lambao.animike.ui.theme.success
import com.lambao.animike.ui.theme.warning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebugScreen(
    onBackClick: () -> Unit,
    onNavigateToNetworkDetail: (Long) -> Unit,
    onNavigateToTableDetail: (String) -> Unit,
    viewModel: DebugViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DebugEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.text)
                is DebugEffect.NavigateToNetworkDetail -> onNavigateToNetworkDetail(effect.id)
                is DebugEffect.NavigateToTableDetail -> onNavigateToTableDetail(effect.name)
            }
        }
    }

    DebugScreenContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun DebugScreenContent(
    state: DebugState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onEvent: (DebugEvent) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // AniMikeNavHost đã có Scaffold ngoài tiêu thụ insets — tránh pad 2 lần
        // quanh status bar (cùng lý do DetailScreen).
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.SpaceSm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                BackButton(onClick = onBackClick)
                Text(
                    text = "Debug",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            val tabs = DebugTab.entries
            TabRow(
                selectedTabIndex = tabs.indexOf(state.selectedTab),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = tab == state.selectedTab,
                        onClick = { onEvent(DebugEvent.OnTabSelected(tab)) },
                        text = { Text(tab.title) },
                    )
                }
            }

            when (state.selectedTab) {
                DebugTab.NETWORK -> NetworkTab(
                    logs = state.networkLogs,
                    query = state.networkQuery,
                    onEvent = onEvent,
                )
                DebugTab.LOG -> LogTab(
                    logs = state.appLogs,
                    query = state.logQuery,
                    onEvent = onEvent,
                )
                DebugTab.CACHE -> CacheTab(
                    tableStats = state.tableStats,
                    query = state.cacheQuery,
                    isLoadingStats = state.isLoadingStats,
                    onEvent = onEvent,
                )
            }
        }
    }
}

private val DebugTab.title: String
    get() = when (this) {
        DebugTab.NETWORK -> "API"
        DebugTab.LOG -> "Log local"
        DebugTab.CACHE -> "Cache"
    }

// ── Tab API ──────────────────────────────────────────────────────────────

@Composable
private fun NetworkTab(logs: List<NetworkLogEntry>, query: String, onEvent: (DebugEvent) -> Unit) {
    val filtered = remember(logs, query) {
        if (query.isBlank()) {
            logs
        } else {
            logs.filter {
                it.url.contains(query, ignoreCase = true) ||
                    it.method.contains(query, ignoreCase = true) ||
                    it.code?.toString()?.contains(query) == true
            }
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        TabActionBar(
            countLabel = "${filtered.size}/${logs.size} request",
            onClear = { onEvent(DebugEvent.OnClearNetworkLogs) },
        )
        DebugSearchBar(
            query = query,
            onQueryChange = { onEvent(DebugEvent.OnNetworkQueryChange(it)) },
            placeholder = "Lọc theo URL / method / code",
        )
        if (filtered.isEmpty()) {
            EmptyHint(if (logs.isEmpty()) "Chưa có request nào — thao tác trong app rồi quay lại đây." else "Không có request khớp.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                items(filtered, key = { it.id }) { entry ->
                    NetworkLogCard(entry = entry, onClick = { onEvent(DebugEvent.OnNetworkLogClick(entry.id)) })
                }
            }
        }
    }
}

// List chỉ hiện overview 1 dòng — bấm mở màn chi tiết (headers/body/cURL, Đợt 2).
@Composable
private fun NetworkLogCard(entry: NetworkLogEntry, onClick: () -> Unit) {
    val statusColor = when {
        entry.error != null -> MaterialTheme.colorScheme.error
        entry.isSuccess -> MaterialTheme.colorScheme.success
        else -> MaterialTheme.colorScheme.error
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusCard))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClickLabel = "Xem chi tiết request", role = Role.Button, onClick = onClick)
            .padding(Dimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entry.code?.toString() ?: "ERR",
                style = MaterialTheme.typography.labelLarge,
                color = statusColor,
            )
            Text(
                text = entry.method,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${entry.durationMs}ms",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = timeFormat.format(Date(entry.timestampMs)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = entry.url,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Tab Log local ────────────────────────────────────────────────────────

@Composable
private fun LogTab(logs: List<AppLogEntry>, query: String, onEvent: (DebugEvent) -> Unit) {
    val filtered = remember(logs, query) {
        if (query.isBlank()) {
            logs
        } else {
            logs.filter {
                it.tag.contains(query, ignoreCase = true) ||
                    it.message.contains(query, ignoreCase = true) ||
                    it.level.name.contains(query, ignoreCase = true)
            }
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        TabActionBar(
            countLabel = "${filtered.size}/${logs.size} dòng",
            onClear = { onEvent(DebugEvent.OnClearAppLogs) },
        )
        DebugSearchBar(
            query = query,
            onQueryChange = { onEvent(DebugEvent.OnLogQueryChange(it)) },
            placeholder = "Lọc theo tag / message / level",
        )
        if (filtered.isEmpty()) {
            EmptyHint(if (logs.isEmpty()) "Chưa có log nào." else "Không có log khớp.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                items(filtered, key = { it.id }) { entry -> AppLogCard(entry) }
            }
        }
    }
}

@Composable
private fun AppLogCard(entry: AppLogEntry) {
    var expanded by remember(entry.id) { mutableStateOf(false) }
    val levelColor = when (entry.level) {
        AppLogLevel.ERROR -> MaterialTheme.colorScheme.error
        AppLogLevel.WARN -> MaterialTheme.colorScheme.warning
        AppLogLevel.INFO -> MaterialTheme.colorScheme.primary
        AppLogLevel.DEBUG, AppLogLevel.VERBOSE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val hasStack = entry.stackTrace != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusCard))
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (hasStack) {
                    Modifier.clickable(
                        onClickLabel = if (expanded) "Thu gọn" else "Xem stack trace",
                        role = Role.Button,
                    ) { expanded = !expanded }
                } else {
                    Modifier
                },
            )
            .padding(Dimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entry.level.name.first().toString(),
                style = MaterialTheme.typography.labelLarge,
                color = levelColor,
            )
            Text(
                text = entry.tag,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = timeFormat.format(Date(entry.timestampMs)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (expanded) {
            entry.stackTrace?.let { CodeBlock(label = "Stack trace", text = it) }
        }
    }
}

// ── Tab Cache ────────────────────────────────────────────────────────────

// Nhận field lẻ (không cả DebugState) — đứng ở tab Cache mà interceptor vẫn
// ghi networkLogs liên tục sẽ đẩy DebugState mới; truyền cả state khiến Cache
// recompose oan mỗi request mạng (góp ý review).
@Composable
private fun CacheTab(
    tableStats: List<TableStat>,
    query: String,
    isLoadingStats: Boolean,
    onEvent: (DebugEvent) -> Unit,
) {
    // Bảng đang chờ xác nhận xoá (null = không có dialog) — state UI thuần.
    var pendingClear by remember { mutableStateOf<TableStat?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = { onEvent(DebugEvent.OnClearCache) }) { Text("Xoá cache") }
            TextButton(onClick = { onEvent(DebugEvent.OnRefreshStats) }) { Text("Làm mới") }
        }
        Text(
            text = "\"Xoá cache\" giữ lại favorite/tracking (dữ liệu bạn). Bấm 1 bảng để xem dữ liệu.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
        )
        DebugSearchBar(
            query = query,
            onQueryChange = { onEvent(DebugEvent.OnCacheQueryChange(it)) },
            placeholder = "Lọc theo tên bảng",
        )
        val filtered = remember(tableStats, query) {
            if (query.isBlank()) {
                tableStats
            } else {
                tableStats.filter { it.name.contains(query, ignoreCase = true) }
            }
        }
        // Chỉ hiện spinner khi CHƯA có dữ liệu nào (lần load đầu) — các lần
        // refresh sau đã có bảng cũ để xem, không chớp spinner gây nhấp nháy.
        if (isLoadingStats && tableStats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
            ) {
                items(filtered, key = { it.name }) { stat ->
                    TableStatRow(
                        stat = stat,
                        onRowClick = { onEvent(DebugEvent.OnTableClick(stat.name)) },
                        onClearClick = { pendingClear = stat },
                    )
                }
            }
        }
    }

    pendingClear?.let { stat ->
        AlertDialog(
            onDismissRequest = { pendingClear = null },
            title = { Text("Xoá bảng ${stat.name}?") },
            text = {
                Text(
                    if (stat.isUserData) {
                        "Đây là DỮ LIỆU USER THẬT (${stat.rowCount} dòng) — xoá là mất, không khôi phục được."
                    } else {
                        "Xoá ${stat.rowCount} dòng cache của bảng này."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onEvent(DebugEvent.OnClearTable(stat.name))
                    pendingClear = null
                }) { Text("Xoá") }
            },
            dismissButton = {
                TextButton(onClick = { pendingClear = null }) { Text("Huỷ") }
            },
        )
    }
}

@Composable
private fun TableStatRow(stat: TableStat, onRowClick: () -> Unit, onClearClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusChip))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClickLabel = "Xem dữ liệu bảng ${stat.name}", role = Role.Button, onClick = onRowClick)
            .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        Text(
            text = stat.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (stat.isUserData) MaterialTheme.colorScheme.warning else MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stat.rowCount.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onClearClick, enabled = stat.rowCount > 0) { Text("Xoá") }
    }
}

// Ô search dùng chung 3 tab — OutlinedTextField 1 dòng, có nút xoá nhanh khi
// đang có từ khoá.
@Composable
private fun DebugSearchBar(query: String, onQueryChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceXs),
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        trailingIcon = {
            if (query.isNotEmpty()) {
                TextButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.semantics { contentDescription = "Xoá từ khoá" },
                ) { Text("✕", modifier = Modifier.clearAndSetSemantics {}) }
            }
        },
        textStyle = MaterialTheme.typography.bodyMedium,
    )
}

// ── Dùng chung ───────────────────────────────────────────────────────────

@Composable
private fun TabActionBar(countLabel: String, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = countLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onClear) { Text("Xoá log") }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.ScreenPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CodeBlock(label: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusChip))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Dimens.SpaceSm),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// Định dạng giờ:phút:giây.mili — remember-free (đọc-only, dùng ở nhiều item);
// Locale.US ép cố định để không phụ thuộc cấu hình máy.
private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
