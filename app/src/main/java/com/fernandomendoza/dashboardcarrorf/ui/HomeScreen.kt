package com.fernandomendoza.dashboardcarrorf.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.fernandomendoza.dashboardcarrorf.R
import com.fernandomendoza.dashboardcarrorf.repository.DashboardRepository
import com.fernandomendoza.dashboardcarrorf.ui.state.HomeScreenState
import com.fernandomendoza.dashboardcarrorf.ui.widgets.MapBoxMap
import com.mapbox.geojson.Point

internal const val HOME_SCREEN_ROUTE = "home"

@Composable
private fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToConnections: () -> Unit,
    modifier: Modifier = Modifier
) {
    DisposableEffect(viewModel::subscribeToDashboardState) {
        viewModel.subscribeToDashboardState()

        onDispose {

        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        
        when (uiState) {
            is HomeScreenState.Connected -> ConnectedHomeScreen(state = uiState as HomeScreenState.Connected)
            HomeScreenState.Disconnected -> DisconnectedHomeScreen(onNavigateToConnections)
            else -> LoadingHomeScreen()
        }
    }
}

@Composable
fun ConnectedHomeScreen(
    state: HomeScreenState.Connected,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RectangleShape,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxHeight(0.25f)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                DashboardTile(
                    label = stringResource(R.string.satellite_count_label),
                    icon = R.drawable.satellite,
                    iconColor = MaterialTheme.colorScheme.onSurface
                ) {
                    if (state.gps != null) {
                        Text(
                            state.gps.numberOfSatellites.toString(),
                            style = MaterialTheme.typography.displaySmall
                        )
                    } else {
                        TextPlaceholder()
                    }
                }

                DashboardTile(
                    label = stringResource(R.string.speed),
                    icon = R.drawable.speed,
                    iconColor = MaterialTheme.colorScheme.onSurface
                ) {
                    if (state.approximateSpeedMetersPerSecond != null && state.approximateSpeedMetersPerSecond < 1000.0) {
                        Text(
                            String.format("%.1f RPM", state.approximateSpeedMetersPerSecond),
                            style = MaterialTheme.typography.displaySmall
                        )
                    } else {
                        TextPlaceholder()
                    }
                }

                val batteryIcon = if (state.batterySoC != null) {
                    if (state.batterySoC > 90) R.drawable.battery_full
                    else if (state.batterySoC >= 65) R.drawable.battery_075
                    else if (state.batterySoC >= 35) R.drawable.battery_050
                    else if (state.batterySoC >= 10) R.drawable.battery_025
                    else R.drawable.battery_empty
                } else {
                    R.drawable.battery_full
                }

                val batteryColor = if (state.batterySoC != null) {
                    if (state.batterySoC >= 50) Color.Green
                    else if (state.batterySoC >= 20) Color.Yellow
                    else Color.Red
                } else {
                    MaterialTheme.colorScheme.onBackground
                }

                DashboardTile(
                    label = stringResource(R.string.battery_level_label),
                    icon = batteryIcon,
                    iconColor = batteryColor
                ) {
                    if (state.batterySoC != null && state.hoursOfBatteryLeft != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
//                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "${state.batterySoC} %",
                                style = MaterialTheme.typography.displaySmall
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                String.format("%.1f hora${if (state.hoursOfBatteryLeft < 1.0 || state.hoursOfBatteryLeft > 2.0f)"s" else ""}", state.hoursOfBatteryLeft),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    } else {
                        TextPlaceholder()
                    }
                }
            }
        }

        if (state.gps != null) {
            with(state.gps) {
                MapBoxMap(
                    point = Point.fromLngLat( longitudeAsDecimalDegrees, latitudeAsDecimalDegrees),
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun LoadingHomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RectangleShape,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxHeight(0.25f)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                DashboardTile(
                    label = stringResource(R.string.satellite_count_label),
                    icon = R.drawable.satellite,
                    iconColor = MaterialTheme.colorScheme.onSurface
                ) {
                    TextPlaceholder()
                }

                DashboardTile(
                    label = stringResource(R.string.speed),
                    icon = R.drawable.speed,
                    iconColor = MaterialTheme.colorScheme.onSurface
                ) {
                    TextPlaceholder()
                }

                DashboardTile(
                    label = stringResource(R.string.battery_level_label),
                    icon = R.drawable.battery_050,
                ) {
                    TextPlaceholder()
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun DisconnectedHomeScreen(
    onNavigateToConnections: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.disconnected_hint),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        FilledTonalButton(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            onClick = onNavigateToConnections
        ) {
            Icon(painter = painterResource(R.drawable.bluetooth), contentDescription = null)
            Text(
                text = stringResource(R.string.connect_action),
            )
        }
    }
}

@Composable
fun DashboardTile(
    label: String,
    @DrawableRes icon: Int,
    iconColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = iconColor
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                label,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.labelLarge
            )
        }

        content()
    }
}

@Composable
fun TextPlaceholder() {
    Box(
        modifier = Modifier
            .sizeIn(minWidth = 80.dp, minHeight = 16.dp)
            .background(color = MaterialTheme.colorScheme.onSurfaceVariant)
    )
}

fun NavGraphBuilder.homeScreen(
    dashboardRepository: DashboardRepository,
    onNavigateToConnections: () -> Unit,
    modifier: Modifier = Modifier
) {
    composable(route = HOME_SCREEN_ROUTE) {
        val viewModel by remember {
            mutableStateOf(HomeViewModel(dashboardRepository))
        }

        HomeScreen(
            viewModel,
            onNavigateToConnections,
            modifier
        )
    }
}

fun NavController.navigateToHome() {
    navigate(HOME_SCREEN_ROUTE) { launchSingleTop = true }
}