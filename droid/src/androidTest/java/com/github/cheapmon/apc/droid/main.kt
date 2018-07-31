package com.github.cheapmon.apc.droid

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import org.junit.Test
import org.junit.runner.RunWith
import java.io.PrintWriter
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Paths

data class Config(val ids: List<String>, val mode: String, val algorithm: String)

@RunWith(AndroidJUnit4::class)
class DroidMain {

    @Test
    fun main() {
        val config = parseExtra()
        with(Logger) {
            info("Droid")
            line()
            info("Found ids:")
            for (id in config.ids) {
                info("* $id")
            }
            info(String.format("Extraction mode is %s", config.mode))
            info(String.format("Using %s", config.algorithm))
            line()
        }
        send(finished = true)
    }

    private fun parseExtra(): Config {
        with(InstrumentationRegistry.getArguments()) {
            val ids = Files.readAllLines(Paths.get(getString("file"))).toList()
            val mode = getString("mode")
            val algorithm = getString("algorithm")
            return Config(ids, mode, algorithm)
        }
    }

    private fun send(txt: String = "", id: String = "", finished: Boolean = false) {
        val s = Socket("10.0.2.2", 2000)
        with(PrintWriter(s.getOutputStream())) {
            if (finished) {
                print("\n")
                print("OK")
            } else {
                print("$id\n")
                print(txt)
                print("\n")
                print("---")
            }
            flush()
            close()
        }
        s.close()
    }

}

object Logger {
    fun info(msg: Any?) = Log.i(DroidMain::class.simpleName, "$msg")
    fun line() = Log.i(DroidMain::class.simpleName, "=".repeat(80))
}