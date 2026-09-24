package com.samuelbaldasso.financeflow.domain

import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.core.model.transaction.Transaction
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionStatus
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionType
import com.samuelbaldasso.financeflow.domain.usecase.importer.CsvTransactionService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class CsvTransactionServiceTest {

    private val accountId = UUID.randomUUID()

    @Test
    fun `computeDuplicateHash normalizes casing and whitespace`() {
        val date = LocalDate.of(2026, 3, 15)
        val amount = 15000L

        val hash1 = CsvTransactionService.computeDuplicateHash(
            accountId = accountId,
            date = date,
            amountMinor = amount,
            description = "  Supermercado   Extra  "
        )

        val hash2 = CsvTransactionService.computeDuplicateHash(
            accountId = accountId,
            date = date,
            amountMinor = amount,
            description = "supermercado extra"
        )

        val hash3 = CsvTransactionService.computeDuplicateHash(
            accountId = accountId,
            date = date,
            amountMinor = amount,
            description = "SUPERMERCADO EXTRA"
        )

        assertEquals(hash1, hash2)
        assertEquals(hash2, hash3)
    }

    @Test
    fun `parseCsv correctly parses comma and semicolon delimited files with quotes`() {
        val csvComma = """
            Data,Descricao,Valor
            2026-03-10,"Compra no Mercado, Centro",150.50
            2026-03-11,Farmacia,45.00
        """.trimIndent()

        val parsedComma = CsvTransactionService.parseCsv(csvComma)
        assertEquals(listOf("Data", "Descricao", "Valor"), parsedComma.headers)
        assertEquals(2, parsedComma.rows.size)
        assertEquals(listOf("2026-03-10", "Compra no Mercado, Centro", "150.50"), parsedComma.rows[0])
        assertEquals(listOf("2026-03-11", "Farmacia", "45.00"), parsedComma.rows[1])

        val csvSemi = """
            Data;Descricao;Valor
            10/03/2026;Restaurante;89,90
        """.trimIndent()

        val parsedSemi = CsvTransactionService.parseCsv(csvSemi)
        assertEquals(listOf("Data", "Descricao", "Valor"), parsedSemi.headers)
        assertEquals(1, parsedSemi.rows.size)
        assertEquals(listOf("10/03/2026", "Restaurante", "89,90"), parsedSemi.rows[0])
    }

    @Test
    fun `generatePreview identifies duplicates against existing transactions`() {
        val existingTxDate = LocalDate.of(2026, 3, 10)
        val existingTx = Transaction(
            accountId = accountId,
            type = TransactionType.EXPENSE,
            amount = Money(150_50L),
            competenceDate = existingTxDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
            effectiveDate = existingTxDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
            description = "Supermercado Pão de Açúcar",
            status = TransactionStatus.CLEARED
        )

        val csvContent = """
            Data,Descricao,Valor
            2026-03-10,supermercado pão de açúcar,-150.50
            2026-03-12,Cinema,-35.00
        """.trimIndent()

        val table = CsvTransactionService.parseCsv(csvContent)
        val candidates = CsvTransactionService.generatePreview(
            table = table,
            targetAccountId = accountId,
            existingTransactions = listOf(existingTx),
            dateColIndex = 0,
            descColIndex = 1,
            amountColIndex = 2
        )

        assertEquals(2, candidates.size)

        // First candidate matches existingTx -> isDuplicate must be true
        assertTrue(candidates[0].isDuplicate)
        assertEquals(150_50L, candidates[0].amount.amountMinor)
        assertEquals(TransactionType.EXPENSE, candidates[0].type)

        // Second candidate is new -> isDuplicate must be false
        assertFalse(candidates[1].isDuplicate)
        assertEquals(35_00L, candidates[1].amount.amountMinor)
        assertEquals(TransactionType.EXPENSE, candidates[1].type)
    }

    @Test
    fun `exportTransactionsToCsv formats RFC-compliant CSV with header and quotes`() {
        val tx1 = Transaction(
            id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            accountId = accountId,
            type = TransactionType.INCOME,
            amount = Money(5000_00L),
            competenceDate = LocalDate.of(2026, 3, 5).atStartOfDay(ZoneOffset.UTC).toInstant(),
            effectiveDate = LocalDate.of(2026, 3, 5).atStartOfDay(ZoneOffset.UTC).toInstant(),
            description = "Salário \"Mensal\"",
            status = TransactionStatus.CLEARED
        )

        val csv = CsvTransactionService.exportTransactionsToCsv(listOf(tx1))

        val lines = csv.lines().filter { it.isNotBlank() }
        assertEquals(2, lines.size)
        assertEquals("ID,Data_Competencia,Tipo,Valor_Centavos,Descricao,Status,Transfer_ID", lines[0])
        assertTrue(lines[1].contains("00000000-0000-0000-0000-000000000001"))
        assertTrue(lines[1].contains("2026-03-05"))
        assertTrue(lines[1].contains("INCOME"))
        assertTrue(lines[1].contains("500000"))
        assertTrue(lines[1].contains("\"Salário \"\"Mensal\"\"\""))
        assertTrue(lines[1].contains("CLEARED"))
    }
}
