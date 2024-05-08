package Room

import Modelo.Notas
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Notas::class], version = 2, exportSchema = true)
abstract class AppDataBase: RoomDatabase() {

    abstract fun NotaDao(): NotaDao

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