package com.samuelbaldasso.financeflow.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FinanceFlowMigrationTest {
    @Test
    fun `version one upgrades without losing account transaction or audit records`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-${UUID.randomUUID()}.db"
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        val schema = JSONObject(File("schemas/com.samuelbaldasso.financeflow.core.database.FinanceFlowDatabase/1.json").readText()).getJSONObject("database")
        val accountId = UUID.randomUUID()
        val txId = UUID.randomUUID()
        val auditId = UUID.randomUUID()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                fun expand(sql: String) = sql.replace("\${TABLE_NAME}", entity.getString("tableName"))
                old.execSQL(expand(entity.getString("createSql")))
                val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                for (j in 0 until indices.length()) old.execSQL(expand(indices.getJSONObject(j).getString("createSql")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
            old.execSQL("INSERT INTO accounts (id,name,type,currency,initial_balance_minor,is_archived,created_at,updated_at) VALUES (?, 'Preservada', 'CHECKING', 'BRL', 12345, 0, 0, 0)", arrayOf(accountId.toString()))
            old.execSQL("INSERT INTO transactions (id,account_id,type,amount_minor,competence_date,effective_date,description,tags,status,created_at,updated_at) VALUES (?, ?, 'INCOME', 500, 0, 0, 'Anterior', '', 'CLEARED', 0, 0)", arrayOf(txId.toString(), accountId.toString()))
            old.execSQL("INSERT INTO audit_logs (id,entity_type,entity_id,action,actor,timestamp) VALUES (?, 'TRANSACTION', ?, 'CREATE', 'user', 0)", arrayOf(auditId.toString(), txId.toString()))
            old.version = 1
        }
        val upgraded = Room.databaseBuilder(context, FinanceFlowDatabase::class.java, name)
            .addMigrations(FinanceFlowDatabase.MIGRATION_1_2).allowMainThreadQueries().build()
        try {
            assertEquals(12345L, upgraded.accountDao().getById(accountId)!!.initialBalanceMinor)
            assertEquals(500L, upgraded.transactionDao().getById(txId)!!.amountMinor)
            assertNotNull(upgraded.openHelper.writableDatabase)
            upgraded.openHelper.writableDatabase.query("SELECT COUNT(*) FROM audit_logs WHERE id = '$auditId'").use {
                it.moveToFirst(); assertEquals(1, it.getInt(0))
            }
            upgraded.openHelper.writableDatabase.query("PRAGMA user_version").use {
                it.moveToFirst(); assertEquals(2, it.getInt(0))
            }
        } finally {
            upgraded.close()
            context.deleteDatabase(name)
        }
    }
}
