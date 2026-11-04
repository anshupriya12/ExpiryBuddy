package com.expirybuddy.app.testing

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** A clock tests can move by hand. */
class MutableClock(
    var now: Instant,
    private val zone: ZoneId = ZoneId.of("UTC"),
) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)
    override fun instant(): Instant = now
}

/** A clock that advances with a test scheduler's virtual time, for testing delay-based code. */
@OptIn(ExperimentalCoroutinesApi::class)
class VirtualTimeClock(
    private val scheduler: TestCoroutineScheduler,
    private val start: Instant,
    private val zone: ZoneId = ZoneId.of("UTC"),
) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = VirtualTimeClock(scheduler, start, zone)
    override fun instant(): Instant = start.plusMillis(scheduler.currentTime)
}
