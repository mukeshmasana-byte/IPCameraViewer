package com.mukesh.ipcameraviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TopBar(
    onLayoutChange: (Int) -> Unit,
    onAddCamera: () -> Unit,
    onDiscoverOnvif: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF202020))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "IP CAMERA VIEWER",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )

        val buttonModifier = Modifier.padding(horizontal = 4.dp).height(44.dp)
        Button(onClick = { onLayoutChange(1) }, modifier = buttonModifier) { Text("1×1", fontSize = 11.sp) }
        Button(onClick = { onLayoutChange(2) }, modifier = buttonModifier) { Text("2×2", fontSize = 11.sp) }
        Button(onClick = { onLayoutChange(3) }, modifier = buttonModifier) { Text("3×3", fontSize = 11.sp) }
        Button(onClick = onAddCamera, modifier = buttonModifier) { Text("＋ Camera", fontSize = 11.sp) }
        Button(onClick = onDiscoverOnvif, modifier = buttonModifier) { Text("ONVIF", fontSize = 11.sp) }
    }
}
