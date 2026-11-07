package com.example.foodapp.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Run with optimizeBuild=false: capture original symbols, then let R8 rewrite them. */
@LargeTest
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val profileRule = BaselineProfileRule()

    @Test
    fun loggedOutStartup() = profileRule.collect(
        packageName = "com.example.jetpackcompose.benchmark",
        includeInStartupProfile = false,
        maxIterations = 15,
        stableIterations = 3
    ) {
        pressHome()
        startActivityAndWait()
        assertTrue(device.wait(Until.hasObject(By.text("Đăng nhập để tiếp tục")), 10_000))
    }

    /** DEX layout targets initial display; the baseline journey also covers Login/TTFD. */
    @Test
    fun initialDisplayStartup() = profileRule.collect(
        packageName = "com.example.jetpackcompose.benchmark",
        includeInStartupProfile = true,
        maxIterations = 15,
        stableIterations = 3
    ) {
        pressHome()
        startActivityAndWait()
    }
}
