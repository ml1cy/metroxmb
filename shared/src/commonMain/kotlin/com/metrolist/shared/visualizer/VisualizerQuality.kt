package com.metrolist.shared.visualizer

/**
 * How much detail the visualizers draw (Music Player Settings): fewer terrain layers and noise
 * octaves, and fewer ribbon passes, for phones and older graphics.
 */
enum class VisualizerQuality(
    val id: String,
    val valleyLayers: Int,
    val valleyOctaves: Int,
    val gaiaOctaves: Int,
    val ribbonPasses: Int,
) {
    HIGH("high", valleyLayers = 28, valleyOctaves = 4, gaiaOctaves = 5, ribbonPasses = 8),
    BALANCED("balanced", valleyLayers = 18, valleyOctaves = 3, gaiaOctaves = 4, ribbonPasses = 4),
    LOW("low", valleyLayers = 10, valleyOctaves = 2, gaiaOctaves = 3, ribbonPasses = 1),
    ;

    companion object {
        fun of(id: String): VisualizerQuality = entries.firstOrNull { it.id == id } ?: HIGH
    }
}
