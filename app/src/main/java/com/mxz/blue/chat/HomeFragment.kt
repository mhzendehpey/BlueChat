package com.mxz.blue.chat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.activity.result.registerForActivityResult
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.mxz.blue.chat.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

  private lateinit var bluetoothConnectPermissionLauncher: ActivityResultLauncher<Unit>
  private lateinit var bluetoothScanPermissionLauncher: ActivityResultLauncher<Unit>
  private var _binding: FragmentHomeBinding? = null

  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding get() = _binding!!

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {

    _binding = FragmentHomeBinding.inflate(inflater, container, false)

    initPermissionRequestLauncher()

    return binding.root

  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.btnClient.setOnClickListener {
      onClickClient()
    }
    binding.btnServer.setOnClickListener {
      onClickServer()
    }

  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  private fun initPermissionRequestLauncher() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      if (ActivityCompat.checkSelfPermission(
          requireContext(),
          Manifest.permission.BLUETOOTH_CONNECT
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        bluetoothConnectPermissionLauncher = registerForActivityResult(
          ActivityResultContracts.RequestPermission(),
          Manifest.permission.BLUETOOTH_CONNECT
        ) {
          if (it) {
            findNavController().navigate(R.id.action_HomeFragment_to_serverFragment)
          }
        }


        bluetoothScanPermissionLauncher = registerForActivityResult(
          ActivityResultContracts.RequestPermission(),
          Manifest.permission.BLUETOOTH_SCAN
        ) {
          if (it) {
            findNavController().navigate(R.id.action_HomeFragment_to_ClientFragment)
          }
        }
      }
    }
  }

  private fun onClickClient() {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
      ActivityCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.BLUETOOTH_SCAN
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      bluetoothScanPermissionLauncher.launch()
      updateStatus("Please allow requested permission to continue using this app.")
    } else {
      findNavController().navigate(R.id.action_HomeFragment_to_ClientFragment)
    }
  }

  private fun onClickServer() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.BLUETOOTH_CONNECT
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      bluetoothConnectPermissionLauncher.launch()
      updateStatus("Please allow requested permission to continue using this app.")
    } else {
      findNavController().navigate(R.id.action_HomeFragment_to_serverFragment)
    }
  }

  private fun updateStatus(status: String) {
    (activity as MainActivity).updateStatus(status, Snackbar.LENGTH_LONG)
  }

  private fun dismissStatus() {
    (activity as MainActivity).dismissStatus()
  }


}