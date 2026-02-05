package com.sandeep.silentzone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SilentModeViewModel(
    private val repo: SilentModeRepository
) : ViewModel() {
    private val _availableSsidList = MutableStateFlow<List<String>>(emptyList())
    val availableSsidList: StateFlow<List<String>> = _availableSsidList.asStateFlow()
    fun updateSsidList(ssids: List<String>) {
        _availableSsidList.value = ssids
    }
    private val uiState = MutableStateFlow(
        UiState(
            accessGranted = repo.hasPolicyAccess(),
            currentMode = repo.getCurrentMode(),
              null
        ))
    val uiStateFlow: StateFlow<UiState> = uiState.asStateFlow()
    fun refresh() {
        uiState.value = uiState.value.copy(
            accessGranted = repo.hasPolicyAccess(),
            currentMode = repo.getCurrentMode()
        )
    }
    fun setSilent() {
        viewModelScope.launch {
            val mode = repo.setSilent()
            uiState.value = uiState.value.copy(currentMode = mode, message = "Silent mode enabled")
        }
    }
    fun setNormal() {
        viewModelScope.launch {
            val mode = repo.setNormal()
            uiState.value = uiState.value.copy(currentMode = mode, message = "Normal mode enabled")
        }
    }
}