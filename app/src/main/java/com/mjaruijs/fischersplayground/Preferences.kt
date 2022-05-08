package com.mjaruijs.fischersplayground

// TODO: Can probably be removed
object Preferences {

    private val data = HashMap<String, String>()

    fun get(key: String) = data[key]

    fun put(key: String, value: String) {
        data[key] = value
    }

}