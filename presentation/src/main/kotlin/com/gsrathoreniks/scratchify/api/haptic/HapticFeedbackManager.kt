package com.gsrathoreniks.scratchify.api.haptic

import com.gsrathoreniks.scratchify.api.config.HapticFeedbackType

/**
 * Interface for platform-specific haptic feedback implementations
 */
interface HapticFeedbackManager {
    /**
     * Performs haptic feedback with the specified type
     */
    fun performHapticFeedback(type: HapticFeedbackType)
    
    /**
     * Checks if haptic feedback is available on the current platform
     */
    fun isHapticFeedbackAvailable(): Boolean
}

/**
 * Default implementation that does nothing (for platforms without haptic support)
 */
internal object NoOpHapticFeedbackManager : HapticFeedbackManager {
    override fun performHapticFeedback(type: HapticFeedbackType) {
        // No-op implementation
    }
    
    override fun isHapticFeedbackAvailable(): Boolean = false
}

/**
 * Desktop deck copy: the upstream `expect` function, resolved to the no-op manager because the
 * presentation is a single-platform JVM module (no haptics on a laptop anyway).
 */
fun createHapticFeedbackManager(): HapticFeedbackManager = NoOpHapticFeedbackManager