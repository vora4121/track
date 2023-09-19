package org.ecmtracker.client.data.repository

import org.ecmtracker.client.data.remote.RemoteDataSource
import org.ecmtracker.client.utils.performGetOperation
import javax.inject.Inject

class RemoteRepository @Inject constructor(
    private val remoteDataSource: RemoteDataSource
) {

    fun startTrip(url : String) =
        performGetOperation(networkCall = { remoteDataSource.startTrip(url) })

    fun stopTrip(url : String) =
        performGetOperation(networkCall = { remoteDataSource.stopTrip(url) })
}