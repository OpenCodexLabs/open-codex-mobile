package com.remodex.mobile.services

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodexServiceHistoryTest {
    @Test
    fun shouldSkipThreadHistoryHydration_doesNotSkipPreviouslyHydratedEmptyTimeline() {
        assertFalse(
            shouldSkipThreadHistoryHydration(
                force = false,
                alreadyHydrated = true,
                hasLocalTimeline = false,
            ),
        )
    }

    @Test
    fun shouldSkipThreadHistoryHydration_skipsPreviouslyHydratedNonEmptyTimeline() {
        assertTrue(
            shouldSkipThreadHistoryHydration(
                force = false,
                alreadyHydrated = true,
                hasLocalTimeline = true,
            ),
        )
    }

    @Test
    fun shouldSkipThreadHistoryHydration_forceNeverSkips() {
        assertFalse(
            shouldSkipThreadHistoryHydration(
                force = true,
                alreadyHydrated = true,
                hasLocalTimeline = true,
            ),
        )
    }
}
