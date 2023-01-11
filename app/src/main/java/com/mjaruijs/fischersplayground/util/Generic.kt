package com.mjaruijs.fischersplayground.util

class Generic<T : Any>(val klass: Class<T>) {

    fun checkType(t: Any): Boolean {
        return klass.isAssignableFrom(t.javaClass)
    }

    companion object {
        inline operator fun <reified T : Any>invoke() = Generic(T::class.java)
    }

}