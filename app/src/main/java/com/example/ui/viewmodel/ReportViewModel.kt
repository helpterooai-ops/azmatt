package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Report
import com.example.data.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ReportUiState {
    object Idle : ReportUiState
    object Loading : ReportUiState
    data class Success(val message: String) : ReportUiState
    data class Error(val errorMessage: String) : ReportUiState
}

class ReportViewModel(
    private val repository: ReportRepository = ReportRepository()
) : ViewModel() {

    private val _publishState = MutableStateFlow<ReportUiState>(ReportUiState.Idle)
    val publishState: StateFlow<ReportUiState> = _publishState.asStateFlow()

    fun publishReport(
        stationName: String,
        fuelType: String,
        locationName: String,
        latitude: Double,
        longitude: Double,
        notes: String,
        userId: String,
        userName: String,
        userUsername: String,
        onSuccess: () -> Unit
    ) {
        val cleanStation = stationName.trim()
        val cleanLocation = locationName.trim()

        if (cleanStation.isBlank()) {
            _publishState.value = ReportUiState.Error("يرجى إدخال اسم المحطة.")
            return
        }

        if (cleanLocation.isBlank()) {
            _publishState.value = ReportUiState.Error("يرجى تحديد موقع المحطة على الخريطة أولاً.")
            return
        }

        viewModelScope.launch {
            _publishState.value = ReportUiState.Loading

            val report = Report(
                stationName = cleanStation,
                fuelType = fuelType,
                locationName = cleanLocation,
                latitude = latitude,
                longitude = longitude,
                notes = notes.trim(),
                userId = userId,
                userName = userName,
                userUsername = userUsername,
                timestamp = System.currentTimeMillis()
            )

            val result = repository.createReport(report)
            result.fold(
                onSuccess = {
                    _publishState.value = ReportUiState.Success("تم نشر البلاغ بنجاح في قاعدة البيانات!")
                    onSuccess()
                },
                onFailure = { err ->
                    _publishState.value = ReportUiState.Error(err.localizedMessage ?: "فشل نشر البلاغ.")
                }
            )
        }
    }

    fun resetPublishState() {
        _publishState.value = ReportUiState.Idle
    }
}
