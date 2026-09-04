package com.aalaya.pg.feature.invitations

import com.aalaya.pg.core.domain.model.Role
import com.aalaya.pg.navigation.digilockerRoute
import com.aalaya.pg.navigation.invitationDetailRoute
import com.aalaya.pg.navigation.invitationReviewRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InvitationPresentationTest {
    @Test
    fun `all backend onboarding actions have clear copy`() {
        assertEquals("Review and accept or decline this invitation.", onboardingNextActionLabel("accept_or_decline"))
        assertEquals("Continue the tenant onboarding steps.", onboardingNextActionLabel("continue_onboarding"))
        assertEquals("Complete DigiLocker identity verification.", onboardingNextActionLabel("complete_identity_verification"))
        assertEquals("Identity verification needs review. Contact the property team.", onboardingNextActionLabel("contact_property"))
        assertEquals("Your property team is reviewing the invitation.", onboardingNextActionLabel("wait_for_approval"))
        assertEquals("Your tenant access is being activated.", onboardingNextActionLabel("activation_in_progress"))
        assertEquals("Activation is complete. Switch to this property to continue.", onboardingNextActionLabel("open_tenant_home"))
        assertEquals("No action is currently required.", onboardingNextActionLabel("none"))
    }

    @Test
    fun `owner and manager can manage invitations but tenant cannot`() {
        assertTrue(Role.Owner.isStaff)
        assertTrue(Role.Manager.isStaff)
        assertFalse(Role.Tenant.isStaff)
        assertFalse(Role.Prospect.isStaff)
    }

    @Test
    fun `deep link destinations contain only invitation identifier`() {
        val id = "10000000-0000-0000-0000-000000000001"
        assertEquals("invitation/$id", invitationDetailRoute(id))
        assertEquals("invitation-review/$id", invitationReviewRoute(id))
        assertEquals("digilocker/$id", digilockerRoute(id))
        listOf(invitationDetailRoute(id), invitationReviewRoute(id), digilockerRoute(id)).forEach { route ->
            assertFalse(route.contains("token", ignoreCase = true))
            assertFalse(route.contains("email", ignoreCase = true))
        }
    }
}
