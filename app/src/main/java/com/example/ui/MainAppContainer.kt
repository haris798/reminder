package com.example.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.SupabaseSettingsManager

enum class AppTab(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard, "tab_dashboard"),
    RIWAYAT("Riwayat", Icons.Default.History, "tab_riwayat"),
    PENGATURAN("Pengaturan", Icons.Default.Settings, "tab_pengaturan"),
    DEBUG("Log Debug", Icons.Default.BugReport, "tab_debug")
}

@Composable
fun MainAppContainer(
    viewModel: HydrationViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsManager = remember { SupabaseSettingsManager(context) }
    val supabaseConfig by settingsManager.configState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 8.dp
            ) {
                AppTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (selectedTab) {
            0 -> DashboardScreen(
                viewModel = viewModel,
                isSupabaseConnected = supabaseConfig.isConnected,
                modifier = contentModifier
            )
            1 -> HistoryScreen(viewModel = viewModel, modifier = contentModifier)
            2 -> SettingsScreen(
                settingsManager = settingsManager,
                viewModel = viewModel,
                modifier = contentModifier
            )
            3 -> DebugLogScreen(
                viewModel = viewModel,
                modifier = contentModifier
            )
        }
    }
}
