package com.kaze.liveshell

import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

class ShellProcess {
    @Volatile private var process: Process? = null
    private var stdin: DataOutputStream? = null
    private var stdout: BufferedReader? = null
    private var stderr: BufferedReader? = null

    fun start() {
        process = Runtime.getRuntime().exec("/system/bin/sh")
        stdin = DataOutputStream(process!!.outputStream)
        stdout = BufferedReader(InputStreamReader(process!!.inputStream))
        stderr = BufferedReader(InputStreamReader(process!!.errorStream))
        Log.d("LiveShell", "Shell started, pid=${getPid()}")
    }

    fun write(data: String) {
        try {
            stdin?.write(data.toByteArray())
            stdin?.flush()
        } catch (e: Exception) {
            Log.e("LiveShell", "stdin write failed", e)
        }
    }

    fun readStdout(): String {
        val sb = StringBuilder()
        try {
            while (stdout?.ready() == true) {
                val char = stdout?.read() ?: break
                sb.append(char.toChar())
            }
        } catch (_: Exception) {}
        return sb.toString()
    }

    fun readStderr(): String {
        val sb = StringBuilder()
        try {
            while (stderr?.ready() == true) {
                val char = stderr?.read() ?: break
                sb.append(char.toChar())
            }
        } catch (_: Exception) {}
        return sb.toString()
    }

    fun isAlive(): Boolean {
        return try {
            process?.exitValue()
            false
        } catch (_: IllegalThreadStateException) {
            true
        }
    }

    fun destroy() {
        process?.destroyForcibly()
        stdin?.close()
        stdout?.close()
        stderr?.close()
    }

    private fun getPid(): Int {
        return try {
            val pidField = process?.javaClass?.getDeclaredField("pid")
            pidField?.isAccessible = true
            pidField?.getInt(process) ?: -1
        } catch (_: Exception) { -1 }
    }
}
