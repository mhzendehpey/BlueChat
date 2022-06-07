package com.mxz.blue.chat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mxz.blue.chat.databinding.FragmentConversationBinding
import java.util.*

class ConversationFragment : Fragment() {

  private lateinit var selectImageLauncher: ActivityResultLauncher<String>

  // region Properties
  private var _binding: FragmentConversationBinding? = null
  private val binding: FragmentConversationBinding get() = _binding!!

  private lateinit var chatBiz: ChatBiz
  private lateinit var conversationAdapter: ConversationAdapter

  // endregion

  // region Fragment Lifecycle
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentConversationBinding.inflate(inflater, container, false)

    selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
      selectImage(it)
    }

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    chatBiz = ChatBiz.getInstance(requireContext(), requireActivity())

    conversationAdapter = ConversationAdapter(mutableListOf())

    binding.rvConversation.layoutManager =
      LinearLayoutManager(context, RecyclerView.VERTICAL, false)
    binding.rvConversation.adapter = conversationAdapter

    binding.btnSend.setOnClickListener {
      sendMessage()
      binding.edtMessage.text.clear()
    }

    binding.btnSendImage.setOnClickListener {
//      sendImageMessage()
      selectImageLauncher.launch("image/*")
    }

    setUpBroadcastReceiver()

    dismissStatus()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
    chatBiz.disconnect()
    chatBiz.cancelListening()
  }

  // endregion

  // region BroadcastReceiver

  private fun setUpBroadcastReceiver() {
    val intentFilter = IntentFilter(ACT_MESSAGE_READ)
    intentFilter.addAction(ACT_MESSAGE_WRITE_SUCCEED)
    intentFilter.addAction(ACT_MESSAGE_WRITE_FAILED)
    requireActivity().registerReceiver(receiver, intentFilter)
  }

  private val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      when (intent.action) {
        ACT_MESSAGE_READ -> {
          onMessageReceived(intent, true)
        }
        ACT_MESSAGE_WRITE_SUCCEED -> {
          onMessageReceived(intent, false)
        }
        ACT_MESSAGE_WRITE_FAILED -> {
          Toast.makeText(context, "Couldn't send data to the other device", Toast.LENGTH_SHORT)
            .show()
        }
        ACT_CONNECTION_LOST -> {
          findNavController().navigateUp()
        }
      }
    }
  }

  // endregion

  // region Methods

  private fun selectImage(uri: Uri?) {
    uri?.let {
      val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireContext().contentResolver, uri))
      } else {
        MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
      }
      sendImageMessage(bitmap)
    }
  }

  private fun onMessageReceived(intent: Intent, isRemote: Boolean) {
    val buffer = intent.getByteArrayExtra("buffer")!!
    val numBytes = intent.getIntExtra("numBytes", -1)
    val messageDataType = intent.getSerializableExtra("msg_type")!! as MessageDataType
    addMessage(buffer, numBytes, messageDataType, isRemote)
  }

  private fun addMessage(
    bytes: ByteArray,
    numBytes: Int,
    messageDataType: MessageDataType,
    isRemote: Boolean
  ) {
    val message = Message(date = Calendar.getInstance().time)
    val sender = if (isRemote) {
      chatBiz.remoteDeviceName
    } else {
      "You"
    }

    message.sender = sender
    message.dataType = messageDataType

    when (messageDataType) {
      MessageDataType.TYPE_TEXT -> {
        val msg = String(bytes, 0, numBytes)
        message.message = msg
      }
      MessageDataType.TYPE_IMAGE -> {
        message.imageBytes = bytes
      }
    }
    conversationAdapter.add(message)
    binding.rvConversation.scrollToPosition(conversationAdapter.itemCount - 1)
  }

  private fun sendMessage() {
    val message = binding.edtMessage.text.toString()
    chatBiz.sendMessage(message)
  }

  private fun sendImageMessage(bitmap: Bitmap) {
    chatBiz.sendImageMessage(bitmap)
  }

  private fun dismissStatus() {
    (activity as MainActivity).dismissStatus()
  }

  // endregion

}