package com.fernandomendoza.dashboardcarrorf.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernandomendoza.dashboardcarrorf.models.DashboardState
import com.fernandomendoza.dashboardcarrorf.repository.DashboardRepository
import com.fernandomendoza.dashboardcarrorf.ui.state.HomeScreenState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.lang.IllegalStateException

class HomeViewModel(private val dashboardRepository: DashboardRepository) : ViewModel() {

    val uiState: StateFlow<HomeScreenState?> = dashboardRepository.dashboardState
        .map { state ->
            when (state) {
                DashboardState.Disconnected -> HomeScreenState.Disconnected
                is DashboardState.Loaded -> HomeScreenState.Connected(
                    state.gps,
                    state.approximateSpeedMetersPerSecond,
                    state.batterySoC,
                    state.hoursOfBatteryLeft
                )
                DashboardState.Loading -> HomeScreenState.Loading
            }
        }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeScreenState.Disconnected
        )

    fun subscribeToDashboardState() {
        viewModelScope.launch {
            try {
                dashboardRepository.subscribeToState()
            } catch (e: IOException) {
                cancelStateSubscription()
            } catch (e: IllegalStateException) {
                Log.w(HomeViewModel::class.simpleName, e.message ?: "Cannot start subscription to dashboard state")
                cancelStateSubscription()
            }
        }
    }

    fun cancelStateSubscription() = dashboardRepository.cancelSubscription()
}