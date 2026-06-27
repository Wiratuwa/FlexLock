package com.example.fitnessrepcounter.data.supabase.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RepLogDto(
    @SerialName("id")
    val id: String? = null,
    
    @SerialName("session_id")
    val sessionId: String,
    
    @SerialName("rep_number")
    val repNumber: Int,
    
    @SerialName("detected_at")
    val detectedAt: String? = null,
    
    @SerialName("sensor_peak_magnitude")
    val sensorPeakMagnitude: Float?,
    
    @SerialName("pose_confidence")
    val poseConfidence: Float?,
    
    @SerialName("validation_state")
    val validationState: String
)
