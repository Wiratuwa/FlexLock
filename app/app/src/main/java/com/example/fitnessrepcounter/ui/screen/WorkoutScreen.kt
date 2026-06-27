package com.example.fitnessrepcounter.ui.screen

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.fitnessrepcounter.data.audio.AudioRepository
import com.example.fitnessrepcounter.data.pose.PoseAngleCalculator
import com.example.fitnessrepcounter.data.sensor.SensorDataFilter
import com.example.fitnessrepcounter.data.sensor.SensorRepository
import com.example.fitnessrepcounter.domain.engine.ExerciseValidator
import com.example.fitnessrepcounter.domain.engine.FusionEngine
import com.example.fitnessrepcounter.domain.model.ExerciseType
import com.example.fitnessrepcounter.domain.model.RepState
import com.example.fitnessrepcounter.data.supabase.SupabaseRepository
import com.example.fitnessrepcounter.data.supabase.models.RepLogDto
import com.example.fitnessrepcounter.data.supabase.models.WorkoutSessionDto
import com.example.fitnessrepcounter.theme.*
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun WorkoutScreen(
    exerciseType: ExerciseType,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // ── State ──
    var repCount by remember { mutableIntStateOf(0) }
    var lastRepState by remember { mutableStateOf(RepState.IDLE) }
    var isSoftLocked by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var currentPose by remember { mutableStateOf<Pose?>(null) }
    var poseConfidence by remember { mutableFloatStateOf(0f) }
    val repLogs = remember { mutableStateListOf<RepLogDto>() }

    // ── Engines ──
    val fusionEngine = remember { FusionEngine() }
    val exerciseValidator = remember { ExerciseValidator() }
    val sensorFilter = remember { SensorDataFilter() }
    val sensorRepository = remember { SensorRepository(context) }
    val audioRepository = remember { AudioRepository(context) }

    val scope = rememberCoroutineScope()

    // Collect rep count from fusion engine
    LaunchedEffect(Unit) {
        fusionEngine.repCount.collectLatest { count ->
            repCount = count
        }
    }

    // Collect fusion state
    LaunchedEffect(Unit) {
        fusionEngine.state.collectLatest { state ->
            lastRepState = state
            if (state == RepState.VALIDATED) {
                repLogs.add(
                    RepLogDto(
                        sessionId = "", // populated before save
                        repNumber = fusionEngine.repCount.value,
                        sensorPeakMagnitude = null,
                        poseConfidence = poseConfidence,
                        validationState = "VALIDATED"
                    )
                )
            }
        }
    }

    // Start sensor pipeline
    LaunchedEffect(Unit) {
        launch(Dispatchers.Default) {
            sensorRepository.accelerometerFlow.collect { data ->
                val result = sensorFilter.process(data)
                if (result is SensorDataFilter.FilterResult.PeakDetected) {
                    val validated = fusionEngine.onSensorPeak(System.currentTimeMillis())
                    if (validated) {
                        audioRepository.playTick()
                    }
                }
            }
        }
    }

    // Preload audio
    LaunchedEffect(Unit) {
        audioRepository.preload()
    }

    // Keep screen on
    DisposableEffect(Unit) {
        val activity = context as? android.app.Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            audioRepository.release()
        }
    }

    // Periodic window expiry tick
    LaunchedEffect(Unit) {
        while (true) {
            delay(200)
            fusionEngine.tick(System.currentTimeMillis())
        }
    }

    // ── Back handler (soft lock) ──
    BackHandler(enabled = true) {
        if (!isSoftLocked) {
            showFinishDialog = true
        }
        // When soft locked, back button is completely suppressed
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Charcoal),
    ) {
        // ── Camera + Pose Overlay ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
        ) {
            WorkoutCameraPreview(
                exerciseType = exerciseType,
                exerciseValidator = exerciseValidator,
                fusionEngine = fusionEngine,
                audioRepository = audioRepository,
                onPoseUpdate = { pose, confidence ->
                    currentPose = pose
                    poseConfidence = confidence
                },
            )

            // Pose skeleton overlay
            currentPose?.let { pose ->
                PoseOverlay(pose = pose)
            }

            // Status indicator top-left
            StatusChip(
                text = when (lastRepState) {
                    RepState.IDLE -> "READY"
                    RepState.SENSOR_TRIGGERED -> "MOTION…"
                    RepState.POSE_TRIGGERED -> "POSE…"
                    RepState.VALIDATED -> "REP!"
                    RepState.REJECTED -> "MISSED"
                },
                color = when (lastRepState) {
                    RepState.IDLE -> TextDisabled
                    RepState.SENSOR_TRIGGERED, RepState.POSE_TRIGGERED -> WarningAmber
                    RepState.VALIDATED -> ValidGreen
                    RepState.REJECTED -> RejectRed
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
            )
        }

        // ── Counter + Controls ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Exercise label
            Text(
                text = exerciseType.displayName.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = TextDisabled,
                    letterSpacing = 4.sp,
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Giant rep counter with animation
            val animatedCount by animateIntAsState(
                targetValue = repCount,
                animationSpec = tween(300),
                label = "repCount",
            )
            Text(
                text = "$animatedCount",
                style = MaterialTheme.typography.displayLarge.copy(
                    color = ElectricLime,
                    fontSize = 120.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )

            Text(
                text = "REPS",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = TextSecondary,
                    letterSpacing = 6.sp,
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Confidence bar
            LinearProgressIndicator(
                progress = { poseConfidence },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = when {
                    poseConfidence > 0.7f -> ValidGreen
                    poseConfidence > 0.4f -> WarningAmber
                    else -> RejectRed
                },
                trackColor = SubtleGray,
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pose confidence",
                style = MaterialTheme.typography.labelSmall.copy(color = TextDisabled),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Control buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Soft lock toggle
                FilledIconButton(
                    onClick = { isSoftLocked = !isSoftLocked },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isSoftLocked) RejectRed.copy(alpha = 0.2f) else MidGray,
                        contentColor = if (isSoftLocked) RejectRed else TextSecondary,
                    ),
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = if (isSoftLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (isSoftLocked) "Unlock" else "Lock",
                    )
                }

                // Stop button
                Button(
                    onClick = { showFinishDialog = true },
                    enabled = !isSoftLocked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MidGray,
                        contentColor = TextPrimary,
                        disabledContainerColor = SubtleGray.copy(alpha = 0.3f),
                        disabledContentColor = TextDisabled,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FINISH",
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
                    )
                }
            }
        }

        // ── Soft lock unlock zone (right top corner) ──
        if (isSoftLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(60.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                // 5-second long press handled by gesture timeout
                                isSoftLocked = false
                            },
                        )
                    },
            )
        }
    }

    // ── Finish dialog ──
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = {
                Text(
                    "End Workout?",
                    style = MaterialTheme.typography.headlineMedium.copy(color = TextPrimary),
                )
            },
            text = {
                Text(
                    "You completed $repCount reps of ${exerciseType.displayName}.",
                    style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isSaving = true
                        scope.launch {
                            val repo = SupabaseRepository()
                            val session = WorkoutSessionDto(
                                exerciseType = exerciseType.name,
                                targetReps = 0, // Not explicitly tracked in this screen yet
                                actualReps = repCount,
                                setCount = 1,
                                isCompleted = true
                            )
                            repo.saveWorkoutSession(session, repLogs)
                            showFinishDialog = false
                            isSaving = false
                            onFinish()
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.textButtonColors(contentColor = ElectricLime),
                ) {
                    Text(if (isSaving) "SAVING..." else "FINISH")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showFinishDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextDisabled),
                ) {
                    Text("CANCEL")
                }
            },
            containerColor = DarkSurface,
        )
    }
}

@Composable
private fun StatusChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Charcoal.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = color,
                    letterSpacing = 1.sp,
                ),
            )
        }
    }
}

@Composable
private fun WorkoutCameraPreview(
    exerciseType: ExerciseType,
    exerciseValidator: ExerciseValidator,
    fusionEngine: FusionEngine,
    audioRepository: AudioRepository,
    onPoseUpdate: (Pose?, Float) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var frameCounter by remember { mutableIntStateOf(0) }

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

                // ML Kit Pose Detector
                val poseOptions = AccuratePoseDetectorOptions.Builder()
                    .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
                    .build()
                val poseDetector = PoseDetection.getClient(poseOptions)

                // Image Analysis with backpressure
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                    // Frame sub-sampling: process every 3rd frame
                    frameCounter++
                    if (frameCounter % 3 != 0) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    @androidx.camera.core.ExperimentalGetImage
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val inputImage = com.google.mlkit.vision.common.InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees,
                        )

                        poseDetector.process(inputImage)
                            .addOnSuccessListener { pose ->
                                val angleResult = PoseAngleCalculator.calculate(pose, exerciseType)
                                val avgConfidence = if (angleResult != null) angleResult.confidence else 0f
                                onPoseUpdate(pose, avgConfidence)

                                if (angleResult != null) {
                                    val repCompleted = exerciseValidator.processAngle(exerciseType, angleResult)
                                    if (repCompleted) {
                                        val validated = fusionEngine.onPoseConfirmed(System.currentTimeMillis())
                                        if (validated) {
                                            audioRepository.playTick()
                                        }
                                    }
                                }
                            }
                            .addOnFailureListener {
                                onPoseUpdate(null, 0f)
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis,
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

@Composable
private fun PoseOverlay(pose: Pose) {
    // Draw skeleton lines between connected landmarks
    val connections = remember {
        listOf(
            // Torso
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
            // Left arm
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
            PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
            // Right arm
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
            // Left leg
            PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
            PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
            // Right leg
            PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
            PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE,
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val scaleX = size.width
        val scaleY = size.height

        // Draw connections
        connections.forEach { (startType, endType) ->
            val start = pose.getPoseLandmark(startType)
            val end = pose.getPoseLandmark(endType)
            if (start != null && end != null &&
                start.inFrameLikelihood > 0.5f && end.inFrameLikelihood > 0.5f
            ) {
                // Normalize landmark positions (ML Kit returns pixel coordinates)
                // These need to be scaled to the canvas size
                drawLine(
                    color = SkeletonLime,
                    start = Offset(start.position.x, start.position.y),
                    end = Offset(end.position.x, end.position.y),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round,
                )
            }
        }

        // Draw landmark dots
        pose.allPoseLandmarks.forEach { landmark ->
            if (landmark.inFrameLikelihood > 0.5f) {
                drawCircle(
                    color = ElectricLime,
                    radius = 8f,
                    center = Offset(landmark.position.x, landmark.position.y),
                )
            }
        }
    }
}
