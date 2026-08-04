package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.AppThemeMode

@Composable
fun QuickThemeToggleButton(
    currentThemeMode: AppThemeMode,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = currentThemeMode == AppThemeMode.DARK

    GlassCard(
        shape = CircleShape,
        elevation = 4.dp,
        modifier = modifier
            .testTag("quick_theme_toggle_button")
            .clickable { onToggleTheme() }
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(targetState = isDark, label = "ThemeIconCrossfade") { dark ->
                if (dark) {
                    Icon(
                        imageVector = Icons.Outlined.LightMode,
                        contentDescription = "تحويل للوضع الفاتح",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.DarkMode,
                        contentDescription = "تحويل للوضع الداكن",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
