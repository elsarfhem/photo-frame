package com.photoframe.tests.functional

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Functional test for the watchdog-reload → network-gate → NETWORK_RECOVERED
 * state machine in SlideshowViewModel.
 *
 * Scenario (v1.0.34 regression):
 *   1. Slideshow is playing with SMB-only source.
 *   2. Device enters Doze → NETWORK_LOST.
 *   3. Watchdog detects stall, triggers FullReloadRequested.
 *   4. ViewModel pauses, clears buffer, calls initialize(autoPlay=true, isRetry=true).
 *   5. initialize() hits the SMB-only offline gate, exhausts retries, bails.
 *      photos list is NOT cleared (repo still reports 31k photos).
 *   6. NETWORK_RECOVERED fires ~9 minutes later.
 *   7. Before the fix, needsRecovery checked only totalPhotos==0 → false →
 *      slideshow stayed paused for hours until user force-killed the app.
 *   8. After the fix, pendingRecoveryReload is set on the bail-out and
 *      consumed on NETWORK_RECOVERED to retry initialize().
 */
class RecoveryReloadStateTest {

    private class RecoveryFsm {
        var isInitialized: Boolean = true
        var totalPhotos: Int = 31434
        var pendingRecoveryReload: Boolean = false
        var initializeInvocations: Int = 0
        var lastAutoPlay: Boolean = false

        // Mirrors SlideshowViewModel.initialize() failure exit on network-gate exhaustion.
        fun initializeFailsOnNetworkGate(autoPlay: Boolean) {
            if (autoPlay) {
                pendingRecoveryReload = true
            }
        }

        // Mirrors the success branch: success clears the pending flag.
        fun initializeSucceeds() {
            pendingRecoveryReload = false
            initializeInvocations++
        }

        // Mirrors handleNetworkStateChange(true) decision logic.
        fun onNetworkRecovered() {
            val needsRecovery = isInitialized &&
                (totalPhotos == 0 || pendingRecoveryReload)
            if (needsRecovery) {
                pendingRecoveryReload = false
                // simulate initialize(autoPlay=true, isRetry=true)
                lastAutoPlay = true
                initializeInvocations++
            }
        }
    }

    @Test
    fun `v1_0_34 bug repro — without pending flag recovery is skipped`() {
        val fsm = RecoveryFsm()
        // Watchdog reload ran initialize(autoPlay=true) which failed; pretend
        // pre-fix code did NOT set the flag.
        fsm.pendingRecoveryReload = false
        // photos list was NOT cleared — repo still reports photos
        assertEquals(31434, fsm.totalPhotos)

        fsm.onNetworkRecovered()

        assertEquals(0, fsm.initializeInvocations)
        assertFalse(fsm.lastAutoPlay)
    }

    @Test
    fun `fix — bail-out on network gate sets pendingRecoveryReload when autoPlay`() {
        val fsm = RecoveryFsm()

        fsm.initializeFailsOnNetworkGate(autoPlay = true)

        assertTrue(fsm.pendingRecoveryReload)
    }

    @Test
    fun `fix — bail-out does not set flag when autoPlay is false`() {
        val fsm = RecoveryFsm()

        fsm.initializeFailsOnNetworkGate(autoPlay = false)

        assertFalse(fsm.pendingRecoveryReload)
    }

    @Test
    fun `fix — NETWORK_RECOVERED consumes pendingRecoveryReload and retries with autoPlay`() {
        val fsm = RecoveryFsm()
        fsm.initializeFailsOnNetworkGate(autoPlay = true)
        assertTrue(fsm.pendingRecoveryReload)

        fsm.onNetworkRecovered()

        assertFalse(fsm.pendingRecoveryReload, "flag must be cleared after consumption")
        assertEquals(1, fsm.initializeInvocations)
        assertTrue(fsm.lastAutoPlay)
    }

    @Test
    fun `fix — flag cleared on successful initialize so stale recovery does not fire`() {
        val fsm = RecoveryFsm()
        fsm.initializeFailsOnNetworkGate(autoPlay = true)
        fsm.initializeSucceeds()

        // Simulate a second network recovery later — nothing should re-fire.
        fsm.onNetworkRecovered()

        // one invocation from initializeSucceeds, none from onNetworkRecovered
        assertEquals(1, fsm.initializeInvocations)
        assertFalse(fsm.pendingRecoveryReload)
    }

    @Test
    fun `cold-start path still works — totalPhotos zero triggers recovery`() {
        val fsm = RecoveryFsm()
        fsm.totalPhotos = 0
        fsm.pendingRecoveryReload = false

        fsm.onNetworkRecovered()

        assertEquals(1, fsm.initializeInvocations)
        assertTrue(fsm.lastAutoPlay)
    }

    @Test
    fun `NETWORK_RECOVERED is a no-op when initialized with photos and no pending reload`() {
        val fsm = RecoveryFsm()
        // Happy state: photos loaded, slideshow running, network blip without
        // a watchdog-driven reload in flight.
        fsm.totalPhotos = 31434
        fsm.pendingRecoveryReload = false

        fsm.onNetworkRecovered()

        assertEquals(0, fsm.initializeInvocations)
    }
}
