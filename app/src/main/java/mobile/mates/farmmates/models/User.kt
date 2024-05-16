package mobile.mates.farmmates.models

class User(
    var name: String,
    var lastName: String,
    var phoneNumber: String,
    var id: String,
    var profilePicUrl: String,
    var lat: Double,
    var long: Double
) {
    constructor() : this("", "", "", "", "", 0.0, 0.0)
}