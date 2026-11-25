package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ThemeToggleButton(
    isDarkTheme: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (isDarkTheme) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        },
        animationSpec = tween(240),
        label = "themeToggleBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isDarkTheme) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        animationSpec = tween(240),
        label = "themeToggleFg",
    )
    val label = if (isDarkTheme) "ពន្លឺ" else "ងងឹត"
    val description = if (isDarkTheme) "ប្តូរទៅផ្ទៃពន្លឺ" else "ប្តូរទៅផ្ទៃងងឹត"

    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .semantics { contentDescription = description }
            .testTag("theme_switcher_button"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AnimatedContent(
                targetState = isDarkTheme,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                label = "themeIcon",
            ) { dark ->
                Icon(
                    imageVector = if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun ThemeToggleIconButton(
    isDarkTheme: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = if (isDarkTheme) "ប្តូរទៅផ្ទៃពន្លឺ" else "ប្តូរទៅផ្ទៃងងឹត"
    IconButton(
        onClick = onToggle,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("theme_switcher_icon_button"),
    ) {
        Icon(
            imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
