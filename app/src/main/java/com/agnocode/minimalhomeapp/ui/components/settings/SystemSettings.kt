package com.agnocode.minimalhomeapp.ui.components.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.res.stringResource
import com.agnocode.minimalhomeapp.R

@Composable
fun SystemSettings(
    context: Context,
    isDefault: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Column {
        SettingsSectionHeader(
            title = stringResource(R.string.settings_section_system),
            isExpanded = isExpanded,
            onToggle = onToggle
        )

        if (isExpanded) {
            if (!isDefault) {
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(stringResource(R.string.system_set_default), fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    stringResource(R.string.system_is_default),
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 32.dp, bottom = 16.dp)
                )
            }
        }
    }
}
