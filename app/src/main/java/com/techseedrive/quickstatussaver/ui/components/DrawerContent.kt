package com.techseedrive.quickstatussaver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.techseedrive.quickstatussaver.ui.theme.LocalIsDarkTheme

@Composable
fun DrawerContent(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onItemSelected: (String) -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val backgroundColor = if (isDark) Color.Black else Color.White
    val itemColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp) // Fixed width for the drawer
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        // Logo / App Name
        Text(
            text = "QuickStatusSaver",
            color = itemColor,
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(24.dp))

        // WhatsApp
        DrawerItem(
            label = "WhatsApp",
            icon = Icons.Filled.Chat,
            color = itemColor,
            onClick = { onItemSelected("whatsapp") }
        )

        // Business WhatsApp
        DrawerItem(
            label = "Business WhatsApp",
            icon = Icons.Filled.Business,
            color = itemColor,
            onClick = { onItemSelected("business") }
        )

        DrawerItem(
            label = "Saved Status",
            icon = Icons.Default.Save,
            color = itemColor,
            onClick = { onItemSelected("savedStatus") }
        )

        DrawerItem(
            label = "Privacy Policy",
            icon = Icons.Default.PrivacyTip,
            color = itemColor,
            onClick = { onItemSelected("privacyPolicy") }
        )

        DrawerItem(
            label = "About",
            icon = Icons.Default.Info,
            color = itemColor,
            onClick = { onItemSelected("about") }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Theme Switch
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(text = "Dark Mode", color = itemColor)
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isDarkTheme,
                onCheckedChange = { onToggleTheme() }
            )
        }
    }
}
