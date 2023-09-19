package org.ecmtracker.client.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import org.ecmtracker.client.data.entities.StartTrackItem
import org.ecmtracker.client.data.entities.StopTripResponse
import org.ecmtracker.client.data.repository.RemoteRepository
import org.ecmtracker.client.utils.Resource
import javax.inject.Inject

@HiltViewModel
class DashBoardModel @Inject constructor(
    private val repository: RemoteRepository
) : ViewModel() {

    private val startTripURL = MutableLiveData<String>()
    private val StopTripURL = MutableLiveData<String>()

    private val startTripResponse = startTripURL.switchMap { id ->
        repository.startTrip(id)
    }

    private val stopTripResponse = StopTripURL.switchMap { id ->
        repository.stopTrip(id)
    }

    val tartTrip: LiveData<Resource<List<StartTrackItem>>> = startTripResponse

    val stopTrip: LiveData<Resource<List<StopTripResponse>>> = stopTripResponse

    fun getStartTrip(since: String) {
        startTripURL.value = since
    }

    fun getStopTrip(since: String) {
        StopTripURL.value = since
    }

}