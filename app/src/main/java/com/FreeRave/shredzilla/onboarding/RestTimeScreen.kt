package com.FreeRave.shredzilla.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable 
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText 
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.R
import com.FreeRave.shredzilla.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestTimeScreen(onRestTimeSelected: (String) -> Unit) {
    var selectedOptionId by remember { mutableStateOf(restOptions.first().id) } 

    val gender = ThemeManager.currentGenderTheme
    val backgroundImageRes = if (gender == "Female") {
        R.drawable.third_page_female 
    } else {
        R.drawable.third_page_male 
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = backgroundImageRes),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Icon(
                imageVector = Icons.Filled.AvTimer,
                contentDescription = "Set Rest Time Icon",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary 
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Set a Rest Time",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface // Ensure readability
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "With a rest timer, Setgraph will let you\nknow when you're ready to start your\n.next set",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant // Ensure readability
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = ".The timer will reset after every set",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant // Ensure readability
            )
            Spacer(modifier = Modifier.height(24.dp))

            restOptions.forEach { option ->
                RestTimeOptionCard(
                    option = option,
                    isSelected = selectedOptionId == option.id,
                    onOptionSelected = { selectedOptionId = option.id }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.weight(1f).heightIn(min = 24.dp))

            val context = LocalContext.current
            val annotatedString = buildAnnotatedString {
                append("For more control, you can select a custom rest\ntime in the settings. Your optimal rest time can\n.depend on many factors. ")
                pushStringAnnotation(tag = "URL", annotation = "https://www.aworkoutroutine.com/how-long-to-rest-between-sets-exercises/")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                    append("Tap here")
                }
                pop()
                append(" to learn more")
            }

            ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                            context.startActivity(intent)
                        }
                }
            )
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onRestTimeSelected(selectedOptionId) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Next")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true, name = "Rest Time Light Male")
@Composable
fun RestTimeScreenMaleLightPreview() {
    ThemeManager.currentGenderTheme = "Male"
    // ThemeManager.themePreferenceMale = ThemeSetting.LIGHT // To force light for this preview
    ShredzillaTheme {
        Surface { RestTimeScreen {} }
    }
}

@Preview(showBackground = true, name = "Rest Time Dark Male")
@Composable
fun RestTimeScreenMaleDarkPreview() {
    ThemeManager.currentGenderTheme = "Male"
    // ThemeManager.themePreferenceMale = ThemeSetting.DARK // To force dark for this preview
    ShredzillaTheme {
        Surface { RestTimeScreen {} }
    }
}


@Preview(showBackground = true, name = "Rest Time Light Female")
@Composable
fun RestTimeScreenFemaleLightPreview() {
    ThemeManager.currentGenderTheme = "Female"
    // ThemeManager.themePreferenceFemale = ThemeSetting.LIGHT // To force light for this preview
    ShredzillaTheme {
        Surface { RestTimeScreen {} }
    }
}

@Preview(showBackground = true, name = "Rest Time Dark Female")
@Composable
fun RestTimeScreenFemaleDarkPreview() {
    ThemeManager.currentGenderTheme = "Female"
    // ThemeManager.themePreferenceFemale = ThemeSetting.DARK // To force dark for this preview
    ShredzillaTheme {
        Surface { RestTimeScreen {} }
    }
}
