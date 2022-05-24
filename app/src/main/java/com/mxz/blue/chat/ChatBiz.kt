package com.mxz.blue.chat

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*


val SERVICE_UUID: UUID = UUID.fromString("b3d076e6-69bb-478f-87a6-8f36ecd52603")
const val ACT_CLIENT_CONNECTED = "com.mxz.bluetoothChat.action.ACT_CLIENT_CONNECTED"
const val ACT_SERVER_LISTENING = "com.mxz.bluetoothChat.action.ACT_SERVER_LISTENING"
const val ACT_SERVER_ACCEPTED = "com.mxz.bluetoothChat.action.ACT_SERVER_ACCEPTED"
const val ACT_MESSAGE_READ = "com.mxz.bluetoothChat.action.ACT_MESSAGE_READ"
const val ACT_MESSAGE_WRITE_SUCCEED = "com.mxz.bluetoothChat.action.ACT_MESSAGE_WRITE_SUCCEED"
const val ACT_MESSAGE_WRITE_FAILED = "com.mxz.bluetoothChat.action.ACT_MESSAGE_WRITE_FAILED"

const val TAG = "BT_CHAT"

class ChatBiz(
  private val contextReference: WeakReference<Context>,
  private val activity: MainActivity
) {
  private val bluetoothManager = activity.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
  private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter

  private val context: Context get() = contextReference.get()!!

  private var connectThread: ConnectThread? = null
  private var acceptThread: AcceptThread? = null
  private var transferThread: DataTransferThread? = null

  var remoteDeviceName: String = "Unknown"

  @SuppressLint("MissingPermission")
  fun enableBluetooth() {
    if (!bluetoothAdapter.isEnabled) {
      bluetoothAdapter.enable()
    }
  }

  fun startDiscovery() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      if (ActivityCompat.checkSelfPermission(
          context,
          Manifest.permission.BLUETOOTH_SCAN
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        activity.requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN), 254)
      }
    }

    if (bluetoothAdapter.isDiscovering)
      bluetoothAdapter.cancelDiscovery()

    if (bluetoothAdapter.startDiscovery()) {
      Log.d(
        TAG,
        "Discovery Started."
      )
    } else {
      Log.d(
        TAG,
        "Discovery Failed. Make sure your device location is enabled and permission granted."
      )
    }
  }

  fun listen() {
    acceptThread = AcceptThread()
    acceptThread?.start()
  }

  fun cancelListening() {
    acceptThread?.cancel()
  }

  fun connect(device: BluetoothDevice) {
    connectThread = ConnectThread(device)
    connectThread?.start()
  }

  fun disconnect() {
    connectThread?.cancel()
  }

  fun sendMessage(message: String) {
    transferThread?.write(message)
  }

  private fun startTransfer(socket: BluetoothSocket) {
    transferThread = DataTransferThread(socket)
    transferThread?.start()
  }

  // region inner Classes
  @SuppressLint("MissingPermission")
  private inner class AcceptThread : Thread() {
    private val mmServerSocket: BluetoothServerSocket by lazy(LazyThreadSafetyMode.NONE) {
      bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("Bluetooth Chat", SERVICE_UUID)
    }

    override fun run() {
      var shouldLoop = true

      while (shouldLoop) {
        val socket: BluetoothSocket? = try {
          context.sendBroadcast(Intent(ACT_SERVER_LISTENING))
          mmServerSocket.accept()
        } catch (e: IOException) {
          Log.d(TAG, "Socket's accept() method failed. ${e.message}")
          shouldLoop = false
          null
        }
        socket?.also {
          context.sendBroadcast(Intent(ACT_SERVER_ACCEPTED))
          remoteDeviceName = it.remoteDevice.name

          startTransfer(it)

//          mmServerSocket.close()
          shouldLoop = false
        }
      }
    }

    fun cancel() {
      try {
        mmServerSocket.close()
      } catch (e: IOException) {
        Log.d(TAG, "Could not close the connect socket. ${e.message}")
      }
    }
  }

  @SuppressLint("MissingPermission")
  private inner class ConnectThread(device: BluetoothDevice) : Thread() {
    private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
      device.createRfcommSocketToServiceRecord(SERVICE_UUID)
    }

    override fun run() {

      bluetoothAdapter.cancelDiscovery()

      mmSocket?.let { socket ->
        try {
          socket.connect()
          context.sendBroadcast(Intent(ACT_CLIENT_CONNECTED))
          remoteDeviceName = socket.remoteDevice.name
          startTransfer(socket)

        } catch (e: IOException) {
          Log.d(TAG, "Could not close the connect socket. ${e.message}")
        }

      }

    }

    fun cancel() {
      try {
        mmSocket?.close()
      } catch (e: IOException) {
        Log.d(TAG, "Could not close the connect socket. ${e.message}")
      }
    }
  }

  private inner class DataTransferThread(val mmSocket: BluetoothSocket) : Thread() {
    private val mmInStream = mmSocket.inputStream
    private val mmOutStream = mmSocket.outputStream
    private val mmBuffer: ByteArray = ByteArray(1024)

    override fun run() {
      read()
    }

    fun read() {
      var numBytes: Int

      while (true) {
        try {
          sleep(1000)
          if (mmSocket.isConnected) {
            Log.i(TAG, "socket is connected, reading...")
            numBytes = mmInStream.read(mmBuffer)
            val intent = Intent(ACT_MESSAGE_READ)
            intent.putExtra("numBytes", numBytes)
            intent.putExtra("buffer", mmBuffer)
            context.sendBroadcast(intent)
          }
        } catch (e: IOException) {
          Log.d(TAG, "Input stream was disconnected", e)
          break
        }
      }
    }

    fun write(message: String) {
      val bytes: ByteArray = message.toByteArray()
      try {
        if (mmSocket.isConnected) {
          Log.i(TAG, "socket is connected, writing...")
          mmOutStream.write(bytes)
          val intent = Intent(ACT_MESSAGE_WRITE_SUCCEED)
          intent.putExtra("message", message)
          context.sendBroadcast(intent)
        }
      } catch (e: IOException) {
        Log.e(TAG, "Error occurred when sending data", e)
        context.sendBroadcast(Intent(ACT_MESSAGE_WRITE_FAILED))
      }
    }
  }


  // endregion

  companion object {
    @Volatile
    private var INSTANCE: ChatBiz? = null

    fun getInstance(context: Context, fragmentActivity: FragmentActivity): ChatBiz =
      INSTANCE ?: synchronized(this) {
        INSTANCE ?: initChatBiz(context, fragmentActivity).also { INSTANCE = it }
      }

    private fun initChatBiz(context: Context, fragmentActivity: FragmentActivity) =
      ChatBiz(WeakReference(context), fragmentActivity as MainActivity)

  }

}