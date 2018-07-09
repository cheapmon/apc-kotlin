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

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
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
    run("adb", "devices").stream().skip(1)
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
 */
private fun run(vararg commands: String): InputStream {
    Logger.debug(commands.joinToString(" "))
    with(ProcessBuilder(*commands).start()) {
        val exitCode = waitFor()
        if (exitCode == 0) {
            return inputStream
        } else {
            throw Exception(errorStream.string())
        }
    }
}

private fun InputStream.stream(): Stream<String> {
    return BufferedReader(InputStreamReader(this)).lines()
}

private fun InputStream.string(): String {
    return this.stream().collect(Collectors.joining("\n"))
}