package com.mxz.blue.chat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mxz.blue.chat.databinding.FragmentConversationBinding
import java.util.*

class ConversationFragment : Fragment() {

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
          val buffer = intent.getByteArrayExtra("buffer")!!
          val numBytes = intent.getIntExtra("numBytes", -1)
          val msg = String(buffer, 0, numBytes)
          addMessage(msg, true)
        }
        ACT_MESSAGE_WRITE_SUCCEED -> {
          val msg = intent.getStringExtra("message")!!
          addMessage(msg, false)
        }
        ACT_MESSAGE_WRITE_FAILED -> {
          Toast.makeText(context, "Couldn't send data to the other device", Toast.LENGTH_SHORT)
            .show()
        }
      }
    }
  }

  // endregion

  // region Methods

  private fun addMessage(msg: String, isRemote: Boolean) {
    if (isRemote) addRemoteMessage(msg) else addLocalMessage(msg)
    binding.rvConversation.scrollToPosition(conversationAdapter.itemCount - 1)
  }

  private fun addRemoteMessage(msg: String) {
    conversationAdapter.add(Message(chatBiz.remoteDeviceName, msg, Calendar.getInstance().time))
  }

  private fun addLocalMessage(msg: String) {
    conversationAdapter.add(Message("You", msg, Calendar.getInstance().time))
  }

  private fun sendMessage() {
    val message = binding.edtMessage.text.toString()
    chatBiz.sendMessage(message)
  }

  private fun dismissStatus() {
    (activity as MainActivity).dismissStatus()
  }

  // endregion

}