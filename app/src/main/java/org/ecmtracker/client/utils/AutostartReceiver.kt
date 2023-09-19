/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ecmtracker.client.utils

import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import org.ecmtracker.client.ui.dashboard.MainFragment

class AutostartReceiver : WakefulBroadcastReceiver() {

    @Suppress("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        if (sharedPreferences.getBoolean(MainFragment.KEY_STATUS, false)) {
            startWakefulForegroundService(context, Intent(context, TrackingService::class.java))
        }
    }

}
