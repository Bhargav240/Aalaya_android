package com.aalaya.pg.core.network.dto

import kotlinx.serialization.json.Json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonNamingStrategy
import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceDtosTest {

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json { namingStrategy = JsonNamingStrategy.SnakeCase }

    @Test
    fun `finance report reads bed counts from API wire names`() {
        val report = json.decodeFromString<FinanceReportDto>(
            """{
                "collected_paise": 1000,
                "deposits_paise": 2000,
                "accrued_paise": 3000,
                "outstanding_paise": 4000,
                "residents_active": 1,
                "residents_total": 1,
                "beds_total": 2,
                "beds_occupied": 1
            }""",
        )

        assertEquals(2, report.bedsTotal)
        assertEquals(1, report.bedsOccupied)
    }

    @Test
    fun `finance branch reads bed counts from API wire names`() {
        val branch = json.decodeFromString<FinanceBranchDto>(
            """{
                "branch_id": "branch-1",
                "branch_name": "Main Branch",
                "collected_paise": 1000,
                "deposits_paise": 2000,
                "accrued_paise": 3000,
                "outstanding_paise": 4000,
                "residents_active": 1,
                "residents_total": 1,
                "beds_total": 2,
                "beds_occupied": 1
            }""",
        )

        assertEquals(2, branch.bedsTotal)
        assertEquals(1, branch.bedsOccupied)
    }
}
