package MIGRATION

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Adicione a nova coluna `isAtualizar` com um valor padrão
        database.execSQL("ALTER TABLE tabela_tarefas ADD COLUMN isAtualizar INTEGER NOT NULL DEFAULT 0")
    }
}


val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Renomeie a tabela original para um nome temporário
        database.execSQL("ALTER TABLE tabela_tarefas RENAME TO tabela_tarefas_temp")

        // Crie a nova tabela sem a coluna isAtualizar
        database.execSQL("""
            CREATE TABLE tabela_tarefas (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                titulo TEXT NOT NULL,
                descricao TEXT NOT NULL,
                data INTEGER NOT NULL,
                isChecked INTEGER NOT NULL
            )
        """.trimIndent())

        // Copie os dados da tabela temporária para a nova tabela
        database.execSQL("""
            INSERT INTO tabela_tarefas (id, titulo, descricao, data, isChecked)
            SELECT id, titulo, descricao, data, isChecked FROM tabela_tarefas_temp
        """.trimIndent())

        // Remova a tabela temporária
        database.execSQL("DROP TABLE tabela_tarefas_temp")
    }
}