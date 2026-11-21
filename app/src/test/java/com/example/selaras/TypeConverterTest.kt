package com.example.selaras

import com.example.selaras.core.database.converter.DateConverter
import com.example.selaras.core.database.converter.ScreeningConverter
import com.example.selaras.domain.model.ScreeningCategory
import com.example.selaras.domain.model.ScreeningType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class TypeConverterTest {

    private val dateConverter = DateConverter()
    private val screeningConverter = ScreeningConverter()

    @Test
    fun testLocalDateConverter() {
        val date = LocalDate.of(2026, 11, 19)
        val dateString = dateConverter.fromLocalDate(date)
        assertEquals("2026-11-19", dateString)

        val parsedDate = dateConverter.toLocalDate(dateString)
        assertEquals(date, parsedDate)
        assertNull(dateConverter.fromLocalDate(null))
        assertNull(dateConverter.toLocalDate(null))
    }

    @Test
    fun testInstantConverter() {
        val instant = Instant.parse("2026-11-19T14:10:00Z")
        val instantString = dateConverter.fromInstant(instant)
        assertEquals("2026-11-19T14:10:00Z", instantString)

        val parsedInstant = dateConverter.toInstant(instantString)
        assertEquals(instant, parsedInstant)
        assertNull(dateConverter.fromInstant(null))
        assertNull(dateConverter.toInstant(null))
    }

    @Test
    fun testLocalTimeConverter() {
        val time = LocalTime.of(21, 30, 0)
        val timeString = dateConverter.fromLocalTime(time)
        assertEquals("21:30:00", timeString)

        val parsedTime = dateConverter.toLocalTime(timeString)
        assertEquals(time, parsedTime)
        assertNull(dateConverter.fromLocalTime(null))
        assertNull(dateConverter.toLocalTime(null))
    }

    @Test
    fun testScreeningTypeConverter() {
        assertEquals("manual", screeningConverter.fromScreeningType(ScreeningType.MANUAL))
        assertEquals("routine", screeningConverter.fromScreeningType(ScreeningType.ROUTINE))
        assertEquals(ScreeningType.MANUAL, screeningConverter.toScreeningType("manual"))
        assertEquals(ScreeningType.ROUTINE, screeningConverter.toScreeningType("routine"))
        assertNull(screeningConverter.fromScreeningType(null))
        assertNull(screeningConverter.toScreeningType(null))
    }

    @Test
    fun testScreeningCategoryConverter() {
        assertEquals("minimal", screeningConverter.fromScreeningCategory(ScreeningCategory.MINIMAL))
        assertEquals("ringan", screeningConverter.fromScreeningCategory(ScreeningCategory.RINGAN))
        assertEquals("sedang", screeningConverter.fromScreeningCategory(ScreeningCategory.SEDANG))
        assertEquals("sedang_berat", screeningConverter.fromScreeningCategory(ScreeningCategory.SEDANG_BERAT))
        assertEquals("berat", screeningConverter.fromScreeningCategory(ScreeningCategory.BERAT))

        assertEquals(ScreeningCategory.MINIMAL, screeningConverter.toScreeningCategory("minimal"))
        assertEquals(ScreeningCategory.RINGAN, screeningConverter.toScreeningCategory("ringan"))
        assertEquals(ScreeningCategory.SEDANG, screeningConverter.toScreeningCategory("sedang"))
        assertEquals(ScreeningCategory.SEDANG_BERAT, screeningConverter.toScreeningCategory("sedang_berat"))
        assertEquals(ScreeningCategory.BERAT, screeningConverter.toScreeningCategory("berat"))
        assertNull(screeningConverter.fromScreeningCategory(null))
        assertNull(screeningConverter.toScreeningCategory(null))
    }
}
