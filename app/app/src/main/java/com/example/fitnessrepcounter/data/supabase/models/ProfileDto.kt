package com.example.fitnessrepcounter.data.supabase.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    @SerialName("id")
    val id: String,
    
    @SerialName("display_name")
    val displayName: String? = null,
    
    @SerialName("created_at")
    val createdAt: String? = null
)
