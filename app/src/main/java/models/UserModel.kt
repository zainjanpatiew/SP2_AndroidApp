package models

data class UserModel(
    var email: String = "",
    var emergency_contract_name: String = "",
    var emergency_contract_relation: String = "",
    var emergency_contract_surname: String = "",
    var employeeid: String = "",
    var faceAdded: Boolean = false,
    var features: String?= null,
    var first_name: String = "",
    var id: String = "",
    var is_admin: Boolean = false,
    var config_id:String = "",
    var last_name: String = "",
    var phone_number: String = "",
    var position: String = "",
    var zipcode: String = ""
)
