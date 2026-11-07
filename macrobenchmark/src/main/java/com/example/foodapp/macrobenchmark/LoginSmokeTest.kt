package com.example.foodapp.macrobenchmark

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Check navigation and deferred credentials on the minified app without submitting an account. */
@RunWith(AndroidJUnit4::class)
class LoginSmokeTest {
    @Test
    fun loginAndRegisterNavigation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        // Launch with shell privileges, as Macrobenchmark does, so an OEM's
        // background activity restriction on the runner cannot hide the app.
        device.executeShellCommand(
            "am start -W -f 0x10008000 -n " +
                "com.example.jetpackcompose.benchmark/com.example.jetpackcompose.MainActivity"
        )
        assertTrue(device.wait(Until.hasObject(By.text("Đăng nhập để tiếp tục")), 10_000))
        device.waitForIdle()
        val registerButton = device.findObject(By.clickable(true).hasDescendant(By.text("Đăng ký")))
        checkNotNull(registerButton) { "Register button not found" }
        registerButton.click()
        assertTrue(device.wait(Until.hasObject(By.text("Tạo tài khoản mới")), 20_000))
        device.pressBack()
        assertTrue(device.wait(Until.hasObject(By.text("Đăng nhập để tiếp tục")), 10_000))
    }
}
