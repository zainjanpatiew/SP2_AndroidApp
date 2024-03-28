package database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import models.User

@Dao
interface UserDao {

    @Insert
    suspend fun insert(user: User)

    @Update
    suspend fun updateUser(user: User): Int

    @get:Query("SELECT * FROM User")
    val getUsers: List<User>
}
