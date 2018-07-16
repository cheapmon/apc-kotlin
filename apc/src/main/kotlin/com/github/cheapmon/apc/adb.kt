/*
 * Copyright 2018 Simon Kaleschke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.cheapmon.apc

import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * List of available Android devices attached to computer.
 *
 * This runs `adb devices`. Output of the form
 * ```
 * List of devices attached
 * FA6ATYJ00045	device
 * emulator-5554	device
 * ```
 * is parsed to
 * ```
 * ["FA6ATYJ00045", "emulator-5554"]
 * ```
 *
 * Note that a device needs to have Android debugging enabled to appear in the list.
 */
val deviceList: Array<out String> by lazy {
    run("adb", "devices").lines().skip(1)
            .filter { line -> line.contains("device") }
            .map { line -> line.split("\t").first() }
            .toArray<String> { size -> arrayOfNulls(size) }
}

/**
 * Run system process and wait for termination.
 *
 * ```
 * run("adb", "shell")
 * ```
 * runs `adb` with the `shell` option.
 *
 * @param commands tokens of system call, in order
 * @return output of finished process
 * @throws Exception if call execution ends abnormally
 */
private fun run(vararg commands: String): InputStream {
    Logger.debug(commands.joinToString(" "))
    with(ProcessBuilder(*commands).start()) {
        val exitCode = waitFor()
        if (exitCode == 0) {
            return inputStream
        } else {
            Logger.debug(errorStream.string())
            throw Exception(errorStream.string())
        }
    }
}

/**
 * Send ADB calls only to a single device.
 *
 * @property config extraction configuration
 * @see deviceList
 */
class ADBConnection(private val config: Config) {
    private fun runOnDevice(vararg commands: String): InputStream {
        return run("adb", "-s", config.device, *commands)
    }

    /**
     * Install extraction binary on Android device.
     */
    fun installAPK() {
        val outputFolder = arrayOf(".", "droid", "build", "outputs", "apk")
        val debugApkSource = Paths.get(".", *outputFolder, "debug", "droid-debug.apk")
        val testApkSource =  Paths.get(".", *outputFolder, "androidTest", "debug",
                "droid-debug-androidTest.apk")
        val tmpDirectory = "/data/local/tmp"
        val debugDestination = "$tmpDirectory/com.github.cheapmon.apc.droid"
        val testDestination = "$tmpDirectory/com.github.cheapmon.apc.droid.test"
        runOnDevice("push", debugApkSource.toString(), debugDestination)
        runOnDevice("push", testApkSource.toString(), testDestination)
        runOnDevice("shell", "pm", "install", "-t", "-r", debugDestination)
        runOnDevice("shell", "pm", "install", "-t", "-r", testDestination)
        with(Logger) {
            info("Installed APK on device ${config.device}")
            line()
        }
    }

    /**
     * Remove extraction binary from Android device.
     */
    fun removeAPK() {
        runOnDevice("shell", "am", "force-stop", "com.github.cheapmon.apc.droid")
        runOnDevice("shell", "pm", "uninstall", "com.github.cheapmon.apc.droid")
        runOnDevice("shell", "pm", "uninstall", "com.github.cheapmon.apc.droid.test")
        with(Logger) {
            info("Removed all APC files from device")
            line()
        }
    }

    /**
     * Run extraction on device. Save results to a separate file per id.
     *
     * Results from device are sent via TCP on port 2000, delimited by "---", terminated by "OK".
     */
    fun runTests() {
        fun runInBackground(file: String, test: String, runner: String) {
            launch {
                val stream = runOnDevice("shell", "am", "instrument", "-w", "-r",
                        "--no-window-animation",
                        "-e", "file", file,
                        "-e", "mode", config.mode.toString(),
                        "-e", "algorithm", config.algorithm.toString(),
                        "-e", "debug", "false",
                        "-e", "class", test, runner)
                Logger.debug(stream.string())
            }
        }

        fun writeToFile(lines: ArrayList<String>) {
            val id = lines.first()
            val result = lines.subList(1, lines.size).joinToString("\n")
            val fileEnding = if (config.mode == Mode.MODEL) "xml" else "txt"
            val path = Paths.get("out", "$id.$fileEnding")
            val file = File(path.toString())
            file.parentFile.mkdirs()
            Files.createFile(path)
            Files.write(path, result.toByteArray())
            lines.clear()
            Logger.info("Output was written to $path")
        }

        fun collectResults() {
            val server = ServerSocket(2000)
            while (!server.isClosed) {
                val client = server.accept()
                val lines = ArrayList<String>(2)
                client.getInputStream().lines().forEach { line ->
                    when (line.trim()) {
                        "OK" -> {
                            client.close()
                            server.close()
                        }
                        "---" -> writeToFile(lines)
                        else -> lines.add(line)
                    }
                }
                client.close()
            }
        }

        val file = "/data/local/tmp/ids.txt"
        val pkg = "com.github.cheapmon.apc.droid"
        val test = "$pkg.DroidMain#main"
        val runner = "$pkg.test/android.support.test.runner.AndroidJUnitRunner"
        runOnDevice("push", idFile.toString(), file)
        runInBackground(file, test, runner)
        Logger.info("Running tests...")
        collectResults()
        Logger.line()
    }
}

private fun InputStream.lines(): Stream<String> {
    return BufferedReader(InputStreamReader(this)).lines()
}

private fun InputStream.string(): String {
    return this.lines().collect(Collectors.joining("\n"))
}