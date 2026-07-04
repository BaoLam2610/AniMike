package com.lambao.animike.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.lambao.animike.ui.schedules.SchedulesScreen
import com.lambao.animike.ui.seasonarchive.SeasonArchiveScreen
import com.lambao.animike.ui.theme.Dimens

// Gộp Season Archive + Schedules vào 1 tab "Duyệt" (segmented control) thay vì
// mỗi tính năng 1 tab riêng — giữ đúng giới hạn 3-4 tab của animike-design
// SKILL.md (mục Bottom navigation). Mỗi tab con vẫn có ViewModel/state riêng
// (SeasonArchiveScreen/SchedulesScreen tự gọi hiltViewModel()), tab được chọn
// chỉ là UI state cục bộ, không cần MVI contract riêng.
private enum class BrowseTab { SEASON, SCHEDULE }

@Composable
fun BrowseScreen(onNavigateToDetail: (Int) -> Unit) {
    var selectedTab by rememberSaveable { mutableStateOf(BrowseTab.SEASON) }

    // contentWindowInsets = 0: AniMikeNavHost đã có Scaffold ngoài tiêu thụ
    // insets cho bottom nav — tránh tiêu thụ 2 lần gây khoảng trắng dư.
    Scaffold(modifier = Modifier.fillMaxSize(), contentWindowInsets = WindowInsets(0)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Text(
                text = "Duyệt",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(Dimens.ScreenPadding),
            )

            BrowseTabRow(selectedTab = selectedTab, onTabSelected = { selectedTab = it })

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    BrowseTab.SEASON -> SeasonArchiveScreen(onNavigateToDetail = onNavigateToDetail)
                    BrowseTab.SCHEDULE -> SchedulesScreen(onNavigateToDetail = onNavigateToDetail)
                }
            }
        }
    }
}

@Composable
private fun BrowseTabRow(selectedTab: BrowseTab, onTabSelected: (BrowseTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceXs),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        BrowseTabChip(
            label = "Theo mùa",
            selected = selectedTab == BrowseTab.SEASON,
            onClick = { onTabSelected(BrowseTab.SEASON) },
            modifier = Modifier.weight(1f),
        )
        BrowseTabChip(
            label = "Theo thứ",
            selected = selectedTab == BrowseTab.SCHEDULE,
            onClick = { onTabSelected(BrowseTab.SCHEDULE) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BrowseTabChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.RadiusButton))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.SpaceSm),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
