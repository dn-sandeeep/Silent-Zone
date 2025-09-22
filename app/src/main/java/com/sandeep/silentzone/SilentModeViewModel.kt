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
//        viewModelScope.launch {
//            val granted = repo.hasPolicyAccess()
//            val mode = repo.getCurrentMode()
//            uiState.value = uiState.value.copy(accessGranted = granted, currentMode = mode, message = null)
//        }
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