package org.ecmtracker.client.ui.permission

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import org.ecmtracker.client.R
import org.ecmtracker.client.databinding.ActivityPermissionBinding
import org.ecmtracker.client.ui.mian.MainActivity

class PermissionActivity : AppCompatActivity() {

    var pref: SharedPreferences? = null
    var editor: SharedPreferences.Editor? = null
    private val binding: ActivityPermissionBinding by lazy {
        ActivityPermissionBinding.inflate(
            layoutInflater
        )
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        pref = PreferenceManager.getDefaultSharedPreferences(this)
        editor = pref!!.edit()

        if (pref!!.getBoolean("IS_PERMISSION_ALLOW", false)) {
            getLocation()
        }

        binding.btnAllowPermission.setOnClickListener {
            if (pref!!.getBoolean("IS_PERMISSION_ALLOW", false)) {
                getLocation()
            } else {
                val requiredPermissions: MutableSet<String> = HashSet()
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(requiredPermissions.toTypedArray(), 100)
                    }
                } else {

                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        locationPermission()
                    }else{
                        editor!!.putBoolean("IS_PERMISSION_ALLOW", true).apply()
                    }

                }
            }
        }
    }

/*
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onResume() {
        super.onResume()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            val requiredPermissions: MutableSet<String> = HashSet()
            requiredPermissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            requestPermissions(requiredPermissions.toTypedArray(), 3)
        }else{
            editor!!.putBoolean("IS_PERMISSION_ALLOW", true).apply()
            getLocation()
        }

    }
*/


    private fun getLocation() {

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                val requiredPermissions: MutableSet<String> = HashSet()

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(requiredPermissions.toTypedArray(), 100)
                }
            }

            val location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

            if (location != null) {
                editor!!.putString("LAT", location.latitude.toString()).apply()
                editor!!.putString("LNG", location.longitude.toString()).apply()
                startActivity(Intent(this@PermissionActivity, MainActivity::class.java))
                finish()
            }

        } catch (e: RuntimeException) {

        }

    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)


        if (requestCode == 3) {
            var granted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false
                    break
                }
            }

            if (granted) {

                val locationManager = this@PermissionActivity.getSystemService(Context.LOCATION_SERVICE) as LocationManager

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return
                }

                val location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

                editor!!.putBoolean("IS_PERMISSION_ALLOW", true).apply()

                if (location != null) {
                    editor!!.putString("LAT", location.latitude.toString()).apply()
                    editor!!.putString("LNG", location.longitude.toString()).apply()
                    startActivity(Intent(this@PermissionActivity, MainActivity::class.java))
                    finish()
                }

            } else {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("Need to detect location continuously. Do you want to allow or Quite application? ")
                builder.setTitle("Alert!")
                builder.setCancelable(false)

                builder.setPositiveButton("Allow") { dialog: DialogInterface?, which: Int ->
                    dialog!!.dismiss()
                    locationPermission()
                }

                builder.setNegativeButton("Quite") { dialog: DialogInterface?, which: Int ->
                    editor!!.putBoolean("IS_PERMISSION_ALLOW", false).apply()
                    dialog!!.dismiss()
                    finishAffinity()
                }

                val alertDialog = builder.create()
                alertDialog.show()
            }
        }

        if (requestCode == 100) {

            var granted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false
                    break
                }
            }

            if (granted) {

                val builder = AlertDialog.Builder(this)
                val option = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    this.packageManager.backgroundPermissionOptionLabel
                } else {
                    this.getString(R.string.request_background_option)
                }

                builder.setMessage(this.getString(R.string.request_background, option))

                builder.setPositiveButton(android.R.string.ok) { dialog: DialogInterface?, which: Int ->
                    val requiredPermissions: MutableSet<String> = HashSet()
                    requiredPermissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    requestPermissions(requiredPermissions.toTypedArray(), 3)
                }

                builder.setNegativeButton(android.R.string.cancel) { dialog: DialogInterface?, which: Int ->
                    editor!!.putBoolean("IS_PERMISSION_ALLOW", false).apply()
                    dialog!!.dismiss()
                }

                builder.show()


            } else {
                Toast.makeText(
                    this@PermissionActivity,
                    "Please allow permission",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }
    }

    fun locationPermission(){
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }


}