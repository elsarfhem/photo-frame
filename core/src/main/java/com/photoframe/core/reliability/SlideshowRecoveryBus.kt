package com.photoframe.core.reliability

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Event bus for watchdog → ViewModel communication without direct coupling.
 *
 * When the watchdog's nextPhoto() recovery attempt fails, it emits a
 * RecoveryEvent here. The SlideshowViewModel observes and triggers a full
 * re-initialization (reload photos, reset buffer) — escalating past the
 * dead ACTION_SLIDESHOW_STALLED broadcast.
 */
@Singleton
class SlideshowRecoveryBus @Inject constructor() {
    private val _events = MutableSharedFlow<RecoveryEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<RecoveryEvent> = _events.asSharedFlow()

    fun emit(event: RecoveryEvent) {
        _events.tryEmit(event)
    }
}

sealed class RecoveryEvent {
    /** Watchdog detected a stall and the repo-level advance failed. Need full re-init. */
    object FullReloadRequested : RecoveryEvent()
}
