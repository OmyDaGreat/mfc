package xyz.malefic.mfc.util.mic

/**
 * Holds the state for the audio visualizer animation.
 *
 * @property currentHeight The current height of the visualizer bar.
 * @property targetHeight The target height the visualizer bar should animate to.
 * @property percentage The current audio level as a percentage.
 * @property frameCount The number of animation frames rendered.
 */
data class AnimationState(
    val currentHeight: Int = 0,
    val targetHeight: Int = 0,
    val percentage: Int = 0,
    val frameCount: Long = 0,
)
