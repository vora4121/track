package org.ecmtracker.client.data.remote

import javax.inject.Inject

class RemoteDataSource @Inject constructor(
    private val remoteService: RemoteService
) : BaseDataSource() {

    suspend fun startTrip(url : String) = getResult {
        remoteService.startTrip(url)
    }

    suspend fun stopTrip(url : String) = getResult {
        remoteService.stopTrip(url)
    }

}