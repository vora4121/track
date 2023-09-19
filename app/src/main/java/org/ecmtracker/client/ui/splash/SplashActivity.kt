package org.ecmtracker.client.ui.splash

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import org.ecmtracker.client.R
import org.ecmtracker.client.ui.mian.MainActivity
import org.ecmtracker.client.ui.permission.PermissionActivity
import java.util.Locale

class SplashActivity : AppCompatActivity() {

    var pref: SharedPreferences? = null
    var editor: SharedPreferences.Editor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        var android_id = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        pref = PreferenceManager.getDefaultSharedPreferences(this)
        editor = pref!!.edit()

        val resources = resources
        val config = resources.configuration
        editor!!.putString("id", android_id).apply()

        Handler(Looper.getMainLooper()).postDelayed(
            {
                if (pref!!.getString("language", "en").equals("en")) {
                    config.setLocale(Locale("en"))
                } else if (pref!!.getString("language", "en").equals("hi")) {
                    config.setLocale(Locale("hi"))
                }

                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()

                /*    val mIntent =Intent(this@SplashActivity, PermissionActivity::class.java)
                    startActivity(mIntent)
                    finish()*/
            }, 2000
        )

    }
}