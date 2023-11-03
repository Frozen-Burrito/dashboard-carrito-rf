package com.fernandomendoza.dashboardcarrorf.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

internal const val SETTINGS_SCREEN_ROUTE = "settings"

@Composable
internal fun SettingsScreen(
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Ajustes", modifier = Modifier.padding(16.dp)
    )
}

fun NavGraphBuilder.settingsScreen(
    modifier: Modifier = Modifier
) {
    composable(route = SETTINGS_SCREEN_ROUTE) {

        SettingsScreen(
            modifier
        )
    }
}

fun NavController.navigateToSettingsScreen() {
    navigate(SETTINGS_SCREEN_ROUTE) { launchSingleTop = true }
}