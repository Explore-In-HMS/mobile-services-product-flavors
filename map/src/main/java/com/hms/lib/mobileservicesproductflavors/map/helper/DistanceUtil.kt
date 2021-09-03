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

package com.hms.lib.mobileservicesproductflavors.map.helper


import android.location.Location
import com.hms.lib.mobileservicesproductflavors.map.model.CommonLatLng

class DistanceUtil {
    // calculates distance between 2 points in meters
    fun calculateDistance(p1: CommonLatLng, p2: CommonLatLng): Double {
        val poi1 = Location("Poi1")
        val poi2 = Location("Poi2")

        poi1.latitude = p1.lat
        poi1.longitude = p1.lng

        poi2.latitude = p2.lat
        poi2.longitude = p2.lng

        return poi1.distanceTo(poi2).toDouble()
    }
}
