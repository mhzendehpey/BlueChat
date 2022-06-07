package com.mxz.blue.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ConversationAdapter(private val mData: MutableList<Message>) :
  RecyclerView.Adapter<ConversationAdapter.MessageViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder =
    MessageViewHolder(
      LayoutInflater.from(parent.context).inflate(R.layout.row_message, parent, false)
    )

  override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
    holder.onBind(mData[position])
  }

  override fun getItemCount() = mData.size

  fun add(message: Message) {
    mData.add(message)
    notifyItemInserted(mData.lastIndex)
  }

  inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private lateinit var tvMessage: TextView
    private lateinit var tvSender: TextView
    private lateinit var tvDate: TextView
    private lateinit var ivImage: ImageView

    fun onBind(message: Message) {
      tvMessage = itemView.findViewById(R.id.tvMessage)
      tvSender = itemView.findViewById(R.id.tvSender)
      tvDate = itemView.findViewById(R.id.tvDate)
      ivImage = itemView.findViewById(R.id.ivImage)
      val root = itemView.findViewById<LinearLayout>(R.id.root)

      val senderText = message.sender + ":"
      tvSender.text = senderText
      val sdf = SimpleDateFormat("yyyy/MM/dd-HH:mm", Locale.getDefault()).format(message.date)
      tvDate.text = sdf

      when (message.dataType) {
        MessageDataType.TYPE_TEXT -> {
          ivImage.visibility = View.GONE
          tvMessage.visibility = View.VISIBLE
          tvMessage.text = message.message
        }
        MessageDataType.TYPE_IMAGE -> {
          ivImage.visibility = View.VISIBLE
          tvMessage.visibility = View.GONE
          val bitmap: Bitmap? = BitmapFactory.decodeByteArray(message.imageBytes, 0, message.imageBytes?.size!!)
          ivImage.setImageBitmap(bitmap)
          ivImage.maxHeight = bitmap?.height ?: 0
          ivImage.maxWidth = bitmap?.width ?: 0
        }
      }

      root.gravity = if (message.sender == "You") {
        Gravity.END
      } else {
        Gravity.START
      }


    }

  }
}