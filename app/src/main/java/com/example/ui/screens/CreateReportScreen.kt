package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EvStation
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.QuickThemeToggleButton
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.ReportUiState
import com.example.ui.viewmodel.ReportViewModel
import com.example.ui.viewmodel.ThemeViewModel

@Composable
fun CreateReportScreen(
    authViewModel: AuthViewModel,
    themeViewModel: ThemeViewModel,
    reportViewModel: ReportViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentThemeMode by themeViewModel.themeMode.collectAsState()
    val currentUserProfile by authViewModel.currentUserProfile.collectAsState()
    val publishState by reportViewModel.publishState.collectAsState()

    var stationName by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var selectedLat by remember { mutableDoubleStateOf(0.0) }
    var selectedLng by remember { mutableDoubleStateOf(0.0) }
    var notes by remember { mutableStateOf("") }
    var selectedFuelType by remember { mutableStateOf("بترول") }

    var showMapPicker by remember { mutableStateOf(false) }

    val fuelTypes = listOf("بترول", "ديزل", "غاز")

    LaunchedEffect(publishState) {
        when (publishState) {
            is ReportUiState.Success -> {
                val msg = (publishState as ReportUiState.Success).message
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                // Clear fields
                stationName = ""
                locationName = ""
                selectedLat = 0.0
                selectedLng = 0.0
                notes = ""
                reportViewModel.resetPublishState()
            }
            is ReportUiState.Error -> {
                val err = (publishState as ReportUiState.Error).errorMessage
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    if (showMapPicker) {
        MapPickerScreen(
            onLocationSelected = { address, lat, lng ->
                locationName = address
                selectedLat = lat
                selectedLng = lng
                showMapPicker = false
            },
            onDismiss = { showMapPicker = false }
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "إنشاء بلاغ جديد",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "شارك توفر المشتقات النفطية في المحطات القريبة منك",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                QuickThemeToggleButton(
                    currentThemeMode = currentThemeMode,
                    onToggleTheme = { themeViewModel.toggleTheme() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Glass Card Form
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                elevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Select Fuel Type Label
                    Text(
                        text = "نوع المشتق النفطي",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fuel Type Selector Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        fuelTypes.forEach { type ->
                            val isSelected = type == selectedFuelType
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { selectedFuelType = type }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Station Name Field
                    OutlinedTextField(
                        value = stationName,
                        onValueChange = { stationName = it },
                        label = { Text("اسم المحطة") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.EvStation,
                                contentDescription = "اسم المحطة"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_station_name_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Location Picker Button/Field
                    Text(
                        text = "موقع المحطة (الخريطة)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                width = if (locationName.isNotBlank()) 1.5.dp else 1.dp,
                                color = if (locationName.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { showMapPicker = true }
                            .testTag("report_location_picker_card")
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (locationName.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (locationName.isNotBlank()) Icons.Outlined.LocationOn else Icons.Outlined.Map,
                                        contentDescription = "اختيار الخريطة",
                                        tint = if (locationName.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (locationName.isNotBlank()) locationName else "اضغط لاختيار الموقع من الخريطة 🗺️",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (locationName.isNotBlank()) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (locationName.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                    if (selectedLat != 0.0 && selectedLng != 0.0) {
                                        Text(
                                            text = "الإحداثيات: ${String.format("%.4f", selectedLat)}, ${String.format("%.4f", selectedLng)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (locationName.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            locationName = ""
                                            selectedLat = 0.0
                                            selectedLng = 0.0
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                                            .testTag("clear_selected_location_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "حذف الموقع المحدد",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Outlined.Map,
                                    contentDescription = "فتح الخريطة",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Image Upload Space Placeholder
                    Text(
                        text = "إضافة صورة (اختياري)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { /* UI only placeholder */ }
                            .testTag("report_image_upload_placeholder"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AddPhotoAlternate,
                                contentDescription = "إضافة صورة",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "اضغط لرفع صورة المحطة أو الطابور",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Notes Field
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("حقل كتابة الملاحظة") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Notes,
                                contentDescription = "ملاحظات"
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_notes_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Publish Button
                    val isLoading = publishState is ReportUiState.Loading

                    Button(
                        onClick = {
                            val uid = currentUserProfile?.uid ?: ""
                            val userName = currentUserProfile?.name?.ifBlank { "مستخدم أزمات" } ?: "مستخدم أزمات"
                            val userUsername = currentUserProfile?.username ?: "user"

                            reportViewModel.publishReport(
                                stationName = stationName,
                                fuelType = selectedFuelType,
                                locationName = locationName,
                                latitude = selectedLat,
                                longitude = selectedLng,
                                notes = notes,
                                userId = uid,
                                userName = userName,
                                userUsername = userUsername,
                                onSuccess = {}
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("publish_report_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = "نشر البلاغ",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "نشر البلاغ في أزمات",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
