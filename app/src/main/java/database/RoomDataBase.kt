package database

import android.annotation.SuppressLint
import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import models.User

@Database(entities = [User::class], version = 1, exportSchema = false)
abstract class RoomDataBase : RoomDatabase() {
    abstract fun userDao(): UserDao?

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: RoomDataBase? = null

        @SuppressLint("StaticFieldLeak")
        var mContext: Context? = null

        //creating local database
        @Synchronized
        fun getInstance(context: Context): RoomDataBase? {
            mContext = context
            if (instance == null) {
                instance = databaseBuilder(
                    context.applicationContext,
                    RoomDataBase::class.java,
                    "My_db"
                )
                    .build()
            }
            return instance
        }
    }
}
