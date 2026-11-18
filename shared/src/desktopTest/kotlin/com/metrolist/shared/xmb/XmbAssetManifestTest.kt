package com.metrolist.shared.xmb

import java.io.File
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class XmbAssetManifestTest {
    private val repository = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "settings.gradle.kts").isFile }
    private val resources = File(repository, "shared/src/commonMain/composeResources")
    private val manifest = File(repository, "docs/xmb-assets.md")

    @Test
    fun everyManifestEntryResolvesToAValidFile() {
        val entries = manifestEntries()
        assertTrue(entries.size >= 40, "Expected the full asset manifest, parsed ${entries.size} entries")
        val problems =
            entries.mapNotNull { entry ->
                val file = File(resources, entry.path)
                when {
                    !file.isFile -> "${entry.path}: missing"
                    entry.path.startsWith("drawable/") -> pngProblem(entry, file)
                    entry.path.startsWith("font/") -> fontProblem(entry, file)
                    entry.path.startsWith("files/xmb/sounds/") -> oggProblem(entry, file)
                    entry.path.startsWith("files/xmb/gaia/") -> jpegProblem(entry, file)
                    else -> "${entry.path}: unknown asset type"
                }
            }
        if (problems.isNotEmpty()) fail(problems.joinToString("\n"))
    }

    @Test
    fun everyXmbAssetIsListed() {
        val listed = manifestEntries().map { it.path }.toSet()
        val shipped =
            buildList {
                File(resources, "drawable").listFiles { file -> file.name.startsWith("xmb_") }?.forEach { add("drawable/${it.name}") }
                File(resources, "font").listFiles { file -> file.name.startsWith("xmb_") }?.forEach { add("font/${it.name}") }
                File(
                    resources,
                    "files/xmb",
                ).walkTopDown().filter { it.isFile }.forEach { add(it.relativeTo(resources).invariantSeparatorsPath) }
            }
        assertEquals(emptyList(), shipped.filterNot { it in listed }.sorted(), "Assets missing from docs/xmb-assets.md")
    }

    @Test
    fun manifestListsEachFileOnce() {
        val duplicates = manifestEntries().groupBy { it.path }.filterValues { it.size > 1 }.keys
        assertEquals(emptySet(), duplicates)
    }

    private data class Entry(
        val path: String,
        val width: Int?,
        val height: Int?,
    )

    private fun manifestEntries(): List<Entry> =
        manifest.readLines().filter { it.startsWith("|") }.mapNotNull { row ->
            val cells = row.trim('|').split('|').map(String::trim)
            val path = cells.firstNotNullOfOrNull { pathPattern.matchEntire(it)?.groupValues?.get(1) } ?: return@mapNotNull null
            val size = cells.firstNotNullOfOrNull(sizePattern::matchEntire)
            Entry(path, size?.groupValues?.get(1)?.toInt(), size?.groupValues?.get(2)?.toInt())
        }

    private fun pngProblem(
        entry: Entry,
        file: File,
    ): String? {
        val bytes = file.readBytes()
        if (bytes.size < 24 || !bytes.copyOfRange(0, 8).contentEquals(pngSignature)) return "${entry.path}: not a PNG"
        val header = ByteBuffer.wrap(bytes, 16, 8)
        val width = header.int
        val height = header.int
        if (entry.width == null) return "${entry.path}: no size listed"
        return "${entry.path}: listed ${entry.width}×${entry.height}, file is $width×$height"
            .takeIf { width != entry.width || height != entry.height }
    }

    private fun fontProblem(
        entry: Entry,
        file: File,
    ): String? {
        val tag = file.readBytes().copyOfRange(0, 4)
        return "${entry.path}: not a TrueType or OpenType font".takeIf { fontTags.none(tag::contentEquals) }
    }

    private fun oggProblem(
        entry: Entry,
        file: File,
    ): String? = "${entry.path}: not an Ogg file".takeIf { !file.readBytes().copyOfRange(0, 4).contentEquals("OggS".toByteArray()) }

    private fun jpegProblem(
        entry: Entry,
        file: File,
    ): String? {
        val image = javax.imageio.ImageIO.read(file) ?: return "${entry.path}: not a readable image"
        if (entry.width == null) return "${entry.path}: no size listed"
        return "${entry.path}: listed ${entry.width}×${entry.height}, file is ${image.width}×${image.height}"
            .takeIf { image.width != entry.width || image.height != entry.height }
    }

    private companion object {
        val pathPattern = Regex("`((?:drawable|font|files)/[^`]+)`")
        val sizePattern = Regex("(\\d+)×(\\d+)")
        val pngSignature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val fontTags =
            listOf(
                byteArrayOf(0x00, 0x01, 0x00, 0x00),
                "OTTO".toByteArray(),
                "true".toByteArray(),
            )
    }
}
