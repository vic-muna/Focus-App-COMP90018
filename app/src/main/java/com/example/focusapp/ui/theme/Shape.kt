package com.example.focusapp.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/** Side length of the Figma blob's viewBox, which [BLOB_PATH] is drawn in. */
private const val BLOB_VIEWPORT = 135.554f

/** Figma: "Focuse Buttom" outline - a rounded octagon with soft, uneven sides. */
private const val BLOB_PATH =
    "M51.5679 3.63038C61.8318 -1.21013 73.7219 -1.21012 83.9858 3.63038L104.22 13.173" +
        "C112.198 16.9354 118.618 23.3559 122.381 31.3337L131.923 51.5679" +
        "C136.764 61.8318 136.764 73.7219 131.923 83.9858L122.381 104.22" +
        "C118.618 112.198 112.198 118.618 104.22 122.381L83.9858 131.923" +
        "C73.7219 136.764 61.8318 136.764 51.5679 131.923L31.3337 122.381" +
        "C23.3558 118.618 16.9354 112.198 13.173 104.22L3.63038 83.9858" +
        "C-1.21013 73.7219 -1.21012 61.8318 3.63038 51.5679L13.173 31.3337" +
        "C16.9354 23.3558 23.3559 16.9354 31.3337 13.173L51.5679 3.63038Z"

/** The Figma blob shape, scaled to whatever size it's drawn at. Use like any Shape (`background`, `clip`...). */
object FocusBlobShape : Shape {
    private val basePath = PathParser().parsePathString(BLOB_PATH).toPath()

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            addPath(basePath)
            transform(Matrix().apply { scale(size.width / BLOB_VIEWPORT, size.height / BLOB_VIEWPORT) })
        }
        return Outline.Generic(path)
    }
}
