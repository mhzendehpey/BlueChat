package com.mxz.blue.chat

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContract
import androidx.navigation.fragment.findNavController
import com.mxz.blue.chat.databinding.FragmentServerBinding


class ServerFragment : Fragment() {

  // region Properties
  private var _binding: FragmentServerBinding? = null
  private val binding get() = _binding!!

  private var parentActivity = activity as MainActivity?
  private var timer: CountDownTimer? = null

  private lateinit var chatBiz: ChatBiz

  // endregion

  // region Fragment Lifecycle

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentServerBinding.inflate(inflater, container, false)

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    setUpBroadcastReceiver()

    chatBiz = ChatBiz.getInstance(requireContext(), requireActivity())

    chatBiz.listen()

    binding.btnDiscoverable.setOnClickListener {
      enableDiscoverability()
    }

    enableDiscoverability()
  }

  override fun onDestroyView() {
    dismissStatus()
    super.onDestroyView()
    _binding = null
//    chatBiz.cancelListening()
    requireActivity().unregisterReceiver(receiver)
    stopTimer()
  }

  // endregion

  // region Broadcast Receiver
  private fun setUpBroadcastReceiver() {
    val intentFilter = IntentFilter(ACT_SERVER_LISTENING)
    intentFilter.addAction(ACT_SERVER_ACCEPTED)
    requireActivity().registerReceiver(receiver, intentFilter)
  }

  private val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      when (intent.action) {
        ACT_SERVER_LISTENING -> {
          updateStatus("Listening...")
        }
        ACT_SERVER_ACCEPTED -> {
          updateStatus("Accepted!")
          findNavController().navigate(R.id.action_serverFragment_to_conversationFragment)
        }
      }
    }
  }

  // endregion

  // region Methods

  private fun enableDiscoverability() {
    class BtDiscoveryContract : ActivityResultContract<Unit, Int>() {
      override fun createIntent(context: Context, input: Unit?): Intent =
        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)

      override fun parseResult(resultCode: Int, intent: Intent?): Int = Activity.RESULT_OK
    }

    val launcher = registerForActivityResult(BtDiscoveryContract()) {
      if (it == Activity.RESULT_OK) {
        startTimer()
        binding.btnDiscoverable.visibility = View.INVISIBLE
      }
    }
    launcher.launch(Unit)
  }

  private fun startTimer(duration: Long = 120L) {
    timer = object : CountDownTimer(120 * 1000L, 1000L) {
      override fun onTick(millisToFinished: Long) {
//        binding.tvCounter.text = "Your device is discoverable for: ${millisToFinished / 1000}"
        binding.tvCounter.text = getString(R.string.discoverability_counter_text, millisToFinished / 1000)
        binding.tvCounter.visibility = View.VISIBLE
      }

      override fun onFinish() {
        binding.tvCounter.text = getString(R.string.no_longer_discoverable)
        binding.btnDiscoverable.visibility = View.VISIBLE
      }
    }

    (timer as CountDownTimer).start()

  }

  private fun stopTimer() {
    timer?.cancel()
  }

  private fun dismissStatus() {
    (activity as MainActivity).dismissStatus()
  }

  private fun updateStatus(status: String) {
    parentActivity = activity as MainActivity?
    parentActivity?.updateStatus(status)
  }

  // endregion

}