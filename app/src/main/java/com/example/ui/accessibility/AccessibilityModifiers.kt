package com.example.ui.accessibility

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * Advanced Jetpack Compose Accessibility & Semantics delegate wrappers implementing
 * robust WCAG 2.2 compliance and screen reader compatibility across EcoTrack AI.
 */
object AccessibilityModifiers {

    /**
     * Decorates an item as a Screen Reader heading (WCAG 2.2 H1-H6 structural hierarchy equivalent).
     */
    fun Modifier.wcagHeading(): Modifier = this.semantics {
        heading()
    }

    /**
     * Custom click wrapper with custom Action labels ensuring high comprehension of the touch targets.
     * Prevents empty or default ambiguous "Double-tap to activate" triggers.
     * Enforces WCAG 2.2 Target Size minimum (48.dp x 48.dp target size padding).
     */
    fun Modifier.wcagClickable(
        label: String,
        role: Role = Role.Button,
        onClickLabel: String? = null,
        onClick: () -> Unit
    ): Modifier = this
        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        .clickable(
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick
        )
        .semantics {
            this.role = role
            this.contentDescription = label
        }

    /**
     * Declares a screen element as an active dynamic region (e.g. dynamic live emissions counters).
     * The Screen Reader announces state shifts politely when data recalculates.
     */
    fun Modifier.wcagLiveRegion(
        descriptionText: String,
        currentValue: String
    ): Modifier = this.semantics {
        liveRegion = LiveRegionMode.Polite
        contentDescription = "$descriptionText. Current reading: $currentValue"
        stateDescription = currentValue
    }

    /**
     * A pure information block semantic wrapper (clears noisy nested child announcements
     * and compiles child components into a single clear spoken sentence/paragraph block).
     */
    fun Modifier.wcagInformationBlock(
        blockDescription: String
    ): Modifier = this.clearAndSetSemantics {
        contentDescription = blockDescription
    }

    /**
     * Ensures switch / toggle state changes are fully announced matching the correct visual state.
     */
    fun Modifier.wcagToggleSemantics(
        label: String,
        isToggled: Boolean,
        onToggleLabel: String = "Simbulate state toggle"
    ): Modifier = this.semantics {
        this.role = Role.Switch
        this.contentDescription = label
        this.stateDescription = if (isToggled) "Currently Enabled/Active" else "Currently Disabled/Inactive"
        onClick(label = onToggleLabel) {
            true
        }
    }
}
