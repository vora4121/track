package org.ecmtracker.client.data.remote

import org.ecmtracker.client.data.entities.StartTrackItem
import org.ecmtracker.client.data.entities.StopTripResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface RemoteService {
    @GET
    suspend fun startTrip(@Url startTripUrl : String): Response<List<StartTrackItem>>
    
    @GET
    suspend fun stopTrip(@Url stopTripUrl : String): Response<List<StopTripResponse>>
}