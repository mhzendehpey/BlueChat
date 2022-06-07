package com.mxz.blue.chat

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.mxz.blue.chat.databinding.FragmentClientBinding

class ClientFragment : Fragment() {

  // region Properties

  private var _binding: FragmentClientBinding? = null
  private val binding get() = _binding!!

  private var parentActivity: MainActivity? = null

  private lateinit var chatBiz: ChatBiz
  private lateinit var discoveredDevicesAdapter: ArrayAdapter<BluetoothDevice>

  private lateinit var broadcastManager: LocalBroadcastManager
  // endregion

  // region Fragment Lifecycle
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentClientBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    chatBiz = ChatBiz.getInstance(requireContext(), requireActivity())
    broadcastManager = LocalBroadcastManager.getInstance(requireContext())
    enableBluetooth()

    binding.btnRetry.setOnClickListener {
      discoveredDevicesAdapter.clear()
      startDiscovery()
    }

    setupBroadcastReceiver()

    startDiscovery()

    initDiscoverDevicesListView()
  }

  override fun onDestroyView() {
    dismissStatus()
    super.onDestroyView()
    _binding = null
    requireActivity().unregisterReceiver(receiver)
//    chatBiz.disconnect()
  }

  // endregion

  // region BroadcastReceiver
  private fun setupBroadcastReceiver() {
    val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
    filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
    filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
    filter.addAction(ACT_CLIENT_CONNECTED)
    requireActivity().registerReceiver(receiver, filter)
  }

  private val receiver = object : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
      when (intent.action) {
        BluetoothDevice.ACTION_FOUND -> {
          val device: BluetoothDevice? =
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
          if (device?.name?.isNotEmpty() == true || device?.name?.isNotBlank() == true) {
            discoveredDevicesAdapter.add(device)
          }
        }
        BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
          binding.progressBar.visibility = View.VISIBLE
          updateStatus("Finding Devices...")
          binding.btnRetry.isEnabled = false
        }
        BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
          dismissStatus()
          binding.btnRetry.isEnabled = true
          binding.progressBar.visibility = View.INVISIBLE
        }
        ACT_CLIENT_CONNECTED -> {
          updateStatus("Connected!")
          findNavController().navigate(R.id.action_clientFragment_to_conversationFragment)
        }
      }
    }
  }

  // endregion

  // region Methods

  @SuppressLint("MissingPermission")
  private fun initDiscoverDevicesListView() {
    discoveredDevicesAdapter = object :
      ArrayAdapter<BluetoothDevice>(
        requireContext(),
        android.R.layout.simple_list_item_1
      ) {
      override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val tv = view.findViewById<TextView>(android.R.id.text1)
        tv.text = getItem(position)?.name ?: ""
        return view
      }
    }

    binding.lvDiscoveredDevices.adapter = discoveredDevicesAdapter
    binding.lvDiscoveredDevices.setOnItemClickListener { _, _, position, _ ->
      onDeviceItemClicked(position)
    }
  }

  private fun updateStatus(status: String) {
    parentActivity = activity as MainActivity?
    parentActivity?.updateStatus(status)
  }

  private fun dismissStatus() {
    (activity as MainActivity).dismissStatus()
  }

  // endregion

  // region ChatBiz

  private fun startDiscovery() {
    chatBiz.startDiscovery()
  }

  private fun enableBluetooth() {
    updateStatus("Turning On Bluetooth")
    chatBiz.enableBluetooth()
  }

  @SuppressLint("MissingPermission")
  private fun onDeviceItemClicked(position: Int) {
    val device = discoveredDevicesAdapter.getItem(position)
    updateStatus("Connecting ${device?.name ?: "Server"}...")

    chatBiz.connect(device!!)
  }

  // endregion

}