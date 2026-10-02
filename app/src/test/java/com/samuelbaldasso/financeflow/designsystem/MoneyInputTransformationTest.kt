package com.samuelbaldasso.financeflow.designsystem

import androidx.compose.ui.text.AnnotatedString
import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.designsystem.component.MoneyInputTransformation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyInputTransformationTest {
    @Test
    fun `formats cents without changing the integer amount`() {
        val result = MoneyInputTransformation(CurrencyCode.BRL).filter(AnnotatedString("123456"))
        assertTrue(result.text.text.contains("1.234,56"))
        assertEquals(result.text.length, result.offsetMapping.originalToTransformed(6))
        assertEquals(6, result.offsetMapping.transformedToOriginal(result.text.length))
    }

    @Test
    fun `cursor positions are bounded and monotonic including leading zeros`() {
        for (input in listOf("", "1", "12", "0001", "123456789", Long.MAX_VALUE.toString())) {
            val result = MoneyInputTransformation(CurrencyCode.BRL).filter(AnnotatedString(input))
            val forward = (0..input.length).map { result.offsetMapping.originalToTransformed(it) }
            val backward = (0..result.text.length).map { result.offsetMapping.transformedToOriginal(it) }
            assertTrue(forward.all { it in 0..result.text.length })
            assertTrue(backward.all { it in 0..input.length })
            assertTrue(forward.zipWithNext().all { (a, b) -> a <= b })
            assertTrue(backward.zipWithNext().all { (a, b) -> a <= b })
        }
    }
}
