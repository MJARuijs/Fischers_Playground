package com.mjaruijs.fischersplayground.opengl.surfaceviews

import android.opengl.EGL14.EGL_LEVEL
import android.opengl.EGL14.EGL_SAMPLES
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay

class ConfigChooser : GLSurfaceView.EGLConfigChooser {

    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig? {
        val attributes = intArrayOf(
//            EGL_LEVEL, 0,
            EGL_SAMPLES, 4,
            EGL10.EGL_RED_SIZE,        8,
            EGL10.EGL_GREEN_SIZE,      8,
            EGL10.EGL_BLUE_SIZE,       8,
            EGL10.EGL_ALPHA_SIZE,      8,
            EGL10.EGL_DEPTH_SIZE,      16,
            EGL10.EGL_RENDERABLE_TYPE, 4, /* EGL_OPENGL_ES2_BIT */
            EGL10.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val configCounts = IntArray(1)

        egl.eglChooseConfig(display, attributes, configs, 1, configCounts)

        if (configCounts[0] == 0) {
            return null
        }
        return configs[0]
    }
}