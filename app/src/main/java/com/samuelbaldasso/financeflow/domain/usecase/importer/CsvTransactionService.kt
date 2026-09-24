package com.samuelbaldasso.financeflow.domain.usecase.importer

import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.core.model.transaction.Transaction
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionStatus
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionType
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

data class CsvParsedTable(
    val headers: List<String>,
    val rows: List<List<String>>
)

data class ImportCandidate(
    val date: LocalDate,
    val description: String,
    val amount: Money,
    val type: TransactionType,
    val duplicateHash: String,
    val isDuplicate: Boolean
)

object CsvTransactionService {

    fun computeDuplicateHash(
        accountId: UUID,
        date: LocalDate,
        amountMinor: Long,
        description: String
    ): String {
        val normalizedDesc = description.trim().lowercase().replace("\\s+".toRegex(), " ")
        val raw = "$accountId|$date|$amountMinor|$normalizedDesc"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun parseCsv(csvContent: String): CsvParsedTable {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return CsvParsedTable(emptyList(), emptyList())

        val firstLine = lines.first()
        val delimiter = if (firstLine.contains(";")) ";" else ","

        val headers = parseLine(firstLine, delimiter)
        val rows = lines.drop(1).map { parseLine(it, delimiter) }

        return CsvParsedTable(headers = headers, rows = rows)
    }

    private fun parseLine(line: String, delimiter: String): List<String> {
        val tokens = mutableListOf<String>()
        var inQuotes = false
        val sb = StringBuilder()

        for (char in line) {
            when {
                char == '\"' -> inQuotes = !inQuotes
                char.toString() == delimiter && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(char)
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    fun generatePreview(
        table: CsvParsedTable,
        targetAccountId: UUID,
        existingTransactions: List<Transaction>,
        dateColIndex: Int,
        descColIndex: Int,
        amountColIndex: Int,
        typeColIndex: Int? = null
    ): List<ImportCandidate> {
        // Pre-compute existing hashes
        val existingHashes = existingTransactions
            .filter { it.accountId == targetAccountId }
            .map { tx ->
                val date = tx.competenceDate.atZone(ZoneOffset.UTC).toLocalDate()
                computeDuplicateHash(targetAccountId, date, tx.amount.amountMinor, tx.description)
            }.toSet()

        val candidates = mutableListOf<ImportCandidate>()

        for (row in table.rows) {
            if (row.size <= maxOf(dateColIndex, descColIndex, amountColIndex)) continue

            val rawDate = row.getOrNull(dateColIndex)?.trim() ?: continue
            val rawDesc = row.getOrNull(descColIndex)?.trim() ?: continue
            val rawAmount = row.getOrNull(amountColIndex)?.trim() ?: continue

            val parsedDate = parseDateLenient(rawDate) ?: LocalDate.now()
            val parsedMinor = parseAmountMinor(rawAmount)
            if (parsedMinor <= 0L) continue

            val parsedType = if (typeColIndex != null) {
                val rawType = row.getOrNull(typeColIndex)?.uppercase() ?: ""
                when {
                    rawType.contains("REC") || rawType.contains("INCOME") -> TransactionType.INCOME
                    rawType.contains("TRANS") -> TransactionType.TRANSFER
                    else -> TransactionType.EXPENSE
                }
            } else {
                if (rawAmount.contains("-")) TransactionType.EXPENSE else TransactionType.INCOME
            }

            val hash = computeDuplicateHash(targetAccountId, parsedDate, parsedMinor, rawDesc)
            val isDuplicate = existingHashes.contains(hash)

            candidates.add(
                ImportCandidate(
                    date = parsedDate,
                    description = rawDesc,
                    amount = Money(parsedMinor),
                    type = parsedType,
                    duplicateHash = hash,
                    isDuplicate = isDuplicate
                )
            )
        }

        return candidates
    }

    private fun parseDateLenient(dateStr: String): LocalDate? {
        val formats = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy")
        )
        for (formatter in formats) {
            try {
                return LocalDate.parse(dateStr, formatter)
            } catch (_: Exception) {}
        }
        return null
    }

    private fun parseAmountMinor(amountStr: String): Long {
        // Remove currency symbols, whitespace, and negative signs
        val clean = amountStr.replace("[R$€$,\\s-]".toRegex(), "")
        // If has decimal point or comma: e.g. 150.50 -> 15050
        val sanitized = if (amountStr.contains(",") && !amountStr.contains(".")) {
            // Brazilian comma format: 150,50
            val parts = amountStr.replace("[^0-9,]".toRegex(), "").split(",")
            val major = parts.getOrNull(0)?.toLongOrNull() ?: 0L
            val minor = (parts.getOrNull(1)?.padEnd(2, '0')?.take(2))?.toLongOrNull() ?: 0L
            major * 100L + minor
        } else if (amountStr.contains(".")) {
            val parts = amountStr.replace("[^0-9.]".toRegex(), "").split(".")
            val major = parts.getOrNull(0)?.toLongOrNull() ?: 0L
            val minor = (parts.getOrNull(1)?.padEnd(2, '0')?.take(2))?.toLongOrNull() ?: 0L
            major * 100L + minor
        } else {
            (clean.toLongOrNull() ?: 0L) * 100L
        }
        return sanitized.coerceAtLeast(0L)
    }

    fun exportTransactionsToCsv(transactions: List<Transaction>): String {
        val sb = StringBuilder()
        sb.appendLine("ID,Data_Competencia,Tipo,Valor_Centavos,Descricao,Status,Transfer_ID")
        val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        for (tx in transactions) {
            val date = tx.competenceDate.atZone(ZoneOffset.UTC).toLocalDate().format(dateFormatter)
            val line = listOf(
                tx.id.toString(),
                date,
                tx.type.name,
                tx.amount.amountMinor.toString(),
                "\"${tx.description.replace("\"", "\"\"")}\"",
                tx.status.name,
                tx.transferId?.toString() ?: ""
            ).joinToString(",")
            sb.appendLine(line)
        }
        return sb.toString()
    }
}
