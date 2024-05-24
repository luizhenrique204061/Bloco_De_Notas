package Room

import Modelo.Notas
import Modelo.Tarefa
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Notas::class, Tarefa::class], version = 4, exportSchema = true)
abstract class AppDataBase: RoomDatabase() {

    abstract fun NotaDao(): NotaDao
    abstract fun TarefaDao(): TarefaDao

    companion object {
        fun getInstance(context: Context): AppDataBase {
            return Room.databaseBuilder(
                context,
                AppDataBase::class.java,
                "db.notas"
            ).fallbackToDestructiveMigration().build()
        }
    }
}