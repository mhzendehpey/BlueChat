package com.mxz.blue.chat

import java.util.*

enum class MessageDataType {
  TYPE_TEXT,
  TYPE_IMAGE
}

data class Message(
  var sender: String = "",
  var message: String = "",
  val date: Date,
  var dataType: MessageDataType = MessageDataType.TYPE_TEXT
) {
  var imageBytes: ByteArray? = null
}