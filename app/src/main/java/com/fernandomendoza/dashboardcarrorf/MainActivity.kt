package com.fernandomendoza.dashboardcarrorf

import android.bluetooth.BluetoothManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.fernandomendoza.dashboardcarrorf.datasources.BluetoothDeviceDataSource
import com.fernandomendoza.dashboardcarrorf.repository.DashboardRepository
import com.fernandomendoza.dashboardcarrorf.repository.DeviceRepository
import com.fernandomendoza.dashboardcarrorf.ui.HOME_SCREEN_ROUTE
import com.fernandomendoza.dashboardcarrorf.ui.connectionsScreen
import com.fernandomendoza.dashboardcarrorf.ui.homeScreen
import com.fernandomendoza.dashboardcarrorf.ui.navigateToConnectionsScreen
import com.fernandomendoza.dashboardcarrorf.ui.navigateToHome
import com.fernandomendoza.dashboardcarrorf.ui.navigateToSettingsScreen
import com.fernandomendoza.dashboardcarrorf.ui.settingsScreen
import com.fernandomendoza.dashboardcarrorf.ui.theme.DashboardCarroRFTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bluetoothManager: BluetoothManager? = getSystemService(BluetoothManager::class.java)
        val btDeviceDataSource =
            BluetoothDeviceDataSource(bluetoothManager?.adapter, scanIntervalMs = 30_000)

        val deviceRepository = DeviceRepository(btDeviceDataSource)
        val dashboardRepository = DashboardRepository(btDeviceDataSource)

        setContent {
            DashboardCarroRFTheme {
                val navController = rememberNavController()

                var selectedItemIndex: Int by rememberSaveable { mutableStateOf(0) }

                Scaffold(bottomBar = {
                    CustomBottomAppBar(
                        activeScreenIndex = selectedItemIndex,
                        onNavigateToHomeScreen = {
                            selectedItemIndex = 0
                            navController.navigateToHome()
                         },
                        onNavigateToConnectionsScreen = {
                            selectedItemIndex = 1
                            navController.navigateToConnectionsScreen()
                        },
                        onNavigateToSettingsScreen = {
                            selectedItemIndex = 2
                            navController.navigateToSettingsScreen()
                        }
                    )
                }) { contentPadding ->
                    NavHost(
                        navController = navController, startDestination = HOME_SCREEN_ROUTE
                    ) {
                        homeScreen(
                            dashboardRepository,
                            onNavigateToConnections = {
                                selectedItemIndex = 1
                                navController.navigateToConnectionsScreen()
                            },
                            modifier = Modifier
                                .padding(contentPadding)
                                .fillMaxSize()
                        )

                        connectionsScreen(
                            deviceRepository,
                            modifier = Modifier
                                .padding(contentPadding)
                                .fillMaxSize()
                        )

                        settingsScreen(
                            modifier = Modifier
                                .padding(contentPadding)
                                .fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomBottomAppBar(
    activeScreenIndex: Int,
    onNavigateToHomeScreen: () -> Unit,
    onNavigateToConnectionsScreen: () -> Unit,
    onNavigateToSettingsScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    BottomAppBar(modifier) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            IconButton(
                onClick = onNavigateToHomeScreen,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (activeScreenIndex == 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.widthIn(min = 64.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.toy_car),
                        contentDescription = stringResource(R.string.home_screen_label)
                    )

                    if (activeScreenIndex == 0) Text(
                        text = stringResource(R.string.home_screen_label),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            IconButton(
                onClick = onNavigateToConnectionsScreen,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (activeScreenIndex == 1) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.widthIn(min = 64.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.bluetooth),
                        contentDescription = stringResource(R.string.connections_screen_label)
                    )

                    if (activeScreenIndex == 1) Text(
                        text = stringResource(R.string.connections_screen_label),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            IconButton(
                onClick = onNavigateToSettingsScreen,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (activeScreenIndex == 2) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.widthIn(min = 64.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.settings),
                        contentDescription = stringResource(R.string.settings_screen_label)
                    )

                    if (activeScreenIndex == 2) Text(
                        text = stringResource(R.string.settings_screen_label),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}
