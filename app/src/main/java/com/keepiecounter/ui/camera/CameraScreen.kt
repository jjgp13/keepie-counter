package com.keepiecounter.ui.camera

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepiecounter.ui.theme.CounterBackground
import com.keepiecounter.ui.theme.CounterWhite
import com.keepiecounter.ui.theme.SoccerGreen

@Composable
fun CameraScreen(
    onNavigateToHistory: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val count by viewModel.count.collectAsStateWithLifecycle()
    val isSessionActive by viewModel.isSessionActive.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasCameraPermission.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasPermission) {
            // Camera preview will be added in Phase 2
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        } else {
            CameraPermissionRequest(
                onPermissionGranted = { viewModel.onPermissionGranted() }
            )
        }

        // Counter overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
                .background(CounterBackground, RoundedCornerShape(24.dp))
                .padding(horizontal = 48.dp, vertical = 16.dp)
        ) {
            Text(
                text = "$count",
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = CounterWhite
            )
        }

        // Bottom controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // History button
            IconButton(onClick = onNavigateToHistory) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Start/Stop button
            Button(
                onClick = {
                    if (isSessionActive) viewModel.stopSession()
                    else viewModel.startSession()
                },
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSessionActive) Color.Red else SoccerGreen
                )
            ) {
                Icon(
                    imageVector = if (isSessionActive) Icons.Default.Stop
                    else Icons.Default.PlayArrow,
                    contentDescription = if (isSessionActive) "Stop" else "Start",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Placeholder for symmetry
            Box(modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun CameraPermissionRequest(onPermissionGranted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📷",
            fontSize = 64.sp
        )
        Text(
            text = "Camera permission required",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Keepie Counter needs camera access\nto track your keepie-uppies",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
        Button(
            onClick = onPermissionGranted,
            colors = ButtonDefaults.buttonColors(containerColor = SoccerGreen)
        ) {
            Text("Grant Permission")
        }
    }
}
