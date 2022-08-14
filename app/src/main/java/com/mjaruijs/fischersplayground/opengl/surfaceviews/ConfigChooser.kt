package com.mjaruijs.fischersplayground.opengl.surfaceviews

import android.opengl.EGL14.*
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay

class ConfigChooser : GLSurfaceView.EGLConfigChooser {

    override fun chooseConfig(egl: EGL10?, display: EGLDisplay?): EGLConfig? {
        if (egl == null) {
            println("Couldn't choose an EGL configuration.. EGL is null..")
            return null
        }

        val attributes = intArrayOf(
            EGL_LEVEL, 0,
            EGL_RENDERABLE_TYPE, 4,
            EGL_COLOR_BUFFER_TYPE, EGL_RGB_BUFFER,
            EGL_RED_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_DEPTH_SIZE, 16,
            EGL_SAMPLE_BUFFERS, 1,
            EGL_SAMPLES, 4,
            EGL_NONE
        )

        val configurations = arrayOfNulls<EGLConfig>(3)
        val numberOfConfigurations = intArrayOf(1)
        egl.eglChooseConfig(display, attributes, configurations, 1, numberOfConfigurations)

        if (numberOfConfigurations[0] == 0) {
            println("Failed to retrieve an EGL configuration..")
            return null
        }

        return configurations[0]!!
    }

}