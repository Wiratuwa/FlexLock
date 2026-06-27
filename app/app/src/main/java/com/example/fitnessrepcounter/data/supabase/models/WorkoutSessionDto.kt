package com.example.fitnessrepcounter.data.supabase.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkoutSessionDto(
    @SerialName("id")
    val id: String? = null, // Can be null for inserts if using default DB generation, or client-side generated UUID
    
    @SerialName("user_id")
    val userId: String? = null,
    
    @SerialName("exercise_type")
    val exerciseType: String,
    
    @SerialName("target_reps")
    val targetReps: Int,
    
    @SerialName("actual_reps")
    val actualReps: Int,
    
    @SerialName("set_count")
    val setCount: Int,
    
    @SerialName("started_at")
    val startedAt: String? = null,
    
    @SerialName("completed_at")
    val completedAt: String? = null,
    
    @SerialName("is_completed")
    val isCompleted: Boolean
)
