package com.example.mydelivery.util

import android.util.Log
import java.lang.StringBuilder

class MyLogger {
    companion object{
        const val TAG ="Custom Log"

        fun d(message : String) {
            Log.d(TAG, buildLogMsg(message))
        }

        fun v(message : String) {
            Log.v(TAG, buildLogMsg(message))
        }

        fun i(message : String) {
            Log.i(TAG, buildLogMsg(message))
        }

        fun w(message : String) {
            Log.w(TAG, buildLogMsg(message))
        }

        fun e(message : String) {
            Log.e(TAG, buildLogMsg(message))
        }

        fun buildLogMsg(message : String) : String {
            val ste = Thread.currentThread().stackTrace[4]
            val sb = StringBuilder()
            sb.append("[")
            sb.append(ste.fileName.replace(".java", "", false))
            sb.append("::")
            sb.append(ste.methodName)
            sb.append("]")
            sb.append(message)
            return sb.toString()
        }
    }
}