package com.github.cheapmon.apc.droid

import android.support.test.runner.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import java.io.PrintWriter
import java.net.Socket

@RunWith(AndroidJUnit4::class)
class DroidMain {

    @Test
    fun main() {
        send(finished = true)
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