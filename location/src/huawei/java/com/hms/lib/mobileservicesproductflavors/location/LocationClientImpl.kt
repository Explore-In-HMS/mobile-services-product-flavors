// Copyright 2020. Explore in HMS. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at

// http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.hms.lib.mobileservicesproductflavors.location

import android.app.Activity
import android.os.Looper
import android.util.Log
import androidx.lifecycle.Lifecycle
import com.hms.lib.mobileservicesproductflavors.location.Constants.CURRENT_LOCATION_REMOVE_FAIL
import com.hms.lib.mobileservicesproductflavors.location.Constants.CURRENT_LOCATION_REMOVE_SUCCESS
import com.hms.lib.mobileservicesproductflavors.location.Constants.OPEN_LOCATION_SETTING_REQUEST_CODE
import com.hms.lib.mobileservicesproductflavors.location.model.CheckGpsEnabledResult
import com.hms.lib.mobileservicesproductflavors.location.model.LocationResult
import com.hms.lib.mobileservicesproductflavors.location.model.LocationResultState
import com.hms.lib.mobileservicesproductflavors.location.model.Priority
import com.huawei.hms.common.ApiException
import com.huawei.hms.common.ResolvableApiException
import com.huawei.hms.location.*

class LocationClientImpl(
    activity: Activity,
    lifecycle: Lifecycle,
    needBackgroundPermissions: Boolean = false
) :
    LocationClient(activity, lifecycle, needBackgroundPermissions) {

    companion object {
        private const val TAG = "LocationClientImpl"
    }

    private var fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(activity)

    private var locationCallback: LocationCallback? = null

    override fun getLastKnownLocationCore(locationListener: (commonLocationResult: LocationResult) -> Unit) {
        Log.i(TAG, "getLastKnownLocationCore is called from hms")
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                locationListener.invoke(LocationResult(location))
            } ?: kotlin.run {
                locationListener.invoke(
                    LocationResult(
                        null, LocationResultState.NO_LAST_LOCATION,
                        Exception("no last known location")
                    )
                )
            }
        }.addOnFailureListener { err ->
            locationListener.invoke(
                LocationResult(null, LocationResultState.FAIL, err)
            )
        }
    }

    override fun requestLocationUpdatesCore(
        priority: Priority?,
        interval: Long?,
        locationListener: (commonLocationResult: LocationResult) -> Unit
    ) {
        Log.i(TAG, "requestLocationUpdatesCore is called from hms")
        val locationRequest = LocationRequest()
        locationRequest.interval = interval ?: 100000
        locationRequest.priority = when (priority) {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY -> LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            Priority.PRIORITY_HIGH_ACCURACY -> LocationRequest.PRIORITY_HIGH_ACCURACY
            Priority.PRIORITY_LOW_POWER -> LocationRequest.PRIORITY_LOW_POWER
            Priority.PRIORITY_NO_POWER -> LocationRequest.PRIORITY_NO_POWER
            else -> LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (locationCallback == null) {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: com.huawei.hms.location.LocationResult?) {
                    locationResult?.let {
                        locationListener.invoke(LocationResult(it.lastLocation))
                    } ?: kotlin.run {
                        locationListener.invoke(
                            LocationResult(
                                null, LocationResultState.FAIL,
                                Exception("null location")
                            )
                        )
                    }

                }
            }
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest, locationCallback,
            Looper.getMainLooper()
        ).addOnFailureListener { err ->
            locationListener.invoke(
                LocationResult(null, LocationResultState.FAIL, err)
            )
        }
    }

    override fun removeLocationUpdates() {
        Log.i(TAG, "removeLocationUpdates is called from hms")
        locationCallback?.let {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
                .addOnSuccessListener {
                    locationCallback = null
                    Log.i(
                        Constants.TAG,
                        CURRENT_LOCATION_REMOVE_SUCCESS
                    )
                }.addOnFailureListener { err ->
                    Log.i(
                        Constants.TAG,
                        CURRENT_LOCATION_REMOVE_FAIL + err.message
                    )
                }
        }
    }

    override fun checkLocationSettings(
        activity: Activity,
        callback: (checkGpsEnabledResult: CheckGpsEnabledResult, error: Exception?) -> Unit
    ) {
        Log.i(TAG, "checkLocationSettings is called from hms")
        val settingsClient = LocationServices.getSettingsClient(activity)
        val locationSettingRequest = LocationSettingsRequest.Builder().apply {
            addLocationRequest(LocationRequest())
        }.build()
        settingsClient.checkLocationSettings(locationSettingRequest).addOnSuccessListener {
            callback.invoke(CheckGpsEnabledResult.ENABLED, null)
        }.addOnFailureListener {
            (it as? ApiException)?.let {
                if (it.statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    (it as ResolvableApiException).startResolutionForResult(
                        activity,
                        OPEN_LOCATION_SETTING_REQUEST_CODE
                    )
                } else {
                    callback.invoke(CheckGpsEnabledResult.ERROR, it)
                }
            } ?: kotlin.run {
                callback.invoke(CheckGpsEnabledResult.ERROR, it)
            }
        }
    }
}