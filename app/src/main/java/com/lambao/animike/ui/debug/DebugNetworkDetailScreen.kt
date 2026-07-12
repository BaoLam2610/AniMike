package com.lambao.animike.ui.debug

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.animike.debug.NetworkLogEntry
import com.lambao.animike.debug.buildCurl
import com.lambao.animike.debug.prettyJsonOrRaw
import com.lambao.animike.ui.components.BackButton
import com.lambao.animike.ui.theme.Dimens
import com.lambao.animike.ui.theme.success
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebugNetworkDetailScreen(
    onBackClick: () -> Unit,
    viewModel: DebugNetworkDetailViewModel = hiltViewModel(),
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
                text = "Chi tiết request",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        val entry = state.entry
        when {
            entry != null -> NetworkDetailBody(entry)
            state.resolved -> Text(
                text = "Log đã bị xoá hoặc không còn trong bộ nhớ.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Dimens.ScreenPadding),
            )
            // Chưa resolve (rất nhanh) — không cần spinner, để trống thoáng qua.
            else -> Unit
        }
    }
}

@Composable
private fun NetworkDetailBody(entry: NetworkLogEntry) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    fun copy(text: String, label: String) {
        clipboard.setText(AnnotatedString(text))
        Toast.makeText(context, "Đã copy $label", Toast.LENGTH_SHORT).show()
    }

    val statusColor = when {
        entry.error != null -> MaterialTheme.colorScheme.error
        entry.isSuccess -> MaterialTheme.colorScheme.success
        else -> MaterialTheme.colorScheme.error
    }

    // remember theo id: parse+re-encode JSON (tới ~8KB) và dựng cURL là việc
    // nặng, không chạy lại mỗi recomposition (góp ý review).
    val prettyRequest = remember(entry.id) { entry.requestBody?.let(::prettyJsonOrRaw) }
    val prettyResponse = remember(entry.id) { entry.responseBody?.let(::prettyJsonOrRaw) }
    val curl = remember(entry.id) { buildCurl(entry) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        // Overview
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
            Text(
                text = "${entry.code ?: "ERR"} · ${entry.method} · ${entry.durationMs}ms",
                style = MaterialTheme.typography.titleMedium,
                color = statusColor,
            )
            Text(
                text = timeFormatDetail.format(Date(entry.timestampMs)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = entry.url,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            OutlinedButton(onClick = { copy(curl, "cURL") }) { Text("Copy cURL") }
            prettyResponse?.let { body ->
                OutlinedButton(onClick = { copy(body, "response") }) { Text("Copy body") }
            }
        }

        entry.error?.let { MonoSection(label = "Lỗi", text = it) }

        HeaderSection(label = "Request headers", headers = entry.requestHeaders)
        prettyRequest?.let { MonoSection(label = "Request body", text = it) }

        HeaderSection(label = "Response headers", headers = entry.responseHeaders)
        prettyResponse?.let { MonoSection(label = "Response body", text = it) }
    }
}

@Composable
private fun HeaderSection(label: String, headers: List<Pair<String, String>>) {
    if (headers.isEmpty()) return
    MonoSection(
        label = label,
        text = headers.joinToString("\n") { "${it.first}: ${it.second}" },
    )
}

@Composable
private fun MonoSection(label: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.RadiusChip))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(Dimens.SpaceSm),
        )
    }
}

private val timeFormatDetail = SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS", Locale.US)
