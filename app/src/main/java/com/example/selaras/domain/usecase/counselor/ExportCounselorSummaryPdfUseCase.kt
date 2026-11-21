@file:Suppress("NewApi")

package com.example.selaras.domain.usecase.counselor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.selaras.domain.model.CounselorSummary
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Generates a structured PDF report matching the Serasi style from a frozen CounselorSummary snapshot.
 * Features strict typographic hierarchy, structured tables for screenings & personal notes,
 * and user identity integration.
 */
class ExportCounselorSummaryPdfUseCase {

    operator fun invoke(
        context: Context,
        summary: CounselorSummary,
        fileName: String = "Serasi · Ringkasan Mingguan.pdf",
        userName: String? = null
    ): Result<File> = runCatching {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 at 72dpi
        var page = document.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        // Palette Serasi Tokens
        val darkGreenText = Color.rgb(0x24, 0x3B, 0x35)       // #243B35
        val mutedGreenText = Color.rgb(0x52, 0x67, 0x5F)      // #52675F
        val pineGreen = Color.rgb(0x35, 0x68, 0x59)           // #356859
        val lightSageBg = Color.rgb(0xE0, 0xED, 0xE4)         // #E0EDE4
        val cardBg = Color.rgb(0xF7, 0xF7, 0xF2)              // #F7F7F2
        val cardBorder = Color.rgb(0xDC, 0xEC, 0xE3)          // #DCECE3
        val tableAltRow = Color.rgb(0xFA, 0xFC, 0xFA)         // #FAFCFA
        val lavenderBg = Color.rgb(0xEE, 0xE5, 0xF5)          // #EEE5F5
        val lavenderText = Color.rgb(0x4A, 0x3E, 0x56)        // #4A3E56

        // Typography Paints
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkGreenText
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerSubtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedGreenText
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pineGreen
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkGreenText
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkGreenText
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkGreenText
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedGreenText
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cardBorder
            style = Paint.Style.STROKE
            strokeWidth = 0.75f
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        val snapshot = try {
            JSONObject(summary.snapshotJson)
        } catch (_: Exception) {
            JSONObject()
        }

        val resolvedUserName = userName?.takeIf { it.isNotBlank() }
            ?: snapshot.optString("userName").takeIf { it.isNotBlank() }
            ?: "Pengguna Serasi"

        val periodFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id"))
        val periodStr = "${summary.periodStartDate.format(periodFmt)} – ${summary.periodEndDate.format(periodFmt)}"
        val exportTimeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy · HH.mm", Locale.forLanguageTag("id")))

        var y = 40f
        var pageNumber = 1

        fun drawFooter(c: Canvas, pageNum: Int) {
            c.drawLine(40f, 796f, 555f, 796f, borderPaint)
            mutedPaint.textSize = 7.5f
            c.drawText("by Selaras", 40f, 810f, mutedPaint)
            c.drawText("Hal. $pageNum", 526f, 810f, mutedPaint)
        }

        fun checkPageBreak(neededHeight: Float) {
            if (y + neededHeight > 780f) {
                drawFooter(canvas, pageNumber)
                document.finishPage(page)
                pageNumber++
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 44f
            }
        }

        // =========================================================================
        // 1. Header Banner & Branding
        // =========================================================================
        canvas.drawText("Selaras: Ringkasan Mingguan untuk Konselor", 40f, y, titlePaint)
        y += 15f
        canvas.drawText("Rekapitulasi data observasi mandiri untuk mempermudah sesi konsultasi.", 40f, y, headerSubtitlePaint)
        y += 16f

        // Disclaimer Card (Lavender)
        fillPaint.color = lavenderBg
        canvas.drawRoundRect(40f, y, 555f, y + 26f, 6f, 6f, fillPaint)
        fillPaint.color = lavenderText
        fillPaint.textSize = 8f
        fillPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText(
            "Bukan Diagnosis Klinis: Ringkasan ini murni catatan observasi mandiri dan tidak menggantikan evaluasi medis profesional.",
            48f,
            y + 16f,
            fillPaint
        )
        y += 36f

        // =========================================================================
        // 2. Metadata Pengguna & Dokumen (Structured Card)
        // =========================================================================
        fillPaint.color = cardBg
        canvas.drawRoundRect(40f, y, 555f, y + 46f, 6f, 6f, fillPaint)
        canvas.drawRoundRect(40f, y, 555f, y + 46f, 6f, 6f, borderPaint)

        // Left Column: Nama & Periode
        canvas.drawText("Nama Klien / Pengguna :", 52f, y + 17f, bodyBoldPaint)
        canvas.drawText(resolvedUserName, 172f, y + 17f, bodyPaint)

        canvas.drawText("Periode Observasi     :", 52f, y + 34f, bodyBoldPaint)
        canvas.drawText(periodStr, 172f, y + 34f, bodyPaint)

        // Right Column: Jumlah Hari & Waktu Ekspor
        canvas.drawText("Hari Tercatat :", 360f, y + 17f, bodyBoldPaint)
        canvas.drawText("${summary.checkinDaysCount} dari 7 hari valid", 435f, y + 17f, bodyPaint)

        canvas.drawText("Waktu Cetak   :", 360f, y + 34f, bodyBoldPaint)
        canvas.drawText(exportTimeStr, 435f, y + 34f, mutedPaint)
        y += 68f

        // =========================================================================
        // 3. Section 01: Analisis Check-in Harian
        // =========================================================================
        checkPageBreak(130f)
        canvas.drawText("01 · Analisis Check-in Harian", 40f, y, sectionTitlePaint)
        y += 12f

        // Metric Card
        fillPaint.color = cardBg
        canvas.drawRoundRect(40f, y, 555f, y + 54f, 6f, 6f, fillPaint)
        canvas.drawRoundRect(40f, y, 555f, y + 54f, 6f, 6f, borderPaint)

        val avgMood = snapshot.optDouble("avgMood", 0.0)
        val avgSleep = snapshot.optDouble("avgSleepHours", 0.0)
        val avgEnergy = snapshot.optDouble("avgEnergy", 0.0)

        canvas.drawText(String.format(Locale.forLanguageTag("id"), "• Rata-rata Suasana Hati : %.1f / 5", avgMood), 52f, y + 17f, bodyPaint)
        canvas.drawText(String.format(Locale.forLanguageTag("id"), "• Rata-rata Durasi Tidur : %.1f jam / hari", avgSleep), 52f, y + 31f, bodyPaint)
        canvas.drawText(String.format(Locale.forLanguageTag("id"), "• Rata-rata Tingkat Energi : %.1f / 10", avgEnergy), 52f, y + 45f, bodyPaint)
        y += 66f

        // Daily breakdown entries
        val dailyEntries = snapshot.optJSONArray("dailyEntries")
        if (dailyEntries != null && dailyEntries.length() > 0) {
            checkPageBreak(20f + dailyEntries.length() * 14f)
            canvas.drawText("Rincian Harian (Mood / Durasi Tidur / Energi):", 40f, y, bodyBoldPaint)
            y += 14f

            for (i in 0 until dailyEntries.length()) {
                val item = dailyEntries.getJSONObject(i)
                val rawDate = item.optString("date")
                val formattedDayDate = try {
                    LocalDate.parse(rawDate).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id")))
                } catch (_: Exception) {
                    rawDate
                }
                val m = item.optInt("mood")
                val s = item.optDouble("sleepHours")
                val e = item.optInt("energy")
                val line = String.format(Locale.forLanguageTag("id"), "  • %s : Mood %d/5  ·  Tidur %.1f jam  ·  Energi %d/10", formattedDayDate, m, s, e)
                canvas.drawText(line, 48f, y, bodyPaint)
                y += 13f
            }
            y += 8f
        }

        // =========================================================================
        // 4. Section 02: Hasil Skrining Terpilih (Structured Table)
        // =========================================================================
        val screenings = snapshot.optJSONArray("screenings")
        val screeningCount = screenings?.length() ?: 0
        val screeningTableHeight = 22f + (if (screeningCount > 0) screeningCount * 20f else 22f)
        checkPageBreak(24f + screeningTableHeight)

        canvas.drawText("02 · Hasil Skrining PHQ-9 Terpilih", 40f, y, sectionTitlePaint)
        y += 12f

        // Table Coordinates: Total Width = 515f (from 40f to 555f)
        // Col 1: Jenis Skrining (135f) -> 40f .. 175f
        // Col 2: Tanggal Penapisan (105f) -> 175f .. 280f
        // Col 3: Skor PHQ-9 (85f) -> 280f .. 365f
        // Col 4: Kategori / Interpretasi (190f) -> 365f .. 555f

        val colX1 = 40f
        val colX2 = 175f
        val colX3 = 280f
        val colX4 = 365f
        val tableRight = 555f

        // Table Header
        val headerH = 20f
        fillPaint.color = lightSageBg
        canvas.drawRect(colX1, y, tableRight, y + headerH, fillPaint)
        canvas.drawRect(colX1, y, tableRight, y + headerH, borderPaint)
        canvas.drawLine(colX2, y, colX2, y + headerH, borderPaint)
        canvas.drawLine(colX3, y, colX3, y + headerH, borderPaint)
        canvas.drawLine(colX4, y, colX4, y + headerH, borderPaint)

        canvas.drawText("Jenis Skrining", colX1 + 8f, y + 13f, tableHeaderPaint)
        canvas.drawText("Tanggal Selesai", colX2 + 8f, y + 13f, tableHeaderPaint)
        canvas.drawText("Skor Total", colX3 + 8f, y + 13f, tableHeaderPaint)
        canvas.drawText("Kategori Penapisan", colX4 + 8f, y + 13f, tableHeaderPaint)
        y += headerH

        if (screenings != null && screenings.length() > 0) {
            for (i in 0 until screenings.length()) {
                val sc = screenings.getJSONObject(i)
                val type = sc.optString("type", "Skrining PHQ-9")
                val dateStr = sc.optString("date").ifEmpty { periodStr }
                val score = sc.optInt("score", 0)
                val cat = sc.optString("category", "Gejala Ringan")

                val rowH = 20f
                fillPaint.color = if (i % 2 == 1) tableAltRow else Color.WHITE
                canvas.drawRect(colX1, y, tableRight, y + rowH, fillPaint)
                canvas.drawRect(colX1, y, tableRight, y + rowH, borderPaint)
                canvas.drawLine(colX2, y, colX2, y + rowH, borderPaint)
                canvas.drawLine(colX3, y, colX3, y + rowH, borderPaint)
                canvas.drawLine(colX4, y, colX4, y + rowH, borderPaint)

                canvas.drawText(type, colX1 + 8f, y + 13f, bodyBoldPaint)
                canvas.drawText(dateStr, colX2 + 8f, y + 13f, bodyPaint)
                canvas.drawText("$score / 27", colX3 + 8f, y + 13f, bodyBoldPaint)
                canvas.drawText(cat, colX4 + 8f, y + 13f, bodyPaint)
                y += rowH
            }
        } else {
            val emptyH = 20f
            fillPaint.color = Color.WHITE
            canvas.drawRect(colX1, y, tableRight, y + emptyH, fillPaint)
            canvas.drawRect(colX1, y, tableRight, y + emptyH, borderPaint)
            mutedPaint.textSize = 8.5f
            canvas.drawText("Tidak ada hasil kuesioner skrining yang disertakan.", colX1 + 10f, y + 13f, mutedPaint)
            y += emptyH
        }
        y += 18f

        // =========================================================================
        // 5. Section 03: Catatan Pribadi (Structured Table with Date)
        // =========================================================================
        val includeNotes = snapshot.optBoolean("includeNotes", false)

        // Extract notes from daily entries
        val noteItems = mutableListOf<Pair<String, String>>()
        if (includeNotes && dailyEntries != null) {
            for (i in 0 until dailyEntries.length()) {
                val item = dailyEntries.getJSONObject(i)
                val note = item.optString("note", "").trim()
                if (note.isNotBlank()) {
                    val rawDate = item.optString("date")
                    val fmtDate = try {
                        LocalDate.parse(rawDate).format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id")))
                    } catch (_: Exception) {
                        rawDate
                    }
                    noteItems.add(fmtDate to note)
                }
            }
        }

        checkPageBreak(50f)
        canvas.drawText("03 · Catatan Refleksi Pribadi (Opsional)", 40f, y, sectionTitlePaint)
        y += 12f

        // Table Coordinates for Notes: Width = 515f (40f .. 555f)
        // Col 1: Tanggal Observasi (115f) -> 40f .. 155f
        // Col 2: Isi Catatan Refleksi (400f) -> 155f .. 555f
        val noteColX1 = 40f
        val noteColX2 = 155f
        val noteTableRight = 555f

        // Header
        val noteHeaderH = 20f
        fillPaint.color = lightSageBg
        canvas.drawRect(noteColX1, y, noteTableRight, y + noteHeaderH, fillPaint)
        canvas.drawRect(noteColX1, y, noteTableRight, y + noteHeaderH, borderPaint)
        canvas.drawLine(noteColX2, y, noteColX2, y + noteHeaderH, borderPaint)

        canvas.drawText("Tanggal Observasi", noteColX1 + 8f, y + 13f, tableHeaderPaint)
        canvas.drawText("Isi Catatan Refleksi Mandiri", noteColX2 + 8f, y + 13f, tableHeaderPaint)
        y += noteHeaderH

        if (!includeNotes || noteItems.isEmpty()) {
            val emptyH = 20f
            fillPaint.color = Color.WHITE
            canvas.drawRect(noteColX1, y, noteTableRight, y + emptyH, fillPaint)
            canvas.drawRect(noteColX1, y, noteTableRight, y + emptyH, borderPaint)
            mutedPaint.textSize = 8.5f
            val emptyMsg = if (!includeNotes) {
                "Pilihan catatan refleksi pribadi nonaktif / tidak disertakan."
            } else {
                "Tidak ada catatan refleksi yang ditulis pada periode ini."
            }
            canvas.drawText(emptyMsg, noteColX1 + 10f, y + 13f, mutedPaint)
            y += emptyH
        } else {
            for (idx in noteItems.indices) {
                val (dateStr, noteText) = noteItems[idx]

                // Word wrap helper for notes
                fun wrapNoteText(text: String, maxChars: Int = 66): List<String> {
                    val words = text.split(" ")
                    val lines = mutableListOf<String>()
                    var cur = StringBuilder()
                    for (w in words) {
                        if (cur.length + w.length + 1 > maxChars) {
                            lines.add(cur.toString())
                            cur = StringBuilder(w)
                        } else {
                            if (cur.isNotEmpty()) cur.append(" ")
                            cur.append(w)
                        }
                    }
                    if (cur.isNotEmpty()) lines.add(cur.toString())
                    return lines
                }

                val wrappedLines = wrapNoteText("\"$noteText\"")
                val rowHeight = (wrappedLines.size * 13f + 10f).coerceAtLeast(22f)

                checkPageBreak(rowHeight)

                fillPaint.color = if (idx % 2 == 1) tableAltRow else Color.WHITE
                canvas.drawRect(noteColX1, y, noteTableRight, y + rowHeight, fillPaint)
                canvas.drawRect(noteColX1, y, noteTableRight, y + rowHeight, borderPaint)
                canvas.drawLine(noteColX2, y, noteColX2, y + rowHeight, borderPaint)

                canvas.drawText(dateStr, noteColX1 + 8f, y + 14f, bodyBoldPaint)

                var textY = y + 14f
                for (line in wrappedLines) {
                    canvas.drawText(line, noteColX2 + 8f, textY, bodyPaint)
                    textY += 13f
                }

                y += rowHeight
            }
        }

        // Draw footer on last page
        drawFooter(canvas, pageNumber)
        document.finishPage(page)

        val outputDir = File(context.filesDir, "counselor_reports").apply { mkdirs() }
        val safeFileName = fileName.replace("/", "_")
        val outputFile = File(outputDir, safeFileName)
        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
        outputFile
    }
}
