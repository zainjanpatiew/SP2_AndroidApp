package models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class User {
    @PrimaryKey(autoGenerate = true)
    var id = 0
    var userId: String = ""
    var configId: String = ""
    var name:String = ""
    var file:String = ""
}