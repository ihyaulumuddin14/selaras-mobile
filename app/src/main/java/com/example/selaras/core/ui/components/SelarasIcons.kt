package com.example.selaras.core.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Self-contained Material Symbols Rounded vector icons for Selaras.
 * Eliminates dependencies on material-icons-extended and ensures 100% build reliability.
 */
object SelarasIcons {

    val Home: ImageVector by lazy {
        ImageVector.Builder(
            name = "Home",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(10f, 20f)
                verticalLineTo(14f)
                horizontalLineTo(14f)
                verticalLineTo(20f)
                curveTo(14f, 20.55f, 14.45f, 21f, 15f, 21f)
                horizontalLineTo(18f)
                curveTo(18.55f, 21f, 19f, 20.55f, 19f, 20f)
                verticalLineTo(12f)
                horizontalLineTo(20.7f)
                curveTo(21.16f, 12f, 21.38f, 11.43f, 21.03f, 11.13f)
                lineTo(12.69f, 3.65f)
                curveTo(12.3f, 3.3f, 11.7f, 3.3f, 11.31f, 3.65f)
                lineTo(2.97f, 11.13f)
                curveTo(2.62f, 11.43f, 2.84f, 12f, 3.3f, 12f)
                horizontalLineTo(5f)
                verticalLineTo(20f)
                curveTo(5f, 20.55f, 5.45f, 21f, 6f, 21f)
                horizontalLineTo(9f)
                curveTo(9.55f, 21f, 10f, 20.55f, 10f, 20f)
                close()
            }
        }.build()
    }

    val EditNote: ImageVector by lazy {
        ImageVector.Builder(
            name = "EditNote",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 10f)
                horizontalLineTo(14f)
                curveTo(14.55f, 10f, 15f, 9.55f, 15f, 9f)
                reflectiveCurveTo(14.55f, 8f, 14f, 8f)
                horizontalLineTo(3f)
                curveTo(2.45f, 8f, 2f, 8.45f, 2f, 9f)
                reflectiveCurveTo(2.45f, 10f, 3f, 10f)
                close()
                moveTo(3f, 14f)
                horizontalLineTo(10f)
                curveTo(10.55f, 14f, 11f, 13.55f, 11f, 13f)
                reflectiveCurveTo(10.55f, 12f, 10f, 12f)
                horizontalLineTo(3f)
                curveTo(2.45f, 12f, 2f, 12.45f, 2f, 13f)
                reflectiveCurveTo(2.45f, 14f, 3f, 14f)
                close()
                moveTo(3f, 6f)
                horizontalLineTo(14f)
                curveTo(14.55f, 6f, 15f, 5.55f, 15f, 5f)
                reflectiveCurveTo(14.55f, 4f, 14f, 4f)
                horizontalLineTo(3f)
                curveTo(2.45f, 4f, 2f, 4.45f, 2f, 5f)
                reflectiveCurveTo(2.45f, 6f, 3f, 6f)
                close()
                moveTo(18.01f, 12.87f)
                lineTo(19.42f, 11.46f)
                curveTo(19.81f, 11.07f, 20.44f, 11.07f, 20.83f, 11.46f)
                lineTo(21.54f, 12.17f)
                curveTo(21.93f, 12.56f, 21.93f, 13.19f, 21.54f, 13.58f)
                lineTo(20.13f, 14.99f)
                lineTo(18.01f, 12.87f)
                close()
                moveTo(17.3f, 13.58f)
                lineTo(12.5f, 18.38f)
                verticalLineTo(20.5f)
                horizontalLineTo(14.62f)
                lineTo(19.42f, 15.7f)
                lineTo(17.3f, 13.58f)
                close()
            }
        }.build()
    }

    val FactCheck: ImageVector by lazy {
        ImageVector.Builder(
            name = "FactCheck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 3f)
                horizontalLineTo(5f)
                curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
                verticalLineTo(19f)
                curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
                horizontalLineTo(19f)
                curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
                verticalLineTo(5f)
                curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
                close()
                moveTo(10f, 17f)
                lineTo(5.7f, 12.7f)
                curveTo(5.31f, 12.31f, 5.31f, 11.68f, 5.7f, 11.29f)
                curveTo(6.09f, 10.9f, 6.72f, 10.9f, 7.11f, 11.29f)
                lineTo(10f, 14.17f)
                lineTo(16.88f, 7.29f)
                curveTo(17.27f, 6.9f, 17.9f, 6.9f, 18.29f, 7.29f)
                curveTo(18.68f, 7.68f, 18.68f, 8.31f, 18.29f, 8.7f)
                lineTo(10.7f, 16.29f)
                curveTo(10.51f, 16.48f, 10.26f, 16.59f, 10f, 16.59f)
                curveTo(9.74f, 16.59f, 9.49f, 16.48f, 9.3f, 16.29f)
                close()
            }
        }.build()
    }

    val History: ImageVector by lazy {
        ImageVector.Builder(
            name = "History",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(13f, 3f)
                curveTo(8.03f, 3f, 4f, 7.03f, 4f, 12f)
                horizontalLineTo(1.5f)
                curveTo(1.05f, 12f, 0.83f, 12.54f, 1.15f, 12.85f)
                lineTo(4.65f, 16.35f)
                curveTo(4.85f, 16.55f, 5.16f, 16.55f, 5.36f, 16.35f)
                lineTo(8.85f, 12.85f)
                curveTo(9.17f, 12.54f, 8.95f, 12f, 8.5f, 12f)
                horizontalLineTo(6f)
                curveTo(6f, 8.13f, 9.13f, 5f, 13f, 5f)
                curveTo(16.87f, 5f, 20f, 8.13f, 20f, 12f)
                curveTo(20f, 15.87f, 16.87f, 19f, 13f, 19f)
                curveTo(11.07f, 19f, 9.32f, 18.22f, 8.04f, 16.96f)
                curveTo(7.65f, 16.57f, 7.02f, 16.57f, 6.63f, 16.96f)
                curveTo(6.24f, 17.35f, 6.24f, 17.98f, 6.63f, 18.37f)
                curveTo(8.29f, 20f, 10.53f, 21f, 13f, 21f)
                curveTo(17.97f, 21f, 22f, 16.97f, 22f, 12f)
                curveTo(22f, 7.03f, 17.97f, 3f, 13f, 3f)
                close()
                moveTo(12f, 7f)
                curveTo(11.45f, 7f, 11f, 7.45f, 11f, 8f)
                verticalLineTo(12.41f)
                curveTo(11f, 12.68f, 11.11f, 12.93f, 11.29f, 13.12f)
                lineTo(14.83f, 16.66f)
                curveTo(15.22f, 17.05f, 15.85f, 17.05f, 16.24f, 16.66f)
                curveTo(16.63f, 16.27f, 16.63f, 15.64f, 16.24f, 15.25f)
                lineTo(13f, 12.01f)
                verticalLineTo(8f)
                curveTo(13f, 7.45f, 12.55f, 7f, 12f, 7f)
                close()
            }
        }.build()
    }

    val SupportAgent: ImageVector by lazy {
        ImageVector.Builder(
            name = "SupportAgent",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(21f, 12.22f)
                curveTo(20.94f, 7.35f, 16.96f, 3.42f, 12.08f, 3.42f)
                curveTo(7.14f, 3.42f, 3.1f, 7.45f, 3.1f, 12.39f)
                curveTo(2.45f, 12.78f, 2f, 13.51f, 2f, 14.34f)
                verticalLineTo(16.79f)
                curveTo(2f, 17.9f, 2.9f, 18.79f, 4f, 18.79f)
                horizontalLineTo(5f)
                verticalLineTo(12.38f)
                curveTo(5f, 8.52f, 8.14f, 5.38f, 12f, 5.38f)
                curveTo(15.86f, 5.38f, 19f, 8.52f, 19f, 12.38f)
                verticalLineTo(19.46f)
                horizontalLineTo(10f)
                curveTo(9.45f, 19.46f, 9f, 19.91f, 9f, 20.46f)
                reflectiveCurveTo(9.45f, 21.46f, 10f, 21.46f)
                horizontalLineTo(19f)
                curveTo(20.1f, 21.46f, 21f, 20.57f, 21f, 19.46f)
                verticalLineTo(17.48f)
                curveTo(21.65f, 17.15f, 22.1f, 16.48f, 22.1f, 15.69f)
                verticalLineTo(14.02f)
                curveTo(22.1f, 13.23f, 21.65f, 12.56f, 21f, 12.22f)
                close()
            }
        }.build()
    }

    val Air: ImageVector by lazy {
        ImageVector.Builder(
            name = "Air",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(14.5f, 17f)
                curveTo(14.5f, 15.62f, 15.62f, 14.5f, 17f, 14.5f)
                curveTo(18.38f, 14.5f, 19.5f, 15.62f, 19.5f, 17f)
                curveTo(19.5f, 18.38f, 18.38f, 19.5f, 17f, 19.5f)
                horizontalLineTo(2f)
                curveTo(1.45f, 19.5f, 1f, 19.05f, 1f, 18.5f)
                reflectiveCurveTo(1.45f, 17.5f, 2f, 17.5f)
                horizontalLineTo(17f)
                curveTo(17.28f, 17.5f, 17.5f, 17.28f, 17.5f, 17f)
                curveTo(17.5f, 16.72f, 17.28f, 16.5f, 17f, 16.5f)
                curveTo(16.72f, 16.5f, 16.5f, 16.72f, 16.5f, 17f)
                horizontalLineTo(14.5f)
                close()
                moveTo(18f, 4f)
                curveTo(16.34f, 4f, 15f, 5.34f, 15f, 7f)
                horizontalLineTo(17f)
                curveTo(17f, 6.45f, 17.45f, 6f, 18f, 6f)
                curveTo(18.55f, 6f, 19f, 6.45f, 19f, 7f)
                curveTo(19f, 7.55f, 18.55f, 8f, 18f, 8f)
                horizontalLineTo(2f)
                curveTo(1.45f, 8f, 1f, 8.45f, 1f, 9f)
                reflectiveCurveTo(1.45f, 10f, 2f, 10f)
                horizontalLineTo(18f)
                curveTo(19.66f, 10f, 21f, 8.66f, 21f, 7f)
                curveTo(21f, 5.34f, 19.66f, 4f, 18f, 4f)
                close()
                moveTo(19f, 10.5f)
                curveTo(17.62f, 10.5f, 16.5f, 11.62f, 16.5f, 13f)
                horizontalLineTo(18.5f)
                curveTo(18.5f, 12.72f, 18.72f, 12.5f, 19f, 12.5f)
                curveTo(19.28f, 12.5f, 19.5f, 12.72f, 19.5f, 13f)
                curveTo(19.5f, 13.28f, 19.28f, 13.5f, 19f, 13.5f)
                horizontalLineTo(2f)
                curveTo(1.45f, 13.5f, 1f, 13.95f, 1f, 14.5f)
                reflectiveCurveTo(1.45f, 15.5f, 2f, 15.5f)
                horizontalLineTo(19f)
                curveTo(20.38f, 15.5f, 21.5f, 14.38f, 21.5f, 13f)
                curveTo(21.5f, 11.62f, 20.38f, 10.5f, 19f, 10.5f)
                close()
            }
        }.build()
    }

    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "Settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19.14f, 12.94f)
                curveTo(19.18f, 12.63f, 19.21f, 12.32f, 19.21f, 12f)
                curveTo(19.21f, 11.68f, 19.18f, 11.37f, 19.14f, 11.06f)
                lineTo(21.16f, 9.48f)
                curveTo(21.34f, 9.34f, 21.39f, 9.08f, 21.28f, 8.88f)
                lineTo(19.37f, 5.57f)
                curveTo(19.26f, 5.37f, 19.01f, 5.3f, 18.8f, 5.38f)
                lineTo(16.42f, 6.34f)
                curveTo(15.93f, 5.96f, 15.4f, 5.64f, 14.82f, 5.4f)
                lineTo(14.46f, 2.87f)
                curveTo(14.43f, 2.64f, 14.23f, 2.47f, 14f, 2.47f)
                horizontalLineTo(10.18f)
                curveTo(9.95f, 2.47f, 9.75f, 2.64f, 9.72f, 2.87f)
                lineTo(9.36f, 5.4f)
                curveTo(8.78f, 5.64f, 8.25f, 5.96f, 7.76f, 6.34f)
                lineTo(5.38f, 5.38f)
                curveTo(5.17f, 5.3f, 4.92f, 5.37f, 4.81f, 5.57f)
                lineTo(2.9f, 8.88f)
                curveTo(2.79f, 9.08f, 2.84f, 9.34f, 3.02f, 9.48f)
                lineTo(5.04f, 11.06f)
                curveTo(5f, 11.37f, 4.97f, 11.69f, 4.97f, 12f)
                curveTo(4.97f, 12.31f, 5f, 12.63f, 5.04f, 12.94f)
                lineTo(3.02f, 14.52f)
                curveTo(2.84f, 14.66f, 2.79f, 14.92f, 2.9f, 15.12f)
                lineTo(4.81f, 18.43f)
                curveTo(4.92f, 18.63f, 5.17f, 18.7f, 5.38f, 18.62f)
                lineTo(7.76f, 17.66f)
                curveTo(8.25f, 18.04f, 8.78f, 18.36f, 9.36f, 18.6f)
                lineTo(9.72f, 21.13f)
                curveTo(9.75f, 21.36f, 9.95f, 21.53f, 10.18f, 21.53f)
                horizontalLineTo(14f)
                curveTo(14.23f, 21.53f, 14.43f, 21.36f, 14.46f, 21.13f)
                lineTo(14.82f, 18.6f)
                curveTo(15.4f, 18.36f, 15.93f, 18.04f, 16.42f, 17.66f)
                lineTo(18.8f, 18.62f)
                curveTo(19.01f, 18.7f, 19.26f, 18.63f, 19.37f, 18.43f)
                lineTo(21.28f, 15.12f)
                curveTo(21.39f, 14.92f, 21.34f, 14.66f, 21.16f, 14.52f)
                lineTo(19.14f, 12.94f)
                close()
                moveTo(12.09f, 15.5f)
                curveTo(10.16f, 15.5f, 8.59f, 13.93f, 8.59f, 12f)
                curveTo(8.59f, 10.07f, 10.16f, 8.5f, 12.09f, 8.5f)
                curveTo(14.02f, 8.5f, 15.59f, 10.07f, 15.59f, 12f)
                curveTo(15.59f, 13.93f, 14.02f, 15.5f, 12.09f, 15.5f)
                close()
            }
        }.build()
    }

    val Phone: ImageVector by lazy {
        ImageVector.Builder(
            name = "Phone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19.23f, 15.26f)
                lineTo(16.69f, 14.97f)
                curveTo(16.08f, 14.9f, 15.48f, 15.11f, 15.05f, 15.54f)
                lineTo(13.21f, 17.38f)
                curveTo(10.38f, 15.94f, 8.06f, 13.63f, 6.62f, 10.79f)
                lineTo(8.47f, 8.94f)
                curveTo(8.9f, 8.51f, 9.11f, 7.91f, 9.04f, 7.3f)
                lineTo(8.75f, 4.78f)
                curveTo(8.63f, 3.77f, 7.78f, 3.01f, 6.76f, 3.01f)
                horizontalLineTo(4.99f)
                curveTo(3.88f, 3.01f, 2.96f, 3.94f, 3.03f, 5.05f)
                curveTo(3.55f, 13.28f, 10.73f, 20.45f, 18.96f, 20.97f)
                curveTo(20.07f, 21.04f, 21f, 20.12f, 21f, 19.01f)
                verticalLineTo(17.25f)
                curveTo(21f, 16.23f, 20.24f, 15.38f, 19.23f, 15.26f)
                close()
            }
        }.build()
    }

    val Description: ImageVector by lazy {
        ImageVector.Builder(
            name = "Description",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(14f, 2f)
                horizontalLineTo(6f)
                curveTo(4.9f, 2f, 4.01f, 2.9f, 4.01f, 4f)
                lineTo(4f, 20f)
                curveTo(4f, 21.1f, 4.89f, 22f, 5.99f, 22f)
                horizontalLineTo(18f)
                curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
                verticalLineTo(8f)
                lineTo(14f, 2f)
                close()
                moveTo(16f, 18f)
                horizontalLineTo(8f)
                curveTo(7.45f, 18f, 7f, 17.55f, 7f, 17f)
                reflectiveCurveTo(7.45f, 16f, 8f, 16f)
                horizontalLineTo(16f)
                curveTo(16.55f, 16f, 17f, 16.55f, 17f, 17f)
                reflectiveCurveTo(16.55f, 18f, 16f, 18f)
                close()
                moveTo(16f, 14f)
                horizontalLineTo(8f)
                curveTo(7.45f, 14f, 7f, 13.55f, 7f, 13f)
                reflectiveCurveTo(7.45f, 12f, 8f, 12f)
                horizontalLineTo(16f)
                curveTo(16.55f, 12f, 17f, 12.55f, 17f, 13f)
                reflectiveCurveTo(16.55f, 14f, 16f, 14f)
                close()
                moveTo(13f, 9f)
                verticalLineTo(3.5f)
                lineTo(18.5f, 9f)
                horizontalLineTo(13f)
                close()
            }
        }.build()
    }

    val Info: ImageVector by lazy {
        ImageVector.Builder(
            name = "Info",
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
                moveTo(13f, 17f)
                horizontalLineTo(11f)
                verticalLineTo(11f)
                horizontalLineTo(13f)
                verticalLineTo(17f)
                close()
                moveTo(13f, 9f)
                horizontalLineTo(11f)
                verticalLineTo(7f)
                horizontalLineTo(13f)
                verticalLineTo(9f)
                close()
            }
        }.build()
    }
}
