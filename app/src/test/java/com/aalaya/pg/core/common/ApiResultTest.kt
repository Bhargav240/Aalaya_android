package com.aalaya.pg.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiResultTest {
    @Test
    fun `map transforms success and passes through failure`() {
        val ok: ApiResult<Int> = ApiResult.Success(21)
        assertEquals(ApiResult.Success(42), ok.map { it * 2 })

        val fail: ApiResult<Int> = ApiResult.Failure(AppFailure.NotFound())
        assertTrue(fail.map { it * 2 } is ApiResult.Failure)
    }

    @Test
    fun `onSuccess and onFailure fire on the right branch`() {
        var successHits = 0
        var failureMsg: String? = null

        ApiResult.Success("x").onSuccess { successHits++ }.onFailure { failureMsg = it.message }
        ApiResult.Failure(AppFailure.Validation("bad")).onSuccess { successHits++ }.onFailure { failureMsg = it.message }

        assertEquals(1, successHits)
        assertEquals("bad", failureMsg)
    }
}


