package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Lead
import com.example.data.model.PipelineStage
import com.example.data.model.Project
import com.example.data.model.ProjectStage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SolarCrmRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createLead_validPayload_createsDocumentAndReturnsId() {
        runBlocking {
            val aliceUid = signInTestUser(ALICE_EMAIL)
            val repository = SolarCrmRepository(firestore)

            val lead = Lead(
                customerName = "Robert Solar",
                phone = "555-432-1098",
                monthlyElectricBill = 280.0,
                pipelineStage = PipelineStage.NEW_LEAD.name
            )

            val result = withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.createLead(aliceUid, lead)
            }
            assertTrue(result.isSuccess)
            val leadId = result.getOrThrow()
            assertTrue(leadId.isNotEmpty())

            val savedLead = repository.getLead(aliceUid, leadId)
            assertEquals("Robert Solar", savedLead?.customerName)
        }
    }

    @Test
    fun leadRepository_withLeadFields_createsAndFetchesLead() {
        runBlocking {
            val aliceUid = signInTestUser(ALICE_EMAIL)
            val leadRepository: LeadRepository = LeadRepositoryImpl(firestore)

            val prospect = Lead(
                name = "Samantha Miller",
                contact = "samantha@example.com / 555-234-5678",
                status = "NEW_LEAD",
                propertyDetails = "Tile Roof, 2400 sq ft, High Sun Irradiance",
                monthlyElectricBill = 310.0
            )

            val addResult = withTimeout(DEFAULT_TIMEOUT_MS) {
                leadRepository.addLead(aliceUid, prospect)
            }
            assertTrue(addResult.isSuccess)
            val leadId = addResult.getOrThrow()

            val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
                leadRepository.getLeadById(aliceUid, leadId)
            }
            assertNotNull(fetched)
            assertEquals("Samantha Miller", fetched?.displayName)
            assertEquals("samantha@example.com / 555-234-5678", fetched?.displayContact)
            assertEquals("NEW_LEAD", fetched?.displayStatus)
            assertEquals("Tile Roof, 2400 sq ft, High Sun Irradiance", fetched?.displayPropertyDetails)
        }
    }

    @Test
    fun createProject_validPayload_createsDocumentAndReturnsId() {
        runBlocking {
            val aliceUid = signInTestUser(ALICE_EMAIL)
            val repository = SolarCrmRepository(firestore)

            val project = Project(
                customerName = "Sarah Jenkins",
                phone = "555-987-6543",
                systemSizeKw = 9.2,
                totalContractPrice = 28500.0,
                projectStage = ProjectStage.SITE_AUDIT.name
            )

            val result = withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.createProject(aliceUid, project)
            }
            assertTrue(result.isSuccess)
            val projectId = result.getOrThrow()
            assertTrue(projectId.isNotEmpty())
        }
    }

    @Test
    fun observeLeads_authenticatedOwner_emitsRealtimeUpdates() {
        runBlocking {
            val aliceUid = signInTestUser(ALICE_EMAIL)
            val repository = SolarCrmRepository(firestore)

            val lead = Lead(
                customerName = "David Ray",
                phone = "555-321-7654",
                pipelineStage = PipelineStage.CONTACTED.name
            )
            val leadId = withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.createLead(aliceUid, lead).getOrThrow()
            }

            val emittedLeads = withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeLeads(aliceUid).first { list -> list.any { it.id == leadId } }
            }
            assertTrue(emittedLeads.any { it.id == leadId })
        }
    }

    @Test
    fun getLead_crossUserAccess_failsWithPermissionDenied() {
        runBlocking {
            val aliceUid = signInTestUser(ALICE_EMAIL)
            val aliceRepo = SolarCrmRepository(firestore)
            val leadId = withTimeout(DEFAULT_TIMEOUT_MS) {
                aliceRepo.createLead(
                    aliceUid,
                    Lead(
                        customerName = "Alice Private",
                        phone = "555-000-1111",
                        pipelineStage = PipelineStage.NEW_LEAD.name
                    )
                ).getOrThrow()
            }

            // Bob signs in and tries to read Alice's lead directly
            signInTestUser(BOB_EMAIL)
            val bobDb = firestore
            try {
                withTimeout(DEFAULT_TIMEOUT_MS) {
                    bobDb.collection("users").document(aliceUid).collection("leads").document(leadId).get().await()
                }
                fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
            } catch (e: Exception) {
                val fse = e.unwrapFirestoreException()
                assertNotNull("Expected FirebaseFirestoreException", fse)
                assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fse?.code)
            }
        }
    }

    @Test
    fun observeProjects_unauthenticatedUser_failsWithPermissionDenied() {
        runBlocking {
            auth.signOut()
            val repository = SolarCrmRepository(firestore)

            try {
                withTimeout(FLOW_TIMEOUT_MS) {
                    repository.observeProjects("unauth_user_id").first()
                }
                fail("Expected Exception PERMISSION_DENIED")
            } catch (e: Exception) {
                val fse = e.unwrapFirestoreException()
                assertNotNull("Expected nested FirebaseFirestoreException, got $e", fse)
                assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fse?.code)
            }
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@testsolar.com"
        const val BOB_EMAIL = "bob@testsolar.com"
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
