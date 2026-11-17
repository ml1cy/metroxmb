package com.metrolist.shared.xmb.sound

import androidx.compose.runtime.staticCompositionLocalOf
import com.metrolist.shared.xmb.nav.XmbFeedback

/** The XMB's sound effects and the committed file each one plays (docs/xmb-assets.md). */
enum class XmbSound(
    val file: String,
) {
    NAV_UP("nav_up.ogg"),
    NAV_DOWN("nav_down.ogg"),

    /** No left/right sound in the pack yet; reuses the down sound. */
    NAV_SIDE("nav_down.ogg"),
    SELECT("select.ogg"),
    BACK("back.ogg"),

    /** No error sound in the pack yet; reuses the back sound. */
    ERROR("back.ogg"),
    NOTICE("notice.ogg"),
    NOTICE_CLOSE("notice_close.ogg"),
    BOOT("boot.ogg"),
    UNLOCK("unlock.ogg"),
    MENU_BGM("menu_bgm.ogg"),
    ;

    /** Path for `Res.readBytes`. */
    val resourcePath: String get() = "files/xmb/sounds/$file"

    companion object {
        /** Short effects a player should preload; the boot jingle and menu music stream on demand. */
        val effects: List<XmbSound> = entries.filter { it != BOOT && it != MENU_BGM }
    }
}

/** Plays XMB sound effects. Platforms provide one through [LocalXmbSoundPlayer]. */
fun interface XmbSoundPlayer {
    fun play(sound: XmbSound)

    /** Menu sound volume, 0 to 1 (Sound Settings). */
    fun setVolume(volume: Float) = Unit
}

val SilentXmbSoundPlayer = XmbSoundPlayer { }

val LocalXmbSoundPlayer = staticCompositionLocalOf { SilentXmbSoundPlayer }

fun XmbFeedback.sound(): XmbSound? =
    when (this) {
        XmbFeedback.NONE -> null
        XmbFeedback.MOVE_UP -> XmbSound.NAV_UP
        XmbFeedback.MOVE_DOWN -> XmbSound.NAV_DOWN
        XmbFeedback.MOVE_SIDE -> XmbSound.NAV_SIDE
        XmbFeedback.SELECT -> XmbSound.SELECT
        XmbFeedback.BACK -> XmbSound.BACK
        XmbFeedback.ERROR -> XmbSound.ERROR
    }
