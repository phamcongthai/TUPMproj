package com.example.foodapp.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.platform.app.InstrumentationRegistry

/** Logged-out startup. ReportDrawn in LoginScreen supplies TTFD on supported Android versions. */
@LargeTest
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartupNoCompilation() = measureStartup(StartupMode.COLD, CompilationMode.None())

    @Test
    fun coldStartupWithBaselineProfile() = measureStartup(
        StartupMode.COLD,
        CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require, warmupIterations = 0)
    )

    @Test
    fun warmStartupWithBaselineProfile() = measureStartup(
        StartupMode.WARM,
        CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require, warmupIterations = 0)
    )

    private fun measureStartup(mode: StartupMode, compilation: CompilationMode) = benchmarkRule.measureRepeated(
        packageName = "com.example.jetpackcompose.benchmark",
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilation,
        startupMode = mode,
        iterations = InstrumentationRegistry.getArguments().getString("iterations")?.toInt() ?: 10,
        setupBlock = { pressHome() }
    ) {
        startActivityAndWait()
        assertTrue(
            "Expected logged-out login screen. Clear only the benchmark app data before retrying.",
            device.wait(Until.hasObject(By.text("Đăng nhập để tiếp tục")), 10_000)
        )
    }
}
