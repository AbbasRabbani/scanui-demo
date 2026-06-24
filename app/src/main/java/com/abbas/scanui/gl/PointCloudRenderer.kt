package com.abbas.scanui.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.abbas.scanui.model.ScanPoint
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Minimal GLES 2.0 renderer for a live-updating 3D point cloud.
 *
 * Kept deliberately small: just enough to prove the rendering pipeline
 * (vertex buffer upload, shader, projection/view matrices, point rendering)
 * works end-to-end on a tablet and can keep up with a live-updating buffer.
 * A production version would add picking, color legends, grid/axes, and
 * camera gestures (pinch-zoom/orbit), but the core loop is the same.
 */
class PointCloudRenderer : GLSurfaceView.Renderer {

    @Volatile
    private var points: List<ScanPoint> = emptyList()

    private var vertexBuffer: FloatBuffer? = null
    private var program = 0
    private var positionHandle = 0
    private var colorHandle = 0
    private var mvpHandle = 0

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var rotationDeg = 0f

    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec3 aPosition;
        attribute vec3 aColor;
        varying vec3 vColor;
        void main() {
            vColor = aColor;
            gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
            gl_PointSize = 6.0;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        varying vec3 vColor;
        void main() {
            gl_FragColor = vec4(vColor, 1.0);
        }
    """.trimIndent()

    /** Called from the UI thread when a new batch arrives from the data source. */
    fun submitPoints(newPoints: List<ScanPoint>, append: Boolean) {
        points = if (append) (points + newPoints).takeLast(20000) else newPoints
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.07f, 0.08f, 0.1f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        colorHandle = GLES20.glGetAttribLocation(program, "aColor")
        mvpHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 2f, 30f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val snapshot = points
        if (snapshot.isEmpty()) return

        rotationDeg = (rotationDeg + 0.3f) % 360f

        Matrix.setLookAtM(viewMatrix, 0, 0f, 2f, 8f, 0f, 0f, 0f, 0f, 1f, 0f)
        val rotationMatrix = FloatArray(16)
        Matrix.setRotateM(rotationMatrix, 0, rotationDeg, 0f, 1f, 0f)
        val rotatedView = FloatArray(16)
        Matrix.multiplyMM(rotatedView, 0, viewMatrix, 0, rotationMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, rotatedView, 0)

        val buf = buildVertexBuffer(snapshot)

        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)

        buf.position(0)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 6 * 4, buf)

        buf.position(3)
        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glVertexAttribPointer(colorHandle, 3, GLES20.GL_FLOAT, false, 6 * 4, buf)

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, snapshot.size)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }

    private fun buildVertexBuffer(pts: List<ScanPoint>): FloatBuffer {
        val data = FloatArray(pts.size * 6)
        pts.forEachIndexed { i, p ->
            val o = i * 6
            data[o] = p.x; data[o + 1] = p.y; data[o + 2] = p.z
            data[o + 3] = p.r; data[o + 4] = p.g; data[o + 5] = p.b
        }
        val bb = ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer()
        fb.put(data)
        fb.position(0)
        vertexBuffer = fb
        return fb
    }

    private fun loadShader(type: Int, code: String): Int {
        return GLES20.glCreateShader(type).also {
            GLES20.glShaderSource(it, code)
            GLES20.glCompileShader(it)
        }
    }
}
