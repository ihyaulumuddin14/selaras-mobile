package com.example.selaras.domain.usecase.checkin

import com.example.selaras.domain.model.CheckIn
import com.example.selaras.domain.model.MoodTrend
import com.example.selaras.domain.model.WeeklyCheckInAnalysis
import com.example.selaras.domain.repository.CheckInRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import kotlin.math.roundToInt

class GetWeeklyCheckInAnalysisUseCase(
    private val repository: CheckInRepository
) {
    operator fun invoke(endDate: LocalDate = LocalDate.now()): Flow<WeeklyCheckInAnalysis> {
        val startDate = endDate.minusDays(6)
        return repository.getCheckInsBetweenFlow(startDate, endDate).map { entries ->
            analyze(entries, startDate, endDate)
        }
    }

    suspend fun getOnce(endDate: LocalDate = LocalDate.now()): WeeklyCheckInAnalysis {
        val startDate = endDate.minusDays(6)
        val entries = repository.getCheckInsBetween(startDate, endDate)
        return analyze(entries, startDate, endDate)
    }

    private fun analyze(
        entries: List<CheckIn>,
        startDate: LocalDate,
        endDate: LocalDate
    ): WeeklyCheckInAnalysis {
        val count = entries.size
        if (count == 0) {
            return WeeklyCheckInAnalysis(
                startDate = startDate,
                endDate = endDate,
                entries = emptyList(),
                totalDaysRecorded = 0,
                averageMood = 0.0,
                averageEnergy = 0.0,
                averageSleepMinutes = 0,
                moodTrend = MoodTrend.INSUFFICIENT_DATA,
                isEligibleForCounselorSummary = false,
                insights = listOf("Belum ada catatan check-in pada periode 7 hari ini. Mulai catat kondisi harimu untuk melihat analisis pola.")
            )
        }

        val avgMood = (entries.map { it.mood }.average() * 10.0).roundToInt() / 10.0
        val avgEnergy = (entries.map { it.energy }.average() * 10.0).roundToInt() / 10.0
        val avgSleep = entries.map { it.sleepDurationMinutes }.average().roundToInt()

        val trend = calculateMoodTrend(entries)
        val isEligible = count >= 4

        val insights = buildList {
            if (avgSleep < 360) {
                add("Rata-rata tidur (${avgSleep / 60} jam ${avgSleep % 60} menit) berada di bawah rekomendasi 6-8 jam. Menjaga waktu istirahat dapat membantu kestabilan emosi.")
            } else if (avgSleep >= 420 && avgEnergy >= 7.0) {
                add("Durasi tidur malam yang cukup berkorelasi positif dengan tingginya tingkat energimu.")
            }

            if (avgMood <= 2.5 || avgEnergy <= 4.0) {
                add("Kondisi energi atau suasana hati cenderung rendah minggu ini. Disarankan mengambil jeda sejenak dengan Latihan Relaksasi Pernapasan.")
            } else if (avgMood >= 4.0) {
                add("Suasana hati rata-rata dalam kategori baik dan stabil sepanjang minggu ini.")
            }

            if (isEligible) {
                add("Data check-in telah mencapai $count hari (syarat min. 4 hari terpenuhi). Kamu dapat membuat Ringkasan Konselor kapan saja.")
            } else {
                val needed = 4 - count
                add("Dibutuhkan $needed hari pencatatan lagi untuk membuka fitur Ringkasan Konselor.")
            }
        }

        return WeeklyCheckInAnalysis(
            startDate = startDate,
            endDate = endDate,
            entries = entries,
            totalDaysRecorded = count,
            averageMood = avgMood,
            averageEnergy = avgEnergy,
            averageSleepMinutes = avgSleep,
            moodTrend = trend,
            isEligibleForCounselorSummary = isEligible,
            insights = insights
        )
    }

    private fun calculateMoodTrend(entries: List<CheckIn>): MoodTrend {
        if (entries.size < 3) return MoodTrend.INSUFFICIENT_DATA

        val mid = entries.size / 2
        val firstHalfAvg = entries.take(mid).map { it.mood }.average()
        val secondHalfAvg = entries.drop(mid).map { it.mood }.average()
        val diff = secondHalfAvg - firstHalfAvg

        return when {
            diff >= 0.5 -> MoodTrend.IMPROVING
            diff <= -0.5 -> MoodTrend.DECLINING
            else -> MoodTrend.STABLE
        }
    }
}
