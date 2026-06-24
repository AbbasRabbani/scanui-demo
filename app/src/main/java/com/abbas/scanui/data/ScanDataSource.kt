package com.abbas.scanui.data

import com.abbas.scanui.model.ScanPoint
import com.abbas.scanui.model.SystemState
import com.abbas.scanui.model.SystemStatus
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Stands in for the real backpack hardware interface.
 *
 * In production this class would be replaced by a driver that reads from
 * the actual scanning unit (likely over USB-OTG or a TCP/Bluetooth socket,
 * depending on how the Datenrucksack exposes its data). The rest of the
 * app -- the GL renderer and the status UI -- doesn't need to know the
 * difference, which is the point of isolating it behind this interface.
 */
interface ScanDataSource {
    fun start()
    fun stop()
    fun nextBatch(): List<ScanPoint>
    fun currentStatus(): SystemStatus
}

class SimulatedBackpackSource : ScanDataSource {

    private var running = false
    private var angle = 0f
    private var pointsCaptured = 0
    private val rng = Random(42)

    override fun start() {
        running = true
    }

    override fun stop() {
        running = false
    }

    /**
     * Generates one "sweep" of points per call, as if a rotating LIDAR-style
     * sensor on the backpack just produced a new slice of the room.
     * Shaped like a noisy cylindrical room scan so it's visually meaningful
     * once rendered, rather than pure random noise.
     */
    override fun nextBatch(): List<ScanPoint> {
        if (!running) return emptyList()

        val batch = mutableListOf<ScanPoint>()
        val pointsThisSweep = 120
        for (i in 0 until pointsThisSweep) {
            val theta = (i.toFloat() / pointsThisSweep) * 2f * Math.PI.toFloat()
            val radius = 3.5f + rng.nextFloat() * 0.15f
            val height = (rng.nextFloat() - 0.5f) * 2.5f + sin(angle + theta) * 0.2f

            val x = radius * cos(theta + angle)
            val z = radius * sin(theta + angle)
            val y = height

            // Color by height purely so the point cloud reads visually in the demo
            val t = (y + 1.5f) / 3f
            batch.add(ScanPoint(x, y, z, r = t, g = 0.6f, b = 1f - t))
        }

        angle += 0.05f
        pointsCaptured += batch.size
        return batch
    }

    override fun currentStatus(): SystemStatus {
        return SystemStatus(
            state = if (running) SystemState.SCANNING else SystemState.IDLE,
            pointsCaptured = pointsCaptured,
            scanRateHz = if (running) 12f + rng.nextFloat() * 2f else 0f,
            batteryPercent = (78 - (pointsCaptured / 4000)).coerceIn(0, 100),
            storageFreeMb = (4096 - pointsCaptured / 50).coerceAtLeast(0),
            message = if (running) "Sweep active" else "Idle"
        )
    }
}
