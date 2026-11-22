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
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Generates a structured PDF report matching the Serasi style from a frozen CounselorSummary snapshot.
 */
class ExportCounselorSummaryPdfUseCase {

    operator fun invoke(
        context: Context,
        summary: CounselorSummary,
        fileName: String = "Serasi · Ringkasan Mingguan.pdf"
    ): Result<File> = runCatching {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 at 72dpi
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x24, 0x3B, 0x35) // SelarasTextPrimary
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerSubtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x52, 0x67, 0x5F)
            textSize = 10f
        }
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x35, 0x68, 0x59) // SelarasPrimary
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x24, 0x3B, 0x35)
            textSize = 10f
        }
        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0xF7, 0xF7, 0xF2)
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0xDC, 0xEC, 0xE3)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        var y = 44f
        // Top Banner / Header
        canvas.drawText("Selaras · Ringkasan Mingguan untuk Konselor", 40f, y, titlePaint)
        y += 18f
        canvas.drawText("Dokumen rekapitulasi data observasi mandiri untuk mempermudah sesi konsultasi.", 40f, y, headerSubtitlePaint)
        y += 20f

        // Disclaimer Box (Bukan diagnosis klinis)
        paint.color = Color.rgb(0xEE, 0xE5, 0xF5) // Lavender
        canvas.drawRoundRect(40f, y, 555f, y + 42f, 8f, 8f, paint)
        paint.color = Color.rgb(0x4A, 0x3E, 0x56)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("Pernyataan: Bukan Diagnosis Klinis", 52f, y + 16f, paint)
        canvas.drawText("Ringkasan ini membantu percakapan, bukan penetapan kondisi klinis. Disusun secara lokal oleh pengguna.", 52f, y + 30f, paint)
        y += 58f

        // Metadata
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id"))
        val periodStr = "${summary.periodStartDate.format(formatter)} – ${summary.periodEndDate.format(formatter)}"
        bodyPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Periode Observasi: $periodStr", 40f, y, bodyPaint)
        y += 24f

        val snapshot = try {
            JSONObject(summary.snapshotJson)
        } catch (_: Exception) {
            JSONObject()
        }

        // Section 01: Analisis Check-in
        canvas.drawText("01 · Analisis Check-in Harian", 40f, y, sectionTitlePaint)
        y += 16f

        canvas.drawRoundRect(40f, y, 555f, y + 100f, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(40f, y, 555f, y + 100f, 8f, 8f, cardBorderPaint)

        bodyPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val recordedDays = summary.checkinDaysCount
        canvas.drawText("Hari Tercatat: $recordedDays dari 7 hari valid", 52f, y + 20f, bodyPaint)

        val avgMood = snapshot.optDouble("avgMood", 0.0)
        val avgSleep = snapshot.optDouble("avgSleepHours", 0.0)
        val avgEnergy = snapshot.optDouble("avgEnergy", 0.0)

        canvas.drawText(String.format(Locale.forLanguageTag("id"), "• Mood rata-rata: %.1f / 5", avgMood), 52f, y + 40f, bodyPaint)
        canvas.drawText(String.format(Locale.forLanguageTag("id"), "• Durasi tidur rata-rata: %.1f jam", avgSleep), 52f, y + 60f, bodyPaint)
        canvas.drawText(String.format(Locale.forLanguageTag("id"), "• Energi rata-rata: %.1f / 5", avgEnergy), 52f, y + 80f, bodyPaint)
        y += 116f

        // Daily entries
        val dailyEntries = snapshot.optJSONArray("dailyEntries")
        if (dailyEntries != null && dailyEntries.length() > 0) {
            bodyPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Rincian Harian:", 40f, y, bodyPaint)
            y += 16f
            bodyPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            for (i in 0 until dailyEntries.length()) {
                val item = dailyEntries.getJSONObject(i)
                val d = item.optString("date")
                val m = item.optInt("mood")
                val s = item.optDouble("sleepHours")
                val e = item.optInt("energy")
                val line = String.format(Locale.forLanguageTag("id"), "• %s — Mood: %d/5, Tidur: %.1f jam, Energi: %d/5", d, m, s, e)
                canvas.drawText(line, 52f, y, bodyPaint)
                y += 14f

                val note = item.optString("note", "")
                if (note.isNotBlank()) {
                    paint.color = Color.rgb(0x52, 0x67, 0x5F)
                    paint.textSize = 9f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    canvas.drawText("   Catatan: \"$note\"", 52f, y, paint)
                    y += 14f
                }
            }
            y += 12f
        }

        // Section 02: Hasil Skrining
        canvas.drawText("02 · Hasil Skrining Terpilih", 40f, y, sectionTitlePaint)
        y += 16f
        val screenings = snapshot.optJSONArray("screenings")
        if (screenings != null && screenings.length() > 0) {
            bodyPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            for (i in 0 until screenings.length()) {
                val sc = screenings.getJSONObject(i)
                val type = sc.optString("type")
                val score = sc.optInt("score")
                val cat = sc.optString("category")
                val instrument = sc.optString("instrumentVersion", "")
                val isDemo = instrument.startsWith("DEMO-", ignoreCase = true)
                val line = if (isDemo) {
                    "• Refleksi demo $type: Skor simulasi $score (tanpa interpretasi klinis)"
                } else {
                    "• Skrining $type: Skor $score ($cat)"
                }
                canvas.drawText(line, 52f, y, bodyPaint)
                y += 14f
            }
        } else {
            bodyPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("• Tidak ada hasil skrining yang disertakan. Empat hari check-in cukup tanpa skrining tambahan.", 52f, y, bodyPaint)
            y += 16f
        }
        y += 16f

        // Section 03: Catatan Pribadi
        val includeNotes = snapshot.optBoolean("includeNotes", false)
        canvas.drawText("03 · Catatan Pribadi", 40f, y, sectionTitlePaint)
        y += 16f
        bodyPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        if (!includeNotes) {
            bodyPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("• Pilihan catatan pribadi nonaktif. Tidak ada teks catatan dalam ringkasan ini.", 52f, y, bodyPaint)
        } else {
            canvas.drawText("• Catatan pribadi terlampir sesuai tanggal yang dipilih pengguna.", 52f, y, bodyPaint)
        }
        y += 30f

        // Footer
        paint.color = Color.rgb(0x7B, 0x91, 0x87)
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Dihasilkan secara lokal di perangkat · Privasi terlindungi · Selaras Mobile", 40f, 800f, paint)

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
