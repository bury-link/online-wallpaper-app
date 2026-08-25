package link.bury.onlinewallpaper.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameFitTest {

    @Test
    fun fill_position_movesTheCropToTheRequestedEdge() {
        val frame = calculateFrame(
            sourceWidth = 3000,
            sourceHeight = 1000,
            targetWidth = 1000,
            targetHeight = 2000,
            fit = Framing.FILL,
            horizontalPosition = Framing.POSITION_END,
            verticalPosition = Framing.POSITION_START,
        )

        // Dragging right means the visible image moves right, so its source crop moves left.
        assertEquals(CropRect(0, 0, 500, 1000), frame.crop)
    }

    @Test
    fun fill_position_movesTheCropToTheBottom() {
        val frame = calculateFrame(
            sourceWidth = 1000,
            sourceHeight = 3000,
            targetWidth = 2000,
            targetHeight = 1000,
            fit = Framing.FILL,
            horizontalPosition = Framing.POSITION_START,
            verticalPosition = Framing.POSITION_END,
        )

        // Dragging down means the visible image moves down, so its source crop moves upward.
        assertEquals(CropRect(0, 0, 1000, 500), frame.crop)
    }

    @Test
    fun fit_position_placesLetterboxedImageAtTheBottom() {
        val frame = calculateFrame(
            sourceWidth = 3000,
            sourceHeight = 1000,
            targetWidth = 1000,
            targetHeight = 2000,
            fit = Framing.FIT,
            verticalPosition = Framing.POSITION_END,
        )

        assertEquals(1_667, frame.drawTop)
    }

    @Test
    fun previewFrame_usesTheDeviceAspectRatioWithinTheAvailableWidth() {
        val frame = calculatePreviewFrame(
            screenWidth = 1080,
            screenHeight = 2400,
            availableWidth = 324,
        )

        assertEquals(324, frame.width)
        assertEquals(720, frame.height)
    }

    @Test
    fun fill_endpoint_matchesCenterCropAndCoversTargetExactly() {
        val frame = calculateFrame(
            sourceWidth = 3000,
            sourceHeight = 1000,
            targetWidth = 1000,
            targetHeight = 2000,
            fit = Framing.FILL,
        )

        // Same crop a plain center-crop would pick...
        assertEquals(
            calculateCenterCrop(3000, 1000, 1000, 2000),
            frame.crop,
        )
        // ...scaled to cover the target exactly, so no borders are visible.
        assertEquals(1000, frame.drawWidth)
        assertEquals(2000, frame.drawHeight)
    }

    @Test
    fun fit_endpoint_usesEntireSourceAndLeavesNoCropping() {
        val frame = calculateFrame(
            sourceWidth = 3000,
            sourceHeight = 1000,
            targetWidth = 1000,
            targetHeight = 2000,
            fit = Framing.FIT,
        )

        // The whole source image is used, nothing is cropped away.
        assertEquals(CropRect(0, 0, 3000, 1000), frame.crop)
        // Scaled down uniformly to fit inside the target: width is the limiting dimension
        // (3000 wide source into a 1000-wide target beats 1000 tall source into 2000-tall target),
        // so the drawn image is narrower than the target on neither axis and shorter on the other.
        assertEquals(1000, frame.drawWidth)
        assertEquals(333, frame.drawHeight)
    }

    @Test
    fun fit_endpoint_withMatchingAspectRatio_fillsTargetWithNoBorders() {
        val frame = calculateFrame(
            sourceWidth = 2160,
            sourceHeight = 3840,
            targetWidth = 1080,
            targetHeight = 1920,
            fit = Framing.FIT,
        )

        assertEquals(CropRect(0, 0, 2160, 3840), frame.crop)
        assertEquals(1080, frame.drawWidth)
        assertEquals(1920, frame.drawHeight)
    }

    @Test
    fun outOfRangeFit_isClampedToTheNearestEndpoint() {
        val belowFit = calculateFrame(3000, 1000, 1000, 2000, fit = -5f)
        val aboveFill = calculateFrame(3000, 1000, 1000, 2000, fit = 5f)

        assertEquals(calculateFrame(3000, 1000, 1000, 2000, fit = Framing.FIT), belowFit)
        assertEquals(calculateFrame(3000, 1000, 1000, 2000, fit = Framing.FILL), aboveFill)
    }

    @Test
    fun midSlider_cropsMoreThanFitButLessThanFill() {
        val fit = calculateFrame(3000, 1000, 1000, 2000, fit = Framing.FIT)
        val mid = calculateFrame(3000, 1000, 1000, 2000, fit = 0.5f)
        val fill = calculateFrame(3000, 1000, 1000, 2000, fit = Framing.FILL)

        // Progressively less of the source survives as the slider moves from FIT to FILL...
        assertTrue(mid.crop.width < fit.crop.width)
        assertTrue(mid.crop.width > fill.crop.width)
        // ...and the drawn image progressively grows to cover more of the target's height (the
        // axis that is letterboxed here), shrinking the border on that axis.
        assertTrue(mid.drawHeight > fit.drawHeight)
        assertTrue(mid.drawHeight < fill.drawHeight)
    }

    @Test
    fun fit_endpoint_upscalesASmallerSourceAndPillarboxesTheMismatchedAxis() {
        val frame = calculateFrame(
            sourceWidth = 500,
            sourceHeight = 1000,
            targetWidth = 1200,
            targetHeight = 2000,
            fit = Framing.FIT,
        )

        assertEquals(CropRect(0, 0, 500, 1000), frame.crop)
        // Height is the limiting axis (2x fits exactly; width would need 2.4x), so the image is
        // upscaled 2x and fills the target's height completely, leaving borders left/right.
        assertEquals(1000, frame.drawWidth)
        assertEquals(2000, frame.drawHeight)
    }

    @Test
    fun nonPositiveDimensions_returnEmptyFrameInsteadOfCrashing() {
        val frame = calculateFrame(
            sourceWidth = 0,
            sourceHeight = 100,
            targetWidth = 1000,
            targetHeight = 2000,
            fit = Framing.FILL,
        )

        assertEquals(CropRect(0, 0, 0, 100), frame.crop)
        assertEquals(0, frame.drawWidth)
        assertEquals(100, frame.drawHeight)
    }
}
