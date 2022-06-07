package com.mxz.blue.chat

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.round
import kotlin.math.sqrt


val SERVICE_UUID: UUID = UUID.fromString("b3d076e6-69bb-478f-87a6-8f36ecd52603")
const val ACT_CLIENT_CONNECTED = "com.mxz.blue.chat.action.ACT_CLIENT_CONNECTED"
const val ACT_SERVER_LISTENING = "com.mxz.blue.chat.action.ACT_SERVER_LISTENING"
const val ACT_SERVER_ACCEPTED = "com.mxz.blue.chat.action.ACT_SERVER_ACCEPTED"
const val ACT_MESSAGE_READ = "com.mxz.blue.chat.action.ACT_MESSAGE_READ"
const val ACT_MESSAGE_WRITE_SUCCEED = "com.mxz.blue.chat.action.ACT_MESSAGE_WRITE_SUCCEED"
const val ACT_MESSAGE_WRITE_FAILED = "com.mxz.blue.chat.action.ACT_MESSAGE_WRITE_FAILED"
const val ACT_CONNECTION_LOST = "com.mxz.blue.chat.action.ACT_CONNECTION_LOST"

const val BLUETOOTH_CONNECT_REQ = 8621

//const val TAG = "BT_CHAT"

val IMAGE_HEADER = listOf<Byte>(
  -1,
  -40,
  -1,
  -32,
  0,
  16,
  74,
  70,
  73,
  70,
  0,
  1,
  1,
  0,
  0,
  1,
  0,
  1,
  0,
  0,
  -1,
  -30,
  2,
  40,
  73,
  67,
  67,
  95,
  80,
  82,
  79,
  70,
  73,
  76,
  69,
)

val IMAGE_TAIL = listOf<Byte>(
  -1, -39
)

fun ByteArray.startsWith(bytes: List<Byte>, first: Int = 10): Boolean {
  for ((i, b) in bytes.take(first).withIndex()) {
    if (b != this[i])
      return false
  }
  return true
}

fun ByteArray.endsWith(bytes: List<Byte>) =
  this.takeLast(2) == bytes.takeLast(2)


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
        activity.requestPermissions(
          arrayOf(Manifest.permission.BLUETOOTH_SCAN),
          BLUETOOTH_CONNECT_REQ
        )
      }
    }

    if (bluetoothAdapter.isDiscovering)
      bluetoothAdapter.cancelDiscovery()

    if (!bluetoothAdapter.startDiscovery()) {
      Toast.makeText(
        context,
        "Discovery Failed. Make sure your device location is enabled and permission granted.",
        Toast.LENGTH_SHORT
      ).show()
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
    val bytes: ByteArray = message.toByteArray()
    transferThread?.write(bytes, MessageDataType.TYPE_TEXT)
  }

  fun sendImageMessage(image: Bitmap) {

    val scaledBitmap = scaleBitmap(image)

    val stream = ByteArrayOutputStream()
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)

    transferThread?.write(stream.toByteArray(), MessageDataType.TYPE_IMAGE)
  }

  private fun scaleBitmap(bitmap: Bitmap): Bitmap {
    val bitmapWidth = bitmap.width
    val bitmapHeight = bitmap.height
    val ratioSquare: Double = bitmapWidth * bitmapHeight / 500000.0

    if (ratioSquare > 1) {
      val ratio = sqrt(ratioSquare)
      val requiredWidth: Int = round(bitmapWidth / ratio).toInt()
      val requiredHeight: Int = round(bitmapHeight / ratio).toInt()
      return Bitmap.createScaledBitmap(bitmap, requiredWidth, requiredHeight, true)
    }
    return bitmap
  }

  private fun startTransfer(socket: BluetoothSocket) {
    transferThread = DataTransferThread(socket)
    transferThread?.start()
  }

  // region Inner Classes
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
//          Log.d(TAG, "Socket's accept() method failed. ${e.message}")
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
//        Log.d(TAG, "Could not close the connect socket. ${e.message}")
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
//          Log.d(TAG, "Could not close the connect socket. ${e.message}")
        }

      }

    }

    fun cancel() {
      try {
        mmSocket?.close()
      } catch (e: IOException) {
//        Log.d(TAG, "Could not close the connect socket. ${e.message}")
      }
    }
  }

  private inner class DataTransferThread(val mmSocket: BluetoothSocket) : Thread() {
    private val mmInStream = mmSocket.inputStream
    private val mmOutStream = mmSocket.outputStream
    private val mBufferSize = 1024

    override fun run() {
      read()
    }

    fun read() {
      var numBytes: Int
      val buffer = ByteArray(mBufferSize)

      while (true) {
        try {
          if (mmSocket.isConnected) {
            var messageDataType = MessageDataType.TYPE_TEXT
//            Log.i(TAG, "socket is connected, reading...")
            var data = ByteArray(0)
            while (true) {
//              Log.d(TAG, "available: ${mmInStream.available()} Before")
              numBytes = mmInStream.read(buffer)

              val temp = ByteArray(data.size + numBytes)
              temp.mapIndexed { index, _ ->
                temp[index] = if (index < data.size) data[index] else buffer[index - data.size]
              }
              data = temp

//              Log.d(TAG, "received $numBytes bytes.")
              if (buffer.startsWith(IMAGE_HEADER)) {
//                Log.d(TAG, "buffer:\t${buffer.take(10)}")
//                Log.d(TAG, "header:\t${IMAGE_HEADER.take(10)}")
//                Log.d(TAG, "received JPEG HEADER.")
                messageDataType = MessageDataType.TYPE_IMAGE
                continue
              } else {
                if (numBytes >= mBufferSize) {
                  continue
                } else if (messageDataType == MessageDataType.TYPE_IMAGE) {
//                  Log.d(TAG, "data endings:\t${data.takeLast(5)}")
//                  Log.d(TAG, "JPEG TAIL:\t${IMAGE_TAIL.takeLast(5)}")

                  if (data.endsWith(IMAGE_TAIL)) {
//                    Log.d(TAG, "receiving terminated.")
                    break
                  }
                } else if (messageDataType == MessageDataType.TYPE_TEXT) {
//                  Log.d(TAG, "receiving terminated.")
                  break
                }
              }
            }

//            Log.d(TAG, "last: ${data.takeLast(10)}")
//            Log.d(TAG, "ImageSize: ${data.size}")

            val intent = Intent(ACT_MESSAGE_READ)
            intent.putExtra("numBytes", data.size)
            intent.putExtra("buffer", data)
            intent.putExtra(
              "msg_type", messageDataType
            )
            context.sendBroadcast(intent)
          }
        } catch (e: IOException) {
//          Log.d(TAG, "Input stream was disconnected", e)
          context.sendBroadcast(Intent(ACT_CONNECTION_LOST))
          break
        }
      }
    }

    fun write(bytes: ByteArray, messageDataType: MessageDataType) {
//      Log.d(TAG, "ImageSize: ${bytes.size}")

      try {
        if (mmSocket.isConnected) {
//          Log.i(TAG, "socket is connected, writing...")
          mmOutStream.write(bytes)
          val intent = Intent(ACT_MESSAGE_WRITE_SUCCEED)
          intent.putExtra("numBytes", bytes.size)
          intent.putExtra("buffer", bytes)
          intent.putExtra("msg_type", messageDataType)
          context.sendBroadcast(intent)
        }
      } catch (e: IOException) {
//        Log.e(TAG, "Error occurred when sending data", e)
        context.sendBroadcast(Intent(ACT_MESSAGE_WRITE_FAILED))
        context.sendBroadcast(Intent(ACT_CONNECTION_LOST))
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