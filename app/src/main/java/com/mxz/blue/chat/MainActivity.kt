package com.mxz.blue.chat

import android.Manifest
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import com.mxz.blue.chat.databinding.ActivityMainBinding

const val REQUEST_LOCATION_PERMISSION = 544

class MainActivity : AppCompatActivity() {

  // region Properties

  private lateinit var appBarConfiguration: AppBarConfiguration
  private lateinit var binding: ActivityMainBinding
  private lateinit var snackbar: Snackbar

  // endregion

  // region Overrides

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setSupportActionBar(binding.toolbar)

    val navController = findNavController(R.id.nav_host_fragment_content_main)
    appBarConfiguration = AppBarConfiguration(navController.graph)
    setupActionBarWithNavController(navController, appBarConfiguration)

    snackbar = Snackbar.make(binding.root, "", Snackbar.LENGTH_LONG)

    enableBluetooth()

    requestPermissions(
      arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
      ), REQUEST_LOCATION_PERMISSION
    )

  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    return when (item.itemId) {
      R.id.action_settings -> true
      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onSupportNavigateUp(): Boolean {
    val navController = findNavController(R.id.nav_host_fragment_content_main)
    return navController.navigateUp(appBarConfiguration)
        || super.onSupportNavigateUp()
  }

  // endregion

  // region Methods
  private fun enableBluetooth() {
//    updateStatus("Turning On Bluetooth", Snackbar.LENGTH_SHORT)
    val chatBiz = ChatBiz.getInstance(this, this)
    chatBiz.enableBluetooth()
  }

  fun updateStatus(status: String) {
    updateStatus(status, Snackbar.LENGTH_INDEFINITE)
  }

  fun updateStatus(status: String, snackBarLength: Int) {
    snackbar = Snackbar.make(binding.root, status, snackBarLength)
    snackbar.show()
  }

  fun dismissStatus() {
    snackbar.dismiss()
  }

  // endregion
}