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

import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.Path

/**
 * Configuration of mode of extraction.
 *
 * There are two possibilities:
 * * Look for privacy policy in an app and extract its text, or
 * * Extract complete model of an app for further inspection.
 */
enum class Mode {
    /** get policy text of an app */
    POLICY,
    /** get model of an app */
    MODEL
}

/**
 * Configuration of search algorithm used in extraction.
 *
 * For text extraction, three naive solutions and one optimized approach are supplied.
 * Optimized search is set as default.
 *
 * Model extraction is strictly based on a breadth first approach.
 *
 * @see Mode
 */
enum class Algorithm {
    /** breadth first search */
    DFS,
    /** depth first search */
    BFS,
    /** randomized search */
    RS,
    /** optimized search */
    OS
}

/**
 * Configuration of extraction.
 *
 * @property mode mode of extraction
 * @property algorithm algorithm used in extraction
 * @property device label of device to run extraction on (e.g. `"emulator-5554"`)
 * @see Mode
 * @see Algorithm
 * @see deviceList
 */
data class Config(
        val mode: Mode,
        val algorithm: Algorithm,
        val device: String
)

/**
 * All valid bundle ids are stored in this file and then sent to the device
 */
val idFile: Path by lazy {
    Paths.get(".", "ids.txt")
}

/**
 * Parse command line arguments to configuration.
 *
 * @param args arguments
 * @return resulting configuration
 * @see Config
 */
fun parse(args: Array<String>): Config {
    with(DefaultParser().parse(options, args, true)) {
        if (hasOption("help")) printUsage()
        val num = checkIdsAndSaveToFile(getOptionValues("ids"), getOptionValue("file"))
        val mode = if (hasOption("model")) Mode.MODEL else Mode.POLICY
        val algorithm = setAlgorithm(getOptionValue("algorithm"))
        val device = setDevice(getOptionValue("device"))
        with(Logger) {
            line()
            info("Found $num application ids")
            info("Extraction mode is $mode")
            info("Using device $device")
            info("Using $algorithm")
            line()
        }
        return Config(mode, algorithm, device)
    }
}

/**
 * Command line options.
 *
 * * Display help message
 * * Bundle ids of apps to search
 * * Device to run on
 * * Search algorithm to use
 * * Mode of extraction
 *
 * @see printUsage
 * @see Algorithm
 * @see Mode
 */
private val options = Options().apply {
    addOption("h", "help", false, "this help message")
    addOption(Option.builder("i").longOpt("ids").hasArgs().desc("app ids").build())
    addOption("f", "file", true, "file containing app ids")
    addOption("d", "device", true, "device to run extraction on")
    addOption("a", "algorithm", true, "search algorithm")
    addOption("m", "model", false, "extract model of app")
}

/**
 * Merge bundle ids from command line and file, save valid ids to an additional file.
 *
 * @param ids bundle ids (e.g. `["com.facebook.orca", "com.booking"]`)
 * @param path path to file containing bundle ids (e.g. `"./my_id_file.txt"`)
 * @return valid ids and path to new file
 * @see idFile
 */
private fun checkIdsAndSaveToFile(ids: Array<String>?, path: String?): Int {

    /**
     * Convert bundle ids from command line.
     *
     * @param ids array of bundle ids
     * @return list of bundle ids
     */
    fun getIDsFromArguments(ids: Array<String>?): List<String> {
        return ids?.toList() ?: emptyList()
    }

    /**
     * Read bundle ids from input file.
     *
     * @param path path to file
     * @return list of bundle ids
     */
    fun getIDsFromFile(path: String?): List<String> {
        if (path == null) return emptyList()
        return try {
            Files.readAllLines(Paths.get(path)).orEmpty()
        } catch (ex: IOException) {
            emptyList()
        }
    }

    /**
     * Bundle ids are valid if their corresponding apps are available on Google Play.
     *
     * Specifically, the bundle id `"com.booking"` is valid if
     * ```
     * GET https://play.google.com/store/apps/details?id=com.booking
     * ```
     * has response code 200.
     *
     * @param id bundle id
     * @return whether the id is valid
     */
    fun validID(id: String): Boolean {
        val agent = "Mozilla/5.0"
        val url = URL("https://play.google.com/store/apps/details?id=$id")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", agent)
        return connection.responseCode == 200
    }

    val validIDs = getIDsFromFile(path).union(getIDsFromArguments(ids)).filter(::validID)
    if (validIDs.isEmpty()) printUsage("ID list empty or invalid")
    Files.write(idFile, validIDs.joinToString("\n").toByteArray())
    return validIDs.size
}

/**
 * Set algorithm used in extraction.
 *
 * @param algorithm algorithm label from command line
 * @return which algorithm will be used
 * @see Algorithm
 */
private fun setAlgorithm(algorithm: String?): Algorithm {
    return try {
        Algorithm.valueOf(algorithm?.toUpperCase() ?: "OS")
    } catch (ex: IllegalArgumentException) {
        Logger.info("Algorithm label $algorithm incorrect")
        Algorithm.OS
    }
}

/**
 * Set device to run extraction on. (default: first attached device)
 *
 * @param device device label from command line
 * @return which device will be used
 * @see deviceList
 */
private fun setDevice(device: String?): String {
    if (deviceList.isEmpty()) printUsage("No Android device attached")
    return when (device) {
        null -> deviceList.first()
        else -> deviceList.find { it == device.trim() } ?: deviceList.first()
    }
}

/**
 * Print usage and options of command line interface.
 *
 * If message is "My message", resulting output is
 * ```
 * My message
 * usage: java -jar ./build/libs/apc-kotlin-0.0.jar [-h] [-i <arg>] [-f <arg>] [-d <arg>] [-a <arg>] [-m]
 * -h,--help              this help message
 * -i,--ids <arg>         app ids
 * -f,--file <arg>        file containing app ids
 * -d,--device <arg>      device to run extraction on
 * -a,--algorithm <arg>   search algorithm
 * -m,--model             extract model of app
 * ```
 *
 * @see options
 */
private fun printUsage(message: String = "") {
    if (!message.isEmpty()) {
        println(message)
    }
    with(HelpFormatter()) {
        optionComparator = null
        width = 120
        printHelp("java -jar ./build/libs/apc-kotlin-0.0.jar", options, true)
    }
    System.exit(1)
}