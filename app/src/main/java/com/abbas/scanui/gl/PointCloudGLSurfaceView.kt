package com.abbas.scanui.gl

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet

class PointCloudGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    val pointCloudRenderer = PointCloudRenderer()

    init {
        setEGLContextClientVersion(2)
        setRenderer(pointCloudRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}
