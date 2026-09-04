package com.aalaya.pg.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComplaintStateTest {
    @Test
    fun `transitions mirror the backend state machine`() {
        assertEquals(listOf(ComplaintState.Assigned, ComplaintState.InProgress), ComplaintState.Open.allowedTransitions())
        assertEquals(listOf(ComplaintState.InProgress), ComplaintState.Assigned.allowedTransitions())
        assertEquals(listOf(ComplaintState.Resolved), ComplaintState.InProgress.allowedTransitions())
        // Resolved can be closed or bounced back to in-progress.
        assertTrue(ComplaintState.Resolved.allowedTransitions().contains(ComplaintState.Closed))
        assertTrue(ComplaintState.Resolved.allowedTransitions().contains(ComplaintState.InProgress))
        assertEquals(emptyList<ComplaintState>(), ComplaintState.Closed.allowedTransitions())
    }

    @Test
    fun `wire parsing is round-trip safe`() {
        ComplaintState.entries.forEach { assertEquals(it, ComplaintState.fromWire(it.wire)) }
    }
}
