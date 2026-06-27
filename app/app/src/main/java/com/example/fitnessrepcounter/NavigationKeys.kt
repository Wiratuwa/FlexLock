package com.example.fitnessrepcounter

import androidx.navigation3.runtime.NavKey
import com.example.fitnessrepcounter.domain.model.ExerciseType
import kotlinx.serialization.Serializable

@Serializable data object Home : NavKey

@Serializable data class Calibration(val exerciseOrdinal: Int) : NavKey

@Serializable data class Workout(val exerciseOrdinal: Int) : NavKey
