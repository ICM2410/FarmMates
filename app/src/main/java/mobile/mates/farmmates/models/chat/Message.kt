package mobile.mates.farmmates.models.chat

import java.util.Date

class Message (
    var room : String = "",
    var content: String = "",
    var senderId: String = "",
    var createdAt: Date = Date()
)