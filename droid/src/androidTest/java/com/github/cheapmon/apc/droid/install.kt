package com.github.cheapmon.apc.droid

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.test.InstrumentationRegistry
import android.support.test.uiautomator.By
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiObject2
import android.support.test.uiautomator.Until

typealias Element = UiObject2

/**
 * Install an app from Google Play.
 *
 * This approach is tailored to Google Plays layout and may not work with later versions.
 * Includes skipping wifi messages and permissions window.
 *
 * @param id bundle id of app
 * @return Installation succeeded
 */
fun install(id: String): Boolean {
    if (installed(id)) {
        return true
    }
    with(GPlay) {
        start(id)
        if (!has("buttonContainer") && has("warningMessage")) return false
        click("buttonContainer", "button")
        if (has("wifiMessage")) {
            click("buttonPanel", "firstButton")
        }
        if (has("buttonPanel")) {
            click("buttonPanel", "button")
            click("skipButton")
        }
        if (has("appPermissions")) {
            click("continueBar", "continueButton")
        }
        waitUntilGone("downloadPanel")
        waitUntilGone("installMessage")
        if (has("message")) {
            click("buttonPanel", "firstButton")
            return false
        }
        waitUntilHas("buttonContainer")
    }
    return true
}

/**
 * Remove an app from device.
 *
 * @param id bundle id of app
 * @return shell uninstall message
 */
fun remove(id: String): String {
    return GPlay.device.executeShellCommand("pm uninstall $id")
}

/*
 * Credit: [Jonik](https://stackoverflow.com/a/28175210/6743101)
 */
private fun installed(id: String): Boolean {
    return try {
        InstrumentationRegistry.getContext().packageManager.getApplicationInfo(id, 0)
        true
    } catch (ex: PackageManager.NameNotFoundException) {
        false
    }
}

private object GPlay {

    const val TIMEOUT: Long = 1000

    val device: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    private val containers by lazy {
        val pkg = "com.android.vending"
        hashMapOf(
                "market" to By.pkg(pkg),
                "warningMessage" to By.res("$pkg:id/warning_message_module"),
                "wifiMessage" to By.res("$pkg:id/wifi_message"),
                "installMessage" to By.res("$pkg:id/summary_dynamic_status")
                        .clazz("android.widget.TextView"),
                "message" to By.res("android:id/message"),
                "buttonContainer" to By.res("$pkg:id/button_container"),
                "buttonPanel" to By.res("$pkg:id/buttonPanel"),
                "downloadPanel" to By.res("$pkg:id/download_progress_panel"),
                "appPermissions" to By.res("$pkg:id/app_permissions"),
                "continueBar" to By.res("$pkg:id/continue_button_bar"),
                "continueButton" to By.res("$pkg:id/continue_button"),
                "skipButton" to By.res("$pkg:id/not_now_button"),
                "button" to By.clazz("android.widget.Button"),
                "firstButton" to By.res("android:id/button1")
        )
    }

    /**
     * Start Google Play at page for specific app.
     *
     * @param id bundle id of app
     */
    fun start(id: String) {
        val uri = Uri.parse("market://details?id=$id")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        InstrumentationRegistry.getContext().startActivity(intent)
        waitUntilHas("market", TIMEOUT * 5)
        waitForChange()
    }

    /**
     * Click specific element on Google Play layout.
     *
     * @param sequence elements of ui in sequence, with every n-th container containing the
     * (n+1)-th container in the sequence and only the last element being clicked
     */
    fun click(vararg sequence: String) {
        waitUntilHas(sequence[0])
        var element: Element? = device.findObject(containers[sequence[0]])
        for (i in 1 until sequence.size) {
            element = element?.findObject(containers[sequence[i]])
        }
        element?.click()
        waitForChange()
        waitUntilGone(sequence[0])
    }

    private fun waitForChange() {
        device.waitForWindowUpdate("com.android.vending", TIMEOUT)
    }

    fun waitUntilHas(container: String, timeout: Long = TIMEOUT) {
        device.wait(Until.hasObject(containers[container]), timeout)
    }

    fun waitUntilGone(container: String) {
        while (has(container)) {
            device.wait(Until.gone(containers[container]), TIMEOUT)
        }
    }

    fun has(container: String): Boolean = device.hasObject(containers[container])

}
