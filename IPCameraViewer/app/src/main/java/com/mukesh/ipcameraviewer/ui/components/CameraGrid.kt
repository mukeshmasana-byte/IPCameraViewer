package com.mukesh.ipcameraviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mukesh.ipcameraviewer.Camera
import kotlin.math.max

@Composable
fun CameraGrid(
    columns: Int,
    cameras: List<Camera>,
    selectedCameraId: Long?,
    onCameraClick: (Camera) -> Unit,
    onCameraEdit: (Camera) -> Unit
) {
    val maxCameras = 9
    val count = columns * columns
    val displayCameras = cameras.take(maxCameras)
    val slots = max(displayCameras.size, count)

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(1.dp)
    ) {
        items(slots) { index ->
            var isFocused by remember { mutableStateOf(false) }
            val modifier = Modifier
                .aspectRatio(16f / 9f)
                .padding(1.dp)
                .background(Color.Black)
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .border(if (isFocused) 3.dp else 0.dp, if (isFocused) Color.White else Color.Transparent)

            Box(modifier = modifier) {
                val camera = displayCameras.getOrNull(index)
                if (camera != null) {
                    val isSelected = camera.id == selectedCameraId
                    CameraTile(
                        camera = camera,
                        isSelected = isSelected,
                        onClick = { onCameraClick(camera) },
                        onLongClick = { onCameraEdit(camera) },
                        columns = columns
                    )
                } else {
                    EmptyTile(slot = index + 1)
                }
            }
        }
    }
}

@Composable
fun CameraTile(
    camera: Camera,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    columns: Int
) {
    var errorText by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
    ) {
        if (camera.rtspUrl.isNotBlank()) {
            VideoPlayer(
                camera = camera,
                columns = columns,
                isSelected = isSelected,
                onError = { errorText = "STREAM ERROR" }
            )
        }

        Row(
            modifier = Modifier
                .background(Color(0x99000000))
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (errorText != null) "${camera.name} • $errorText" else camera.name,
                color = Color.White,
                fontSize = 12.sp
            )
            Text(
                text = "  [EDIT]",
                color = Color.Cyan,
                fontSize = 10.sp,
                modifier = Modifier.clickable { onLongClick() }
            )
            if (isSelected) {
                Text(
                    text = "  ★",
                    color = Color.Yellow,
                    fontSize = 12.sp
                )
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            )
        }
    }
}

@Composable
fun EmptyTile(slot: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Camera $slot\nTap ＋ Camera to add",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}
