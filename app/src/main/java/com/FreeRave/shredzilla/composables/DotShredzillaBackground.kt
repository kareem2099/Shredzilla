package com.FreeRave.shredzilla.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun DotShredzillaBackground(
    gender: String?,
    content: @Composable () -> Unit
) {
    // استخدمنا Radial Gradient عشان يدي إحساس "الوهج" في نص الشاشة (زي إضاءة الجيم)
    val gradientBrush = if (gender == "Female") {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF4A148C), // بنفسجي نيون قوي في المركز
                Color(0xFF1E0033), // بنفسجي داكن جداً
                Color(0xFF0A0A0A)  // أسود ليلي في الأطراف
            ),
            center = Offset(500f, 1000f), // مركز الوهج (ممكن تتغير حسب الشاشة)
            radius = 1800f
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF1B5E20), // أخضر نيون قوي في المركز
                Color(0xFF003300), // أخضر داكن جداً
                Color(0xFF0A0A0A)  // أسود ليلي في الأطراف
            ),
            center = Offset(500f, 1000f),
            radius = 1800f
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)) // خلفية أساسية سوداء لضمان الدمج
            .background(gradientBrush)
    ) {
        content()
    }
}