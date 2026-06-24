package com.abbas.scanui.model

/**
 * A single point in the live 3D scan stream.
 * In the real eParT Datenrucksack system this would come from the
 * scanner's point-cloud driver (e.g. over USB/Bluetooth/network socket).
 * Here it's generated synthetically so the UI/visualization layer can be
 * built and demonstrated independently of the real hardware.
 */
data class ScanPoint(
    val x: Float,
    val y: Float,
    val z: Float,
    val r: Float = 1f,
    val g: Float = 1f,
    val b: Float = 1f
)

/** Mirrors the kind of state a wearable scanning rig would expose. */
enum class SystemState {
    IDLE,
    CONNECTING,
    SCANNING,
    PAUSED,
    EXPORTING,
    ERROR
}

/** Lightweight snapshot of "system health" shown in the status panel. */
data class SystemStatus(
    val state: SystemState,
    val pointsCaptured: Int,
    val scanRateHz: Float,
    val batteryPercent: Int,
    val storageFreeMb: Int,
    val message: String = ""
)
