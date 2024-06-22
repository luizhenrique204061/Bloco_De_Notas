package Room

import MIGRATION.MIGRATION_10_11
import MIGRATION.MIGRATION_8_9
import MIGRATION.MIGRATION_9_10
import Modelo.Notas
import Modelo.Tarefa
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Notas::class, Tarefa::class], version = 11, exportSchema = true)
abstract class AppDataBase : RoomDatabase() {

    abstract fun NotaDao(): NotaDao
    abstract fun TarefaDao(): TarefaDao

    companion object {
        fun getInstance(context: Context): AppDataBase {
            return Room.databaseBuilder(
                context,
                AppDataBase::class.java,
                "db.notas"
            ).addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11).build()
        }
    }
}