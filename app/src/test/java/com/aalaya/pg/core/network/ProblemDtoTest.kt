package com.aalaya.pg.core.network

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProblemDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `full RFC7807 problem parses`() {
        val p = json.decodeFromString<ProblemDto>(
            """{
                "type": "https://hamsavilas.app/problems/forbidden",
                "title": "manager scope mismatch",
                "status": 403,
                "detail": "You can only manage your own branch"
            }"""
        )
        assertEquals(403, p.status)
        assertEquals("manager scope mismatch", p.title)
        assertEquals("You can only manage your own branch", p.detail)
    }

    @Test
    fun `missing detail leaves it null`() {
        val p = json.decodeFromString<ProblemDto>(
            """{"type":"about:blank","title":"boom","status":500}"""
        )
        assertEquals("boom", p.title)
        assertNull(p.detail)
    }

    @Test
    fun `unknown keys are ignored so future fields don't crash older apps`() {
        val p = json.decodeFromString<ProblemDto>(
            """{"type":"x","title":"t","status":400,"trace_id":"abc","future_field":[1,2,3]}"""
        )
        assertEquals(400, p.status)
        assertEquals("t", p.title)
    }
}
