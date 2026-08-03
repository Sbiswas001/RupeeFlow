package sayan.apps.rupeeflow.core.util

import androidx.compose.runtime.staticCompositionLocalOf
import sayan.apps.rupeeflow.domain.model.UserPreferences

val LocalUserPreferences = staticCompositionLocalOf {
    UserPreferences()
}
