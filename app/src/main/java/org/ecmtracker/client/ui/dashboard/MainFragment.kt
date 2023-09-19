package org.ecmtracker.client.ui.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.viewModels
import androidx.preference.EditTextPreference
import androidx.preference.EditTextPreferenceDialogFragmentCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.TwoStatePreference
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import dagger.hilt.android.AndroidEntryPoint
import dev.doubledot.doki.ui.DokiActivity
import org.ecmtracker.client.BuildConfig
import org.ecmtracker.client.R
import org.ecmtracker.client.databinding.ActivityFirstBinding
import org.ecmtracker.client.ui.mian.MainActivity
import org.ecmtracker.client.ui.status.StatusActivity
import org.ecmtracker.client.utils.AutostartReceiver
import org.ecmtracker.client.utils.BatteryOptimizationHelper
import org.ecmtracker.client.utils.DummyMainFragment
import org.ecmtracker.client.utils.Resource
import org.ecmtracker.client.utils.TrackingService
import org.ecmtracker.client.utils.alert
import java.util.*


@AndroidEntryPoint
class MainFragment : PreferenceFragmentCompat(), View.OnClickListener {


    private lateinit var alarmManager: AlarmManager
    private lateinit var alarmIntent: PendingIntent
    private var requestingPermissions: Boolean = false
    private var binding: ActivityFirstBinding? = null
    private val viewModel: DashBoardModel by viewModels()

    private fun getProvider(accuracy: String?): String {
        return when (accuracy) {
            "high" -> LocationManager.GPS_PROVIDER
            "low" -> LocationManager.PASSIVE_PROVIDER
            else -> LocationManager.NETWORK_PROVIDER
        }
    }

    var lat = 0.0
    var lng = 0.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivityFirstBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    @SuppressLint("HardwareIds")
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mLocationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Checking GPS is enabled
        val mGPS = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!mGPS) {
            ImageViewCompat.setImageTintList(
                binding?.ivLocation!!,
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gray))
            );
        }

        binding!!.apply {
            btnStop.setOnClickListener(this@MainFragment)
            btnStart.setOnClickListener(this@MainFragment)
            btnStatus.setOnClickListener(this@MainFragment)
            imgLang.setOnClickListener(this@MainFragment)
        }

        if (sharedPreferences.getBoolean(KEY_STATUS, false)) {

            binding!!.btnStart.apply {
                setBackgroundResource(R.drawable.gray_btn_bg)
                isEnabled = false
            }

            binding!!.btnStop.apply {
                setBackgroundResource(R.drawable.blue_btn_bg)
                isEnabled = true
            }

            setDataOnUI()
            freezedField()
            startTrackingService(checkPermission = true, initialPermission = true)

        } else {

            releasedField()
            clearUI()

            binding!!.btnStart.apply {
                setBackgroundResource(R.drawable.blue_btn_bg)
                isEnabled = true
            }

            binding!!.btnStop.apply {
                setBackgroundResource(R.drawable.gray_btn_bg)
                isEnabled = false
            }
        }

    }

    private fun callStopAPI() {
        var isApiCalled = false
        viewModel.getStopTrip(
            "http://54.169.20.116/mte/api/v3/stopTrip/EcMtcr!/${
                sharedPreferences.getString(
                    "id",
                    "AA00CC00DD"
                )
            }/${sharedPreferences.getString("LAT", "0.0")}/${
                sharedPreferences.getString(
                    "LNG",
                    "0.0"
                )
            }/0"
        )

        viewModel.stopTrip.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            when (it.status) {
                Resource.Status.SUCCESS -> {


                    if (!it.data!!.isNullOrEmpty()) {

                        if (it.data[0].statusCode == "0") {
                            clearUI()
                            sharedPreferences.edit().putBoolean(KEY_STATUS, false).apply()
                            releasedField()
                            binding!!.btnStart.apply {
                                setBackgroundResource(R.drawable.blue_btn_bg)
                                isEnabled = true
                            }

                            binding!!.btnStop.apply {
                                setBackgroundResource(R.drawable.gray_btn_bg)
                                isEnabled = false
                            }
                        }
                        val builder = AlertDialog.Builder(requireContext())
                        builder.setMessage(it.data[0].statusMessage!!)
                        builder.setTitle("Alert!")
                        builder.setCancelable(false)

                        builder.setPositiveButton("Okay") { dialog: DialogInterface?, which: Int ->
                            dialog!!.dismiss()
                            stopTrackingService()
                        }

                        if (!isApiCalled) {
                            isApiCalled = true
                            val alertDialog = builder.create()
                            alertDialog.show()
                        }


                    } else {
                        alert("Data not found")
                    }
                }

                Resource.Status.ERROR -> {
                    alert(it.message.toString())
                }

                Resource.Status.LOADING -> {
                    //   alert("Loading")
                }
            }
        })


    }

    private fun callToStartApi() {

        var isApiCalled = false

        binding!!.apply {
            viewModel.getStartTrip(
                "http://54.169.20.116/mte/api/v3/startTrip/EcMtcr!/${
                    sharedPreferences.getString(
                        "id",
                        "AA00CC00DD"
                    )
                }/${edtDriverName.text.toString().trim()}/${
                    edtMobile.text.toString().trim()
                }/${edtVehicleNumber.text.toString().trim()}/${
                    edtDLRNumber.text.toString().trim()
                }/${
                    edtCompanyName.text.toString().trim()
                }/${sharedPreferences.getString("LAT", "0.0")}/${
                    sharedPreferences.getString("LNG", "0.0")
                }/${
                    edtStartingPoint.text.toString().trim()
                }/${edtDestinationPoint.text.toString().trim()}/0"
            )
        }

        viewModel.tartTrip.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    // binding.progressBar.visibility = View.GONE
                    if (!it.data!!.isNullOrEmpty()) {
                        if (it.data[0].statusCode == "0") {

                            storeUserDataInPrf()

                            sharedPreferences.edit().putBoolean(KEY_STATUS, true).apply()
                            sharedPreferences.edit().putString(
                                KEY_URL,
                                it.data[0].values!![0]!!.configuration!!.serverUrl
                            )
                                .apply()
                            sharedPreferences.edit()
                                .putString(
                                    KEY_ACCURACY,
                                    it.data[0].values!![0]!!.configuration!!.locationAccuray
                                ).apply()
                            sharedPreferences.edit()
                                .putString(
                                    KEY_DISTANCE,
                                    it.data[0].values!![0]!!.configuration!!.distance.toString()
                                )
                                .apply()
                            sharedPreferences.edit()
                                .putString(
                                    KEY_ANGLE,
                                    it.data[0].values!![0]!!.configuration!!.angle.toString()
                                ).apply()
                            sharedPreferences.edit()
                                .putBoolean(
                                    KEY_BUFFER,
                                    it.data[0].values!![0]!!.configuration!!.offlineBuffering!!
                                )
                                .apply()
                            sharedPreferences.edit()
                                .putBoolean(
                                    KEY_WAKELOCK,
                                    it.data[0].values!![0]!!.configuration!!.wakeLock!!
                                ).apply()

                            sharedPreferences.edit()
                                .putInt(
                                    KEY_FREQ,
                                    it.data[0].values!![0]!!.configuration!!.frequency!!
                                ).apply()

                            binding!!.btnStart.apply {
                                setBackgroundResource(R.drawable.gray_btn_bg)
                                isEnabled = false
                            }

                            binding!!.btnStop.apply {
                                setBackgroundResource(R.drawable.blue_btn_bg)
                                isEnabled = true
                            }

                            freezedField()
                            startTrackingService(true, true)
                        } else {
                            releasedField()

                            binding!!.btnStart.apply {
                                setBackgroundResource(R.drawable.gray_btn_bg)
                                isEnabled = false
                            }

                            binding!!.btnStop.apply {
                                setBackgroundResource(R.drawable.blue_btn_bg)
                                isEnabled = true
                            }

                            sharedPreferences.edit().putBoolean(KEY_STATUS, true).apply()
                        }

                        if (!isApiCalled) {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Alert!")
                                .setMessage(it.data[0].statusMessage!!)
                                .setPositiveButton(
                                    android.R.string.yes
                                ) { dialog, which ->
                                    isApiCalled = true
                                    dialog.dismiss()
                                    startTrackingService(
                                        checkPermission = true,
                                        initialPermission = true
                                    )
                                }
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show()

                        }


                    } else {
                        alert("Data not found")
                    }
                }

                Resource.Status.ERROR -> {
                    alert(it.message.toString())
                }

                Resource.Status.LOADING -> {
                    //  alert("Loading")
                }

                //   binding.progressBar.visibility = View.VISIBLE

                else -> {}
            }
        })
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        if (BuildConfig.HIDDEN_APP && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            removeLauncherIcon()
        }
        setHasOptionsMenu(true)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        initPreferences()

        findPreference<Preference>(KEY_DEVICE)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                newValue != null && newValue != ""
            }
        findPreference<Preference>(KEY_URL)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                newValue != null && validateServerURL(newValue.toString())
            }
        findPreference<Preference>(KEY_INTERVAL)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                try {
                    newValue != null && (newValue as String).toInt() > 0
                } catch (e: NumberFormatException) {
                    Log.w(TAG, e)
                    false
                }
            }
        val numberValidationListener = Preference.OnPreferenceChangeListener { _, newValue ->
            try {
                newValue != null && (newValue as String).toInt() >= 0
            } catch (e: NumberFormatException) {
                Log.w(TAG, e)
                false
            }
        }
        findPreference<Preference>(KEY_DISTANCE)?.onPreferenceChangeListener =
            numberValidationListener
        findPreference<Preference>(KEY_ANGLE)?.onPreferenceChangeListener = numberValidationListener

        alarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val originalIntent = Intent(activity, AutostartReceiver::class.java)
        originalIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        alarmIntent = PendingIntent.getBroadcast(activity, 0, originalIntent, flags)


    }

    class NumericEditTextPreferenceDialogFragment : EditTextPreferenceDialogFragmentCompat() {

        override fun onBindDialogView(view: View) {
            val editText = view.findViewById<EditText>(android.R.id.edit)
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            super.onBindDialogView(view)
        }

        companion object {
            fun newInstance(key: String?): NumericEditTextPreferenceDialogFragment {
                val fragment = NumericEditTextPreferenceDialogFragment()
                val bundle = Bundle()
                bundle.putString(ARG_KEY, key)
                fragment.arguments = bundle
                return fragment
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (listOf(KEY_INTERVAL, KEY_DISTANCE, KEY_ANGLE).contains(preference.key)) {
            val f: EditTextPreferenceDialogFragmentCompat =
                NumericEditTextPreferenceDialogFragment.newInstance(preference.key)
            f.setTargetFragment(this, 0)
            f.show(requireFragmentManager(), "androidx.preference.PreferenceFragment.DIALOG")
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun removeLauncherIcon() {
        val className =
            MainActivity::class.java.canonicalName!!.replace(".MainActivity", ".Launcher")
        val componentName = ComponentName(requireActivity().packageName, className)
        val packageManager = requireActivity().packageManager
        if (packageManager.getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            val builder = AlertDialog.Builder(requireActivity())
            builder.setIcon(android.R.drawable.ic_dialog_alert)
            builder.setMessage(getString(R.string.hidden_alert))
            builder.setPositiveButton(android.R.string.ok, null)
            builder.show()
        }
    }

    override fun onStart() {
        super.onStart()
        if (requestingPermissions) {
            requestingPermissions = BatteryOptimizationHelper().requestException(requireContext())
        }
    }


    private fun setPreferencesEnabled(enabled: Boolean) {
        findPreference<Preference>(KEY_DEVICE)?.isEnabled = enabled
        findPreference<Preference>(KEY_URL)?.isEnabled = enabled
        findPreference<Preference>(KEY_INTERVAL)?.isEnabled = enabled
        findPreference<Preference>(KEY_DISTANCE)?.isEnabled = enabled
        findPreference<Preference>(KEY_ANGLE)?.isEnabled = enabled
        findPreference<Preference>(KEY_ACCURACY)?.isEnabled = enabled
        findPreference<Preference>(KEY_BUFFER)?.isEnabled = enabled
        findPreference<Preference>(KEY_WAKELOCK)?.isEnabled = enabled
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.status) {
            startActivity(Intent(activity, StatusActivity::class.java))
            return true
        } else if (item.itemId == R.id.info) {
            DokiActivity.start(requireContext())
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initPreferences() {
        PreferenceManager.setDefaultValues(requireActivity(), R.xml.preferences, false)
        if (!sharedPreferences.contains(KEY_DEVICE)) {
            val id = (Random().nextInt(900000) + 100000).toString()
            sharedPreferences.edit().putString(KEY_DEVICE, id).apply()
            findPreference<EditTextPreference>(KEY_DEVICE)?.text = id
        }
        findPreference<Preference>(KEY_DEVICE)?.summary =
            sharedPreferences.getString(KEY_DEVICE, null)
    }

    private fun showBackgroundLocationDialog(
        context: Context,
        onSuccess: (allow: Boolean) -> Unit
    ) {
        val builder = AlertDialog.Builder(context)
        val option = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.backgroundPermissionOptionLabel
        } else {
            context.getString(R.string.request_background_option)
        }
        builder.setMessage(context.getString(R.string.request_background, option))
        builder.setPositiveButton(android.R.string.ok) { _, _ -> onSuccess(true) }
        builder.setNegativeButton(android.R.string.cancel) { _, _ -> onSuccess(false) }
        builder.show()
    }

    private fun startTrackingService(checkPermission: Boolean, initialPermission: Boolean) {
        var permission = initialPermission

        if (permission) {
            setPreferencesEnabled(false)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                alarmManager.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    ALARM_MANAGER_INTERVAL.toLong(),
                    ALARM_MANAGER_INTERVAL.toLong(),
                    alarmIntent
                )
            }

            ContextCompat.startForegroundService(
                requireContext(),
                Intent(activity, TrackingService::class.java)
            )

            requestingPermissions = BatteryOptimizationHelper().requestException(requireContext())
            ContextCompat.startForegroundService(
                requireContext(),
                Intent(activity, TrackingService::class.java)
            )

        } else {
            sharedPreferences.edit().putBoolean(KEY_STATUS, false).apply()
            val preference = findPreference<TwoStatePreference>(KEY_STATUS)
            preference?.isChecked = false
        }
    }

    private fun stopTrackingService() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            alarmManager.cancel(alarmIntent)
        }
        requireActivity().stopService(Intent(activity, TrackingService::class.java))
        setPreferencesEnabled(true)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {


        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            var granted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false
                    break
                }
            }
            startTrackingService(false, granted)
        }

        if (requestCode == PERMISSIONS_REQUEST_BACKGROUND_LOCATION) {
            var granted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false
                    break
                }
            }
            if (granted) {
                getLatLng()
                callToStartApi()
            }
        }


        if (requestCode == 100) {
            val locationManager =
                requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val provider =
                getProvider(sharedPreferences.getString(MainFragment.KEY_ACCURACY, "medium"))

            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            val location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            if (location != null) {
                lat = location.latitude
                lng = location.longitude
                Log.e(TAG, "onViewCreated: ")
            } else {
                locationManager.requestSingleUpdate(provider, object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        lat = location.latitude
                        lng = location.longitude
                    }

                    override fun onStatusChanged(
                        provider: String,
                        status: Int,
                        extras: Bundle
                    ) {
                    }

                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }, Looper.myLooper())
            }
        }
    }

    private fun validateServerURL(userUrl: String): Boolean {
        val port = Uri.parse(userUrl).port
        if (
            URLUtil.isValidUrl(userUrl) &&
            (port == -1 || port in 1..65535) &&
            (URLUtil.isHttpUrl(userUrl) || URLUtil.isHttpsUrl(userUrl))
        ) {
            return true
        }
        Toast.makeText(activity, R.string.error_msg_invalid_url, Toast.LENGTH_LONG).show()
        return false
    }


    private fun alertDialog(message: String, isStartTrip: Boolean) {

    }

    fun freezedField() {
        binding!!.apply {
            chkAccep1t.isEnabled = false
            edtDriverName.isEnabled = false
            edtDLRNumber.isEnabled = false
            edtVehicleNumber.isEnabled = false
            edtStartingPoint.isEnabled = false
            edtDestinationPoint.isEnabled = false
            edtMobile.isEnabled = false
            edtCompanyName.isEnabled = false
            imgLang.isEnabled = false

        }
    }

    fun releasedField() {

        binding!!.apply {
            chkAccep1t.isEnabled = true
            edtDriverName.isEnabled = true
            edtDLRNumber.isEnabled = true
            edtVehicleNumber.isEnabled = true
            edtStartingPoint.isEnabled = true
            edtDestinationPoint.isEnabled = true
            edtMobile.isEnabled = true
            edtCompanyName.isEnabled = true
            imgLang.isEnabled = true
        }
    }

    fun validateFields(): Boolean {

        binding!!.apply {

            if (!chkAccep1t.isChecked) {
                alert("Please accept Terms And Conditions")
                return false
            } else if (TextUtils.isEmpty(edtDriverName.text.toString())) {
                alert("Please enter driver name")
                return false
            } else if (TextUtils.isEmpty(edtDLRNumber.text.toString())) {
                alert("Please enter LR number")
                return false
            } else if (TextUtils.isEmpty(edtVehicleNumber.text.toString())) {
                alert("Please enter vehicle number")
                return false
            } else if (TextUtils.isEmpty(edtStartingPoint.text.toString())) {
                alert("Please enter starting point")
                return false
            } else if (TextUtils.isEmpty(edtDestinationPoint.text.toString())) {
                alert("Please enter destination point")
                return false
            } else if (TextUtils.isEmpty(edtMobile.text.toString())) {
                alert("Please enter mobile number")
                return false
            } else if (edtMobile.text.toString().length < 10) {
                alert("Please enter valid mobile number")
                return false
            } else if (TextUtils.isEmpty(edtCompanyName.text.toString())) {
                alert("Please enter company name")
                return false

            }
        }

        return true
    }

    override fun onClick(v: View?) {
        when (v?.id) {

            R.id.btnStop -> {
                val builder = AlertDialog.Builder(requireContext())
                builder.setMessage("Are you sure you want to clear all data?")
                builder.setTitle("Alert!")
                builder.setCancelable(false)

                builder.setPositiveButton("Yes") { dialog: DialogInterface?, which: Int ->
                    dialog!!.dismiss()
                    getLatLng()
                    callStopAPI()
                }

                builder.setNegativeButton("No") { dialog: DialogInterface, which: Int ->
                    dialog.dismiss()
                }

                val alertDialog = builder.create()
                alertDialog.show()
            }

            R.id.btnStart -> {
                if (!(sharedPreferences.getBoolean(KEY_STATUS, false))) {
                    if (validateFields()) {

                        val mLocationManager =
                            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

                        // Checking GPS is enabled
                        val mGPS = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

                        if (mGPS) {
                            checkPermission()
                        } else {
                            val builder = AlertDialog.Builder(requireContext())
                            builder.setMessage("Your Location is disable, Please Enable you GPS Location")
                            builder.setTitle("Alert!")
                            builder.setCancelable(false)

                            builder.setPositiveButton("Okay") { dialog: DialogInterface?, which: Int ->
                                dialog!!.dismiss()
                            }

                            val alertDialog = builder.create()
                            alertDialog.show()
                        }

                    }
                }

            }

            R.id.imgLang -> {
                val resources = resources
                val dm = resources.displayMetrics
                val config = resources.configuration

                val builder = AlertDialog.Builder(requireContext())
                builder.setMessage("Select language")
                builder.setTitle("Language")
                builder.setCancelable(false)

                builder.setPositiveButton("Hindi") { dialog: DialogInterface?, which: Int ->
                    config.setLocale(Locale("hi"))
                    resources.updateConfiguration(config, dm)
                    sharedPreferences.edit().putString(KEY_LANGUAGE, "hi").apply()
                    requireActivity().finish()
                    requireContext().startActivity(
                        Intent(
                            requireActivity(),
                            MainActivity::class.java
                        )
                    )
                }

                builder.setNegativeButton("English") { dialog: DialogInterface, which: Int ->
                    config.setLocale(Locale("en"))
                    resources.updateConfiguration(config, dm)
                    sharedPreferences.edit().putString(KEY_LANGUAGE, "en").apply()
                    requireActivity().finish()
                    requireContext().startActivity(
                        Intent(
                            requireActivity(),
                            MainActivity::class.java
                        )
                    )
                }

                val alertDialog = builder.create()
                alertDialog.show()
            }


            R.id.btnStatus -> {
                startActivity(Intent(activity, StatusActivity::class.java))
            }

        }
    }

    private fun storeUserDataInPrf() {
        binding!!.apply {
            sharedPreferences.edit().putString(KEY_DRIVER_NAME, edtDriverName.text.toString())
                .apply()
            sharedPreferences.edit().putString(KEY_LR_NUMBER, edtDLRNumber.text.toString()).apply()
            sharedPreferences.edit().putString(KEY_VEHICLE_NUMBER, edtVehicleNumber.text.toString())
                .apply()
            sharedPreferences.edit().putString(KEY_STARTING_POINT, edtStartingPoint.text.toString())
                .apply()
            sharedPreferences.edit().putString(KEY_DEST_POINT, edtDestinationPoint.text.toString())
                .apply()
            sharedPreferences.edit().putString(KEY_MOBILE, edtMobile.text.toString())
                .apply()
            sharedPreferences.edit().putString(KEY_COMPANY_NAME, edtCompanyName.text.toString())
                .apply()

        }

    }

    private fun setDataOnUI() {
        binding!!.apply {
            edtDriverName.setText(sharedPreferences.getString(KEY_DRIVER_NAME, ""))
            edtDLRNumber.setText(sharedPreferences.getString(KEY_LR_NUMBER, ""))
            edtVehicleNumber.setText(sharedPreferences.getString(KEY_VEHICLE_NUMBER, ""))
            edtStartingPoint.setText(sharedPreferences.getString(KEY_STARTING_POINT, ""))
            edtDestinationPoint.setText(sharedPreferences.getString(KEY_DEST_POINT, ""))
            edtMobile.setText(sharedPreferences.getString(KEY_MOBILE, ""))
            edtCompanyName.setText(sharedPreferences.getString(KEY_COMPANY_NAME, ""))
            chkAccep1t.isChecked = true
        }
    }

    private fun clearUI() {
        binding!!.apply {
            edtDriverName.setText("")
            edtDLRNumber.setText("")
            edtVehicleNumber.setText("")
            edtStartingPoint.setText("")
            edtDestinationPoint.setText("")
            edtMobile.setText("")
            edtCompanyName.setText("")
            chkAccep1t.isChecked = false
        }

        sharedPreferences.edit().putString(KEY_DRIVER_NAME, "").apply()
        sharedPreferences.edit().putString(KEY_LR_NUMBER, "").apply()
        sharedPreferences.edit().putString(KEY_VEHICLE_NUMBER, "").apply()
        sharedPreferences.edit().putString(KEY_STARTING_POINT, "").apply()
        sharedPreferences.edit().putString(KEY_DEST_POINT, "").apply()
        sharedPreferences.edit().putString(KEY_MOBILE, "").apply()
        sharedPreferences.edit().putString(KEY_COMPANY_NAME, "").apply()


    }

    companion object {
        private val TAG = DummyMainFragment::class.java.simpleName
        private const val ALARM_MANAGER_INTERVAL = 15000
        lateinit var sharedPreferences: SharedPreferences
        const val KEY_DEVICE = "id"
        const val KEY_URL = "url"
        const val KEY_INTERVAL = "interval"
        const val KEY_DISTANCE = "distance"
        const val KEY_ANGLE = "angle"
        const val KEY_ACCURACY = "accuracy"
        const val KEY_STATUS = "status"
        const val KEY_BUFFER = "buffer"
        const val KEY_WAKELOCK = "wakelock"
        const val KEY_LANGUAGE = "language"
        const val KEY_FREQ = "frequency"

        const val KEY_DRIVER_NAME = "driver_name"
        const val KEY_LR_NUMBER = "lr_number"
        const val KEY_VEHICLE_NUMBER = "vehicle_number"
        const val KEY_STARTING_POINT = "starting_point"
        const val KEY_DEST_POINT = "dest_point"
        const val KEY_MOBILE = "mobile"
        const val KEY_COMPANY_NAME = "company_name"

        private const val PERMISSIONS_REQUEST_LOCATION = 2
        private const val PERMISSIONS_REQUEST_BACKGROUND_LOCATION = 3
    }

    fun getLatLng() {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = getProvider(sharedPreferences.getString(MainFragment.KEY_ACCURACY, "medium"))

        try {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                val requiredPermissions: MutableSet<String> = HashSet()

                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(
                        requiredPermissions.toTypedArray(),
                        100
                    )
                }

            } else {
                Log.e(TAG, "onViewCreated: ")
            }

            val location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            if (location != null) {

                lat = location.latitude
                lng = location.longitude

                sharedPreferences.edit().putString("LAT", location.latitude.toString()).apply()
                sharedPreferences.edit().putString("LNG", location.longitude.toString()).apply()

            } else {

                locationManager.requestSingleUpdate(provider, object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        lat = location.latitude
                        lng = location.longitude

                        sharedPreferences.edit().putString("LAT", location.latitude.toString())
                            .apply()
                        sharedPreferences.edit().putString("LNG", location.longitude.toString())
                            .apply()
                    }

                    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
                    }

                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }, Looper.myLooper())
            }
        } catch (e: RuntimeException) {
            Log.e(TAG, "onViewCreated: ")
        }

    }

    fun checkPermission() {
        Dexter.withContext(requireContext())
            .withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.isAnyPermissionPermanentlyDenied) {
                        requestPermissionDialog()
                    }

                    if (report.areAllPermissionsGranted()) {
                        if (sharedPreferences.getBoolean("IS_PERMISSION_ALLOW", false)) {
                            getLatLng()
                            callToStartApi()
                        } else {
                            if (ActivityCompat.checkSelfPermission(
                                    requireContext(),
                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    requestPermissionDialog()
                                }, 1000)
                            } else {
                                sharedPreferences.edit().putBoolean("IS_PERMISSION_ALLOW", true)
                                    .apply()
                            }
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest?>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }).check()
    }

    fun requestPermissionDialog() {

        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage("Required location permission to start trip. Please allow All The Time permission")
        builder.setTitle("Alert!")
        builder.setCancelable(false)

        builder.setPositiveButton("Okay") { dialog: DialogInterface?, which: Int ->

            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", requireContext().packageName, null)
            intent.data = uri
            startActivityForResult(intent, 101)
            dialog!!.dismiss()

        }

        builder.setNegativeButton("Cancel") { dialog: DialogInterface?, which: Int ->
            dialog?.dismiss()
        }


        val alertDialog = builder.create()
        alertDialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionDialog()
            } else {
                sharedPreferences.edit().putBoolean("IS_PERMISSION_ALLOW", true).apply()
            }
        }
    }
}