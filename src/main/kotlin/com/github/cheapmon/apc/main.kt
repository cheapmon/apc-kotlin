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

import mu.KotlinLogging

/**
 * Parse command line arguments and run extraction on Android device.
 *
 * @param args command line arguments
 */
fun main(args: Array<String>) {
    parse(args)
}

/**
 * Logger for this project.
 *
 * Instead of typing
 * ```
 * KotlinLogging.logger("APC").debug { "my debug message" }
 * ```
 * every time, we just use
 * ```
 * Logger.debug("my debug message")
 * ```
 */
object Logger {
    private val logger = KotlinLogging.logger("APC")

    /** Info messages are written to console and log file. */
    fun info(msg: Any?) = logger.info { "| $msg" }

    /** Debug messages are written only to log file. */
    fun debug(msg: Any?) = logger.debug { msg }

    /** Visual delimiter between log messages */
    fun line() = logger.info { "=".repeat(40) }
}