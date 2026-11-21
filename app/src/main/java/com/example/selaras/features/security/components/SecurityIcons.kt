package com.example.selaras.features.security.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Self-contained Material Symbols Rounded vector icons for the security module.
 * Eliminates dependency on material-icons-extended.
 */
object SecurityIcons {

    val Lock: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18f, 8f)
                horizontalLineTo(17f)
                verticalLineTo(6f)
                curveTo(17f, 3.24f, 14.76f, 1f, 12f, 1f)
                reflectiveCurveTo(7f, 3.24f, 7f, 6f)
                verticalLineTo(8f)
                horizontalLineTo(6f)
                curveTo(4.9f, 8f, 4f, 8.9f, 4f, 10f)
                verticalLineTo(20f)
                curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
                horizontalLineTo(18f)
                curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
                verticalLineTo(10f)
                curveTo(20f, 8.9f, 19.1f, 8f, 18f, 8f)
                close()
                moveTo(9f, 6f)
                curveTo(9f, 4.34f, 10.34f, 3f, 12f, 3f)
                reflectiveCurveTo(15f, 4.34f, 15f, 6f)
                verticalLineTo(8f)
                horizontalLineTo(9f)
                verticalLineTo(6f)
                close()
                moveTo(12f, 17f)
                curveTo(10.9f, 17f, 10f, 16.1f, 10f, 15f)
                reflectiveCurveTo(10.9f, 13f, 12f, 13f)
                reflectiveCurveTo(14f, 13.9f, 14f, 15f)
                reflectiveCurveTo(13.1f, 17f, 12f, 17f)
                close()
            }
        }.build()
    }

    val Key: ImageVector by lazy {
        ImageVector.Builder(
            name = "Key",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(21f, 10f)
                horizontalLineTo(12.98f)
                curveTo(12.48f, 7.15f, 9.99f, 5f, 7f, 5f)
                curveTo(3.69f, 5f, 1f, 7.69f, 1f, 11f)
                curveTo(1f, 14.31f, 3.69f, 17f, 7f, 17f)
                curveTo(9.99f, 17f, 12.48f, 14.85f, 12.98f, 12f)
                horizontalLineTo(15f)
                verticalLineTo(15f)
                horizontalLineTo(18f)
                verticalLineTo(12f)
                horizontalLineTo(21f)
                curveTo(21.55f, 12f, 22f, 11.55f, 22f, 11f)
                reflectiveCurveTo(21.55f, 10f, 21f, 10f)
                close()
                moveTo(7f, 13f)
                curveTo(5.9f, 13f, 5f, 12.1f, 5f, 11f)
                reflectiveCurveTo(5.9f, 9f, 7f, 9f)
                reflectiveCurveTo(9f, 9.9f, 9f, 11f)
                reflectiveCurveTo(8.1f, 13f, 7f, 13f)
                close()
            }
        }.build()
    }

    val Backspace: ImageVector by lazy {
        ImageVector.Builder(
            name = "Backspace",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(22f, 3f)
                horizontalLineTo(7f)
                curveTo(6.31f, 3f, 5.67f, 3.36f, 5.3f, 3.94f)
                lineTo(0.55f, 11.44f)
                curveTo(0.24f, 11.93f, 0.24f, 12.57f, 0.55f, 13.06f)
                lineTo(5.3f, 20.56f)
                curveTo(5.67f, 21.14f, 6.31f, 21.5f, 7f, 21.5f)
                horizontalLineTo(22f)
                curveTo(23.1f, 21.5f, 24f, 20.6f, 24f, 19.5f)
                verticalLineTo(5f)
                curveTo(24f, 3.9f, 23.1f, 3f, 22f, 3f)
                close()
                moveTo(19f, 15.59f)
                lineTo(17.59f, 17f)
                lineTo(14f, 13.41f)
                lineTo(10.41f, 17f)
                lineTo(9f, 15.59f)
                lineTo(12.59f, 12f)
                lineTo(9f, 8.41f)
                lineTo(10.41f, 7f)
                lineTo(14f, 10.59f)
                lineTo(17.59f, 7f)
                lineTo(19f, 8.41f)
                lineTo(15.41f, 12f)
                lineTo(19f, 15.59f)
                close()
            }
        }.build()
    }

    val ContentCopy: ImageVector by lazy {
        ImageVector.Builder(
            name = "ContentCopy",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(16f, 1f)
                horizontalLineTo(4f)
                curveTo(2.9f, 1f, 2f, 1.9f, 2f, 3f)
                verticalLineTo(17f)
                horizontalLineTo(4f)
                verticalLineTo(3f)
                horizontalLineTo(16f)
                verticalLineTo(1f)
                close()
                moveTo(19f, 5f)
                horizontalLineTo(8f)
                curveTo(6.9f, 5f, 6f, 5.9f, 6f, 7f)
                verticalLineTo(21f)
                curveTo(6f, 22.1f, 6.9f, 23f, 8f, 23f)
                horizontalLineTo(19f)
                curveTo(20.1f, 23f, 21f, 22.1f, 21f, 21f)
                verticalLineTo(7f)
                curveTo(21f, 5.9f, 20.1f, 5f, 19f, 5f)
                close()
                moveTo(19f, 21f)
                horizontalLineTo(8f)
                verticalLineTo(7f)
                horizontalLineTo(19f)
                verticalLineTo(21f)
                close()
            }
        }.build()
    }

    val CheckCircle: ImageVector by lazy {
        ImageVector.Builder(
            name = "CheckCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveTo(6.48f, 22f, 12f, 22f)
                reflectiveCurveTo(22f, 17.52f, 22f, 12f)
                reflectiveCurveTo(17.52f, 2f, 12f, 2f)
                close()
                moveTo(10f, 17f)
                lineTo(5f, 12f)
                lineTo(6.41f, 10.59f)
                lineTo(10f, 14.17f)
                lineTo(17.59f, 6.58f)
                lineTo(19f, 8f)
                lineTo(10f, 17f)
                close()
            }
        }.build()
    }

    val Shield: ImageVector by lazy {
        ImageVector.Builder(
            name = "Shield",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 1f)
                lineTo(3f, 5f)
                verticalLineTo(11f)
                curveTo(3f, 16.55f, 6.84f, 21.74f, 12f, 23f)
                curveTo(17.16f, 21.74f, 21f, 16.55f, 21f, 11f)
                verticalLineTo(5f)
                lineTo(12f, 1f)
                close()
                moveTo(12f, 11.99f)
                horizontalLineTo(19f)
                curveTo(18.47f, 16.11f, 15.72f, 19.78f, 12f, 20.93f)
                verticalLineTo(12f)
                horizontalLineTo(5f)
                verticalLineTo(6.3f)
                lineTo(12f, 3.19f)
                verticalLineTo(11.99f)
                close()
            }
        }.build()
    }
}
