package com.abbas.scanui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.abbas.scanui.data.ScanDataSource
import com.abbas.scanui.data.SimulatedBackpackSource
import com.abbas.scanui.gl.PointCloudGLSurfaceView
import com.abbas.scanui.io.ScanFileIO
import com.abbas.scanui.model.ScanPoint
import java.io.File

/**
 * Proof-of-concept screen demonstrating the four requirements from the
 * eParT HiWi posting on a single tablet-sized layout:
 *   1) Android UI                       -> this Activity + layout
 *   2) Live 3D visualization on tablet  -> PointCloudGLSurfaceView
 *   3) System status visualization      -> status panel (top-left)
 *   4) Import/export of captured data   -> Export/Import buttons (.ply)
 *
 * The hardware feed is simulated (SimulatedBackpackSource) so the whole
 * pipeline runs standalone without the real Datenrucksack -- swapping in
 * a real driver behind the ScanDataSource interface is the only change
 * needed to go from this demo to the real system.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var glView: PointCloudGLSurfaceView
    private lateinit var dataSource: ScanDataSource
    private val uiHandler = Handler(Looper.getMainLooper())
    private var pollRunnable: Runnable? = null
    private var allCapturedPoints = mutableListOf<ScanPoint>()
    private var scanning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        glView = findViewById(R.id.glSurfaceView)
        dataSource = SimulatedBackpackSource()

        val stateLabel: TextView = findViewById(R.id.stateLabel)
        val pointsLabel: TextView = findViewById(R.id.pointsLabel)
        val rateLabel: TextView = findViewById(R.id.rateLabel)
        val batteryLabel: TextView = findViewById(R.id.batteryLabel)
        val storageLabel: TextView = findViewById(R.id.storageLabel)

        val btnStartStop: Button = findViewById(R.id.btnStartStop)
        val btnExport: Button = findViewById(R.id.btnExport)
        val btnImport: Button = findViewById(R.id.btnImport)

        btnStartStop.setOnClickListener {
            scanning = !scanning
            if (scanning) {
                dataSource.start()
                btnStartStop.text = "Stop Scan"
            } else {
                dataSource.stop()
                btnStartStop.text = "Start Scan"
            }
        }

        btnExport.setOnClickListener {
            val outFile = File(getExternalFilesDir(null), "scan_export.ply")
            ScanFileIO.exportToPly(allCapturedPoints, outFile)
            stateLabel.text = "STATE: EXPORTED (${outFile.name})"
        }

        btnImport.setOnClickListener {
            val inFile = File(getExternalFilesDir(null), "scan_export.ply")
            if (inFile.exists()) {
                val imported = ScanFileIO.importFromPly(inFile)
                allCapturedPoints = imported.toMutableList()
                glView.pointCloudRenderer.submitPoints(allCapturedPoints, append = false)
                stateLabel.text = "STATE: IMPORTED (${imported.size} pts)"
            } else {
                stateLabel.text = "STATE: NO FILE TO IMPORT"
            }
        }

        // Poll the (simulated) hardware feed at ~15 Hz and push to renderer + status panel.
        pollRunnable = object : Runnable {
            override fun run() {
                if (scanning) {
                    val batch = dataSource.nextBatch()
                    allCapturedPoints.addAll(batch)
                    glView.pointCloudRenderer.submitPoints(batch, append = true)
                }
                val status = dataSource.currentStatus()
                stateLabel.text = "STATE: ${status.state.name}"
                pointsLabel.text = "Points captured: ${allCapturedPoints.size}"
                rateLabel.text = "Scan rate: %.1f Hz".format(status.scanRateHz)
                batteryLabel.text = "Battery: ${status.batteryPercent}%"
                storageLabel.text = "Storage free: ${status.storageFreeMb} MB"

                uiHandler.postDelayed(this, 66) // ~15 Hz UI refresh
            }
        }
        uiHandler.post(pollRunnable!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        pollRunnable?.let { uiHandler.removeCallbacks(it) }
    }
}
