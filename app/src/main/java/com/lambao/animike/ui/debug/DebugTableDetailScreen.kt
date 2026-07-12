package com.lambao.animike.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.animike.data.repository.ColumnInfo
import com.lambao.animike.data.repository.TableDetail
import com.lambao.animike.ui.components.BackButton
import com.lambao.animike.ui.theme.Dimens

@Composable
fun DebugTableDetailScreen(
    onBackClick: () -> Unit,
    viewModel: DebugTableDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                text = state.tableName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val detail = state.detail
        when {
            state.isLoading && detail == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.error != null && detail == null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = state.error ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = { viewModel.onEvent(DebugTableDetailEvent.OnRetry) }) { Text("Thử lại") }
            }

            detail != null -> TableDetailBody(detail)
        }
    }
}

@Composable
private fun TableDetailBody(detail: TableDetail) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        item { SectionLabel("Schema (${detail.columns.size} cột)") }
        item { SchemaBlock(detail.columns) }

        item {
            SectionLabel(
                if (detail.rows.isEmpty()) "Không có dòng nào" else "Dòng mẫu (${detail.rows.size})",
            )
        }
        // key = index: rows load one-shot, tĩnh, không reorder — index ổn định.
        itemsIndexed(detail.rows, key = { index, _ -> index }) { index, row ->
            RowCard(index = index, columns = detail.columns, values = row)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun SchemaBlock(columns: List<ColumnInfo>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusChip))
            .background(MaterialTheme.colorScheme.surface)
            .padding(Dimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
    ) {
        columns.forEach { col ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                Text(
                    text = col.name + if (col.primaryKey) " 🔑" else "",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = col.type,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RowCard(index: Int, columns: List<ColumnInfo>, values: List<String?>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusCard))
            .background(MaterialTheme.colorScheme.surface)
            .padding(Dimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
    ) {
        Text(
            text = "#${index + 1}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        columns.forEachIndexed { i, col ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                Text(
                    text = col.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.35f),
                )
                Text(
                    // Cắt maxLines để cột chứa JSON/blob dài không chiếm cả màn;
                    // vẫn đủ soi hình dạng dữ liệu đang cache.
                    text = values.getOrNull(i) ?: "null",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = if (values.getOrNull(i) == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(0.65f),
                )
            }
        }
    }
}
