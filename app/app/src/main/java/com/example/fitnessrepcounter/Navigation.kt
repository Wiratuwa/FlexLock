package com.example.fitnessrepcounter

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.fitnessrepcounter.domain.model.ExerciseType
import com.example.fitnessrepcounter.ui.screen.CalibrationScreen
import com.example.fitnessrepcounter.ui.screen.HomeScreen
import com.example.fitnessrepcounter.ui.screen.WorkoutScreen

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Home)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Home> {
                HomeScreen(
                    onExerciseSelected = { exercise ->
                        backStack.add(Calibration(exerciseOrdinal = exercise.ordinal))
                    },
                    modifier = Modifier.safeDrawingPadding(),
                )
            }

            entry<Calibration> { key ->
                val exercise = ExerciseType.entries[key.exerciseOrdinal]
                CalibrationScreen(
                    exerciseType = exercise,
                    onCalibrated = {
                        // Replace calibration with workout
                        backStack.removeLastOrNull()
                        backStack.add(Workout(exerciseOrdinal = exercise.ordinal))
                    },
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.safeDrawingPadding(),
                )
            }

            entry<Workout> { key ->
                val exercise = ExerciseType.entries[key.exerciseOrdinal]
                WorkoutScreen(
                    exerciseType = exercise,
                    onFinish = {
                        // Return to home
                        backStack.removeLastOrNull()
                    },
                    modifier = Modifier.safeDrawingPadding(),
                )
            }
        },
    )
}
