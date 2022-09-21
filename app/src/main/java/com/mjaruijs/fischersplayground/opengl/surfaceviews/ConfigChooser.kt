package com.mjaruijs.fischersplayground.opengl.surfaceviews

import android.content.Context
import android.opengl.EGL14.*
import android.opengl.GLSurfaceView
import android.widget.Toast
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay

class ConfigChooser(private val context: Context) : GLSurfaceView.EGLConfigChooser {

    override fun chooseConfig(egl: EGL10?, display: EGLDisplay?): EGLConfig? {
        if (egl == null) {
            Toast.makeText(context, "Couldn't choose an EGL configuration.. EGL is null..", Toast.LENGTH_SHORT).show()
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
            EGL_NONE
        )

        val n = 20

        val configurations = arrayOfNulls<EGLConfig>(n)
        val numberOfConfigurations = intArrayOf(n)
        egl.eglChooseConfig(display, attributes, configurations, n, numberOfConfigurations)

        if (numberOfConfigurations[0] == 0) {
            return null
        }

        var maxSamples = -1
        var bestConfigIndex = 0

        for ((i, config) in configurations.withIndex()) {
            if (config == null) {
                continue
            }

            val attributeValue = IntArray(1)
            egl.eglGetConfigAttrib(display, config, EGL_SAMPLES, attributeValue)

            val configSamples = attributeValue[0]
            if (configSamples > maxSamples) {
                maxSamples = configSamples
                bestConfigIndex = i
            }
        }

        return configurations[bestConfigIndex]
    }
}