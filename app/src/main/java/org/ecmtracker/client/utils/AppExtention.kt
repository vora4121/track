package org.ecmtracker.client.utils

import android.widget.Toast
import androidx.fragment.app.Fragment

fun Fragment.alert(message: String){
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
}