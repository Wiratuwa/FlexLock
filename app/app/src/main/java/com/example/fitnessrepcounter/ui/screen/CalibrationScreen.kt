package com.example.fitnessrepcounter.ui.screen

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.fitnessrepcounter.domain.model.ExerciseType
import com.example.fitnessrepcounter.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CalibrationScreen(
    exerciseType: ExerciseType,
    onCalibrated: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Charcoal)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // ── Top bar ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextSecondary,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "CALIBRATION",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = TextDisabled,
                    letterSpacing = 3.sp,
                ),
            )
        }

        // ── Camera preview area ──
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (cameraPermission.status.isGranted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(20.dp)),
                ) {
                    CameraPreview()

                    // Pulsing border overlay to indicate calibration
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = EaseInOut),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "pulseAlpha",
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                WarningAmber.copy(alpha = alpha * 0.1f),
                                RoundedCornerShape(20.dp),
                            ),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Position camera at ${exerciseType.recommendedView.lowercase()}",
                    style = MaterialTheme.typography.titleMedium.copy(color = WarningAmber),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Make sure your full body is visible",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextDisabled),
                    textAlign = TextAlign.Center,
                )
            } else {
                // Permission not granted
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurface),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Camera permission required",
                        style = MaterialTheme.typography.bodyLarge.copy(color = RejectRed),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // ── Start button ──
        Button(
            onClick = onCalibrated,
            enabled = cameraPermission.status.isGranted,
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricLime,
                contentColor = Charcoal,
                disabledContainerColor = SubtleGray,
                disabledContentColor = TextDisabled,
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(
                text = "START ${exerciseType.displayName.uppercase()}",
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 2.sp,
                ),
            )
        }
    }
}

@Composable
private fun CameraPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = androidx.camera.core.Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                    )
                } catch (_: Exception) {
                    // Camera bind failed
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize(),
    )
}
