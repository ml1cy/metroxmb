package com.metrolist.desktop.xmb

import com.metrolist.desktop.utils.AppLogger
import com.metrolist.shared.utils.OsUtil
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions

/**
 * Keeps the XMB smooth while its window is behind others on Windows. Windows 11 power-throttles
 * processes without the foreground window (EcoQoS: slower CPU scheduling, coarse timers), which
 * made the animation, and possibly playback, stutter when tabbed out. This opts the process out,
 * as media players do, and asks for 1 ms timers. A window without focus also loses the foreground
 * priority boost, so the render thread waits behind other programs and heavy views (Gaia) drop
 * frames; the process runs at above-normal priority instead, like media players. All of this is
 * best effort: older Windows versions don't know the throttling setting, and nothing changes there.
 */
internal object DesktopXmbPower {
    private var applied = false

    fun keepSmoothInBackground() {
        if (applied || !OsUtil.isWindows) return
        applied = true
        runCatching {
            val state =
                Memory(STATE_SIZE).apply {
                    setInt(0, POWER_THROTTLING_CURRENT_VERSION)
                    setInt(4, POWER_THROTTLING_EXECUTION_SPEED or POWER_THROTTLING_IGNORE_TIMER_RESOLUTION)
                    // A cleared bit under a set control bit means "off": no throttling of either.
                    setInt(8, 0)
                }
            val kernel = Kernel32Power.INSTANCE
            val done = kernel.SetProcessInformation(kernel.GetCurrentProcess(), PROCESS_POWER_THROTTLING, state, STATE_SIZE.toInt())
            AppLogger.info(TAG, "Background power throttling ${if (done) "off" else "unchanged (not supported)"}")
        }.onFailure { AppLogger.warning(TAG, "Could not turn off background power throttling", it) }
        runCatching { WinMm.INSTANCE.timeBeginPeriod(1) }
            .onFailure { AppLogger.warning(TAG, "Could not request 1 ms timers", it) }
        runCatching {
            val kernel = Kernel32Power.INSTANCE
            val done = kernel.SetPriorityClass(kernel.GetCurrentProcess(), ABOVE_NORMAL_PRIORITY_CLASS)
            AppLogger.info(TAG, "Process priority ${if (done) "above normal" else "unchanged"}")
        }.onFailure { AppLogger.warning(TAG, "Could not raise the process priority", it) }
    }

    @Suppress("FunctionName")
    private interface Kernel32Power : StdCallLibrary {
        fun GetCurrentProcess(): WinNT.HANDLE

        fun SetProcessInformation(
            process: WinNT.HANDLE,
            informationClass: Int,
            information: Pointer,
            size: Int,
        ): Boolean

        fun SetPriorityClass(
            process: WinNT.HANDLE,
            priorityClass: Int,
        ): Boolean

        companion object {
            val INSTANCE: Kernel32Power = Native.load("kernel32", Kernel32Power::class.java, W32APIOptions.DEFAULT_OPTIONS)
        }
    }

    @Suppress("FunctionName")
    private interface WinMm : StdCallLibrary {
        fun timeBeginPeriod(period: Int): Int

        companion object {
            val INSTANCE: WinMm = Native.load("winmm", WinMm::class.java)
        }
    }

    private const val TAG = "XmbPower"

    // PROCESS_INFORMATION_CLASS.ProcessPowerThrottling and PROCESS_POWER_THROTTLING_STATE.
    private const val PROCESS_POWER_THROTTLING = 4
    private const val POWER_THROTTLING_CURRENT_VERSION = 1
    private const val POWER_THROTTLING_EXECUTION_SPEED = 0x1
    private const val POWER_THROTTLING_IGNORE_TIMER_RESOLUTION = 0x4
    private const val STATE_SIZE = 12L
    private const val ABOVE_NORMAL_PRIORITY_CLASS = 0x8000
}
