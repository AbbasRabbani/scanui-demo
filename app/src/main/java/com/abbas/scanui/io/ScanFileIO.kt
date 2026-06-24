package com.abbas.scanui.io

import com.abbas.scanui.model.ScanPoint
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Handles import/export of captured point clouds.
 *
 * Uses ASCII PLY because it's a real, widely-supported point-cloud format
 * (readable in CloudCompare, MeshLab, etc.) rather than inventing a custom
 * one -- exported scans should be usable outside this app, which matters
 * for an actual research pipeline.
 */
object ScanFileIO {

    fun exportToPly(points: List<ScanPoint>, outFile: File) {
        outFile.bufferedWriter().use { w ->
            w.write("ply\n")
            w.write("format ascii 1.0\n")
            w.write("element vertex ${points.size}\n")
            w.write("property float x\n")
            w.write("property float y\n")
            w.write("property float z\n")
            w.write("property uchar red\n")
            w.write("property uchar green\n")
            w.write("property uchar blue\n")
            w.write("end_header\n")
            for (p in points) {
                val r = (p.r * 255).toInt().coerceIn(0, 255)
                val g = (p.g * 255).toInt().coerceIn(0, 255)
                val b = (p.b * 255).toInt().coerceIn(0, 255)
                w.write("${p.x} ${p.y} ${p.z} $r $g $b\n")
            }
        }
    }

    fun importFromPly(inFile: File): List<ScanPoint> {
        val result = mutableListOf<ScanPoint>()
        BufferedReader(InputStreamReader(inFile.inputStream())).use { reader ->
            var line = reader.readLine()
            var vertexCount = 0
            // Skip header, but capture vertex count
            while (line != null && line.trim() != "end_header") {
                if (line.startsWith("element vertex")) {
                    vertexCount = line.trim().split(" ").last().toIntOrNull() ?: 0
                }
                line = reader.readLine()
            }
            var read = 0
            while (read < vertexCount) {
                val dataLine = reader.readLine() ?: break
                val parts = dataLine.trim().split(" ")
                if (parts.size >= 6) {
                    result.add(
                        ScanPoint(
                            x = parts[0].toFloat(),
                            y = parts[1].toFloat(),
                            z = parts[2].toFloat(),
                            r = parts[3].toFloat() / 255f,
                            g = parts[4].toFloat() / 255f,
                            b = parts[5].toFloat() / 255f
                        )
                    )
                }
                read++
            }
        }
        return result
    }
}
