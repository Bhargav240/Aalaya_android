package com.aalaya.pg.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleTest {
    @Test
    fun `wire values match the backend`() {
        assertEquals(Role.Owner, Role.fromWire("owner"))
        assertEquals(Role.Manager, Role.fromWire("manager"))
        assertEquals(Role.Tenant, Role.fromWire("tenant"))
        assertNull(Role.fromWire("superadmin"))
        assertNull(Role.fromWire(null))
    }

    @Test
    fun `capabilities are role-gated correctly`() {
        assertTrue(Role.Owner.isStaff)
        assertTrue(Role.Owner.canApprove)
        assertTrue(Role.Owner.canManage)

        assertTrue(Role.Manager.isStaff)
        assertFalse(Role.Manager.canApprove) // only owners approve change requests
        assertTrue(Role.Manager.canManage)

        assertFalse(Role.Tenant.isStaff)
        assertFalse(Role.Tenant.canApprove)
        assertFalse(Role.Tenant.canManage)
    }

    @Test
    fun `plan kind defaults to monthly on unknown input`() {
        assertEquals(PlanKind.Monthly, PlanKind.fromWire("nonsense"))
        assertEquals(PlanKind.Weekly, PlanKind.fromWire("weekly"))
        assertEquals(PlanKind.Daily, PlanKind.fromWire("daily"))
    }
}
