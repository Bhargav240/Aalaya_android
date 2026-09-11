package com.aalaya.pg.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComplaintStateTest {
    @Test
    fun `transitions mirror the backend state machine`() {
        assertEquals(listOf(ComplaintState.Assigned, ComplaintState.InProgress, ComplaintState.Closed), ComplaintState.Open.allowedTransitions())
        assertEquals(listOf(ComplaintState.InProgress, ComplaintState.Closed), ComplaintState.Assigned.allowedTransitions())
        assertTrue(ComplaintState.InProgress.allowedTransitions().contains(ComplaintState.Resolved))
        assertTrue(ComplaintState.InProgress.allowedTransitions().contains(ComplaintState.Closed))
        // Resolved can be closed or bounced back to in-progress.
        assertTrue(ComplaintState.Resolved.allowedTransitions().contains(ComplaintState.Closed))
        assertTrue(ComplaintState.Resolved.allowedTransitions().contains(ComplaintState.InProgress))
        assertTrue(ComplaintState.Closed.allowedTransitions().contains(ComplaintState.InProgress))
    }

    @Test
    fun `wire parsing is round-trip safe`() {
        ComplaintState.entries.forEach { assertEquals(it, ComplaintState.fromWire(it.wire)) }
    }
}
