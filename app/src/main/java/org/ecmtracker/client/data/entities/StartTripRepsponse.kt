package org.ecmtracker.client.data.entities

import com.google.gson.annotations.SerializedName


data class ValuesItem(

	@field:SerializedName("tripDetails")
	val tripDetails: TripDetails? = null,

	@field:SerializedName("configuration")
	val configuration: Configuration? = null
)

data class Configuration(

	@field:SerializedName("locationAccuray")
	val locationAccuray: String? = null,

	@field:SerializedName("OfflineBuffering")
	val offlineBuffering: Boolean? = null,

	@field:SerializedName("WakeLock")
	val wakeLock: Boolean? = null,

	@field:SerializedName("Angle")
	val angle: Int? = null,

	@field:SerializedName("serverUrl")
	val serverUrl: String? = null,

	@field:SerializedName("Distance")
	val distance: Int? = null,

	@field:SerializedName("frequency")
	val frequency: Int? = null
)

data class StartTrackItem(

	@field:SerializedName("values")
	val values: List<ValuesItem?>? = null,

	@field:SerializedName("statusMessage")
	val statusMessage: String? = null,

	@field:SerializedName("statusCode")
	val statusCode: String? = null
)

data class TripDetails(

	@field:SerializedName("startLocation")
	val startLocation: String? = null,

	@field:SerializedName("companyName")
	val companyName: String? = null,

	@field:SerializedName("vehicleNumber")
	val vehicleNumber: String? = null,

	@field:SerializedName("driverMobile")
	val driverMobile: String? = null,

	@field:SerializedName("driverName")
	val driverName: String? = null,

	@field:SerializedName("dlrNumber")
	val dlrNumber: String? = null,

	@field:SerializedName("endLocation")
	val endLocation: String? = null
)
