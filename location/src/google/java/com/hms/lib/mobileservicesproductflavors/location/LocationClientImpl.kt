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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.IntentSender
import android.os.Looper
import android.util.Log
import androidx.lifecycle.Lifecycle
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.hms.lib.mobileservicesproductflavors.location.Constants.CURRENT_LOCATION_REMOVE_FAIL
import com.hms.lib.mobileservicesproductflavors.location.Constants.CURRENT_LOCATION_REMOVE_SUCCESS
import com.hms.lib.mobileservicesproductflavors.location.Constants.OPEN_LOCATION_SETTING_REQUEST_CODE
import com.hms.lib.mobileservicesproductflavors.location.model.CheckGpsEnabledResult
import com.hms.lib.mobileservicesproductflavors.location.model.LocationResult
import com.hms.lib.mobileservicesproductflavors.location.model.LocationResultState
import com.hms.lib.mobileservicesproductflavors.location.model.Priority
import com.livinglifetechway.quickpermissions_kotlin.runWithPermissions

class LocationClientImpl(
    private val activity: Activity,
    lifecycle: Lifecycle,
    needBackgroundPermissions: Boolean = false
) : LocationClient(activity, lifecycle, needBackgroundPermissions) {

    companion object {
        private const val TAG = "LocationClientImpl"
    }

    private var fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(activity)

    var locationCallback: LocationCallback? = null

    override fun checkLocationSettings(
        activity: Activity,
        callback: (checkGpsEnabledResult: CheckGpsEnabledResult, error: Exception?) -> Unit
    ) {
        val locationRequest = LocationRequest.create()
        locationRequest.interval = 100000
        locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(activity)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            callback.invoke(CheckGpsEnabledResult.ENABLED, null)
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(
                        activity,
                        OPEN_LOCATION_SETTING_REQUEST_CODE
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            } else callback.invoke(CheckGpsEnabledResult.ERROR, exception)
        }
    }

    @SuppressLint("MissingPermission")
    override fun getLastKnownLocationCore(locationListener: (commonLocationResult: LocationResult) -> Unit) {
        Log.i(TAG, "getLastKnownLocationCore is called from gms")
        activity.runWithPermissions(*getLocationPermissions(), options = options) {
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
    }

    @SuppressLint("MissingPermission")
    override fun requestLocationUpdatesCore(
        priority: Priority?,
        interval: Long?,
        locationListener: (commonLocationResult: LocationResult) -> Unit
    ) {
        Log.i(TAG, "requestLocationUpdatesCore is called from gms")
        val locationRequest = LocationRequest.create().apply {
            this.interval = interval ?: 100000
            this.priority = when (priority) {
                Priority.PRIORITY_BALANCED_POWER_ACCURACY -> LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
                Priority.PRIORITY_HIGH_ACCURACY -> LocationRequest.PRIORITY_HIGH_ACCURACY
                Priority.PRIORITY_LOW_POWER -> LocationRequest.PRIORITY_LOW_POWER
                Priority.PRIORITY_NO_POWER -> LocationRequest.PRIORITY_NO_POWER
                else -> LocationRequest.PRIORITY_HIGH_ACCURACY
            }
        }

        if (locationCallback == null) {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult?) {
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

                override fun onLocationAvailability(p0: LocationAvailability?) {
                    super.onLocationAvailability(p0)
                    p0?.let {
                        if (!it.isLocationAvailable) {
                            if (!isLocationEnabled()) locationListener.invoke(
                                LocationResult(
                                    null,
                                    LocationResultState.GPS_DISABLED, Exception("User disabled gps")
                                )
                            )
                            else locationListener.invoke(
                                LocationResult(
                                    null, LocationResultState.LOCATION_UNAVAILABLE,
                                    Exception("location unavailable")
                                )
                            )
                        }
                    }
                }
            }
        }
        activity.runWithPermissions(*getLocationPermissions(), options = options) {
            Log.i(TAG, "requestLocationUpdatesCore is called from gms")
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, locationCallback,
                Looper.getMainLooper()
            ).addOnFailureListener { err ->
                locationListener.invoke(
                    LocationResult(null, LocationResultState.FAIL, err)
                )
            }
        }
    }


    override fun removeLocationUpdates() {
        Log.i(TAG, "removeLocationUpdates is called from gms")
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

}