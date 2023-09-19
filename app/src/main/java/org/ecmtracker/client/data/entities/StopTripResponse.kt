package org.ecmtracker.client.data.entities

import com.google.gson.annotations.SerializedName

data class StopTripResponse(

	@field:SerializedName("statusMessage")
	val statusMessage: String? = null,

	@field:SerializedName("statusCode")
	val statusCode: String? = null
)
