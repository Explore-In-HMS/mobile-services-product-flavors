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

package com.hms.lib.mobileservicesproductflavors

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.SearchView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hms.lib.mobileservicesproductflavors.analytics.AnalyticsKit
import com.hms.lib.mobileservicesproductflavors.analytics.saveData
import com.hms.lib.mobileservicesproductflavors.location.LocationClient
import com.hms.lib.mobileservicesproductflavors.location.LocationClientImpl
import com.hms.lib.mobileservicesproductflavors.location.model.EnableGPSFinalResult
import com.hms.lib.mobileservicesproductflavors.location.model.LocationResultState
import com.hms.lib.mobileservicesproductflavors.map.CommonMapView
import com.hms.lib.mobileservicesproductflavors.map.factory.CommonMap
import com.hms.lib.mobileservicesproductflavors.map.model.CommonLatLng
import com.hms.lib.mobileservicesproductflavors.model.City
import com.hms.lib.mobileservicesproductflavors.push.PushServiceImpl
import com.hms.lib.mobileservicesproductflavors.scan.ScanKit
import com.hms.lib.mobileservicesproductflavors.scan.handleSuccess
import com.hms.lib.mobileservicesproductflavors.speechtotext.SpeechToTextKit
import com.hms.lib.mobileservicesproductflavors.speechtotext.handleSuccess

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var searchRecyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var mapView: CommonMapView
    private lateinit var barcodeIcon: ImageView
    private lateinit var microphoneIcon: ImageView
    private lateinit var commonMap: CommonMap
    private lateinit var notificationToggleButton: ToggleButton
    private val scanResultCode = 205
    private val speechToTextResultCode = 207

    private val cityList: ArrayList<City> = arrayListOf(
        City("1", CommonLatLng(41.0054, 28.8720), "Istanbul"),
        City("2", CommonLatLng(39.9035, 32.6226), "Ankara"),
        City("3", CommonLatLng(41.9102, 12.3959), "Roma"),
        City("4", CommonLatLng(40.4381, -3.8196), "Madrid"),
        City("5", CommonLatLng(48.8589, 2.2770), "Paris"),
        City("6", CommonLatLng(51.5287, -0.2416), "London")
    )

    private var searchCityAdapter: SearchCityAdapter =
        SearchCityAdapter(arrayListOf()) { city ->
            searchRecyclerView.visibility = View.GONE
            commonMap.animateCamera(city.coordinate.lat, city.coordinate.lng, 9f)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AnalyticsKit(this).saveData(
            "custom_app_started",
            "started", 1,
            "OS", "android"
        )

        mapView = findViewById(R.id.map_view)
        searchRecyclerView = findViewById(R.id.cities_recycler_view)
        searchView = findViewById(R.id.search_view)
        barcodeIcon = findViewById(R.id.barcode_icon)
        microphoneIcon = findViewById(R.id.microphone_icon)
        notificationToggleButton = findViewById(R.id.notification_toggle_button)

        searchRecyclerView.apply {
            layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = searchCityAdapter
        }

        PushServiceImpl(this).subscribeToTopic(this.getString(R.string.notification_topic))

        LocationClientImpl(this@MainActivity, this.lifecycle, true).also {
            it.enableGps { enableGPSFinalResult, error ->
                when (enableGPSFinalResult) {
                    EnableGPSFinalResult.ENABLED -> {
                        locationEnabled(it)
                    }
                    EnableGPSFinalResult.FAILED -> {
                        Log.e(TAG, "onCreate: ${error?.message}")
                    }
                    EnableGPSFinalResult.USER_CANCELLED -> {
                    }
                }
            }
        }

        commonMap = mapView.onCreate(savedInstanceState, lifecycle).apply {
            getMapAsync {
                it.setOnInfoWindowClickListener { markerTitle, markerSnippet, commonLatLng ->
                    it.createRouteToTheSelectedLocation(this@MainActivity, commonLatLng)
                }
                val openMapEvent = Bundle().apply {
                    putBoolean("map_opened", true)
                }
                saveBundleToAnalytics("custom_map_open", openMapEvent)
            }
        }

        microphoneIcon.setOnClickListener {
            SpeechToTextKit().performSpeechToText(
                this,
                speechToTextResultCode,
                this.getString(R.string.speech_to_text_language_key),
                this.getString(R.string.hms_api_key)
            )
        }

        barcodeIcon.setOnClickListener {
            ScanKit().performScan(this, scanResultCode)
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                val searchPoiEvent = Bundle().apply {
                    putString("search_poi_keyword", p0.toString())
                }
                AnalyticsKit(this@MainActivity).saveEvent(
                    "custom_search_poi",
                    searchPoiEvent
                )

                searchCityAdapter.refreshSearchData(filterCityName(p0.toString()))
                searchRecyclerView.visibility = View.VISIBLE
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return true
            }
        })

        notificationToggleButton.setOnCheckedChangeListener { compoundButton, boolean ->
            if (boolean) {
                PushServiceImpl(this).subscribeToTopic(this.getString(R.string.notification_topic))
            } else {
                PushServiceImpl(this).unsubscribeFromTopic(this.getString(R.string.notification_topic))
            }
        }
    }

    private fun filterCityName(keyword: String): List<City> {
        return cityList.filterIndexed { index, city ->
            cityList[index].name.toLowerCase().contains(keyword.toLowerCase())
        }
    }

    private fun locationEnabled(locationClient: LocationClient) {
        locationClient.getLastKnownLocation {
            when (it.state) {
                LocationResultState.SUCCESS -> {
                    commonMap.addMarker(
                        "Marker", "Snippet", it.location?.latitude!!,
                        it.location?.longitude!!
                    )
                    commonMap.animateCamera(it.location?.latitude!!, it.location?.longitude!!, 10f)
                    val locationEvent = Bundle().apply {
                        putString("custom_location_lat", it.location?.latitude.toString())
                        putString("custom_location_lng", it.location?.longitude.toString())
                    }
                    saveBundleToAnalytics("custom_location", locationEvent)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == speechToTextResultCode) {
            if (data != null) {
                SpeechToTextKit().parseSpeechToTextData(
                    {
                        it.handleSuccess {
                            searchView.setQuery(it.data.toString(), true)
                            searchView.isIconified = false
                            Log.d("Speech to text result: ", it.data.toString())
                            val voiceEvent = Bundle().apply {
                                putString("custom_voice_keyword", it.data.toString())
                            }
                            saveBundleToAnalytics("custom_voice_search", voiceEvent)
                        }
                    }, this, data, resultCode
                )
            }
        } else if (requestCode == scanResultCode) {
            if (data != null) {
                ScanKit().parseScanToTextData({
                    it.handleSuccess { resultData ->
                        searchView.isIconified = false
                        searchView.setQuery(resultData.data, true)
                        Log.d("ScanSDK", resultData.data.toString())
                        val scanEvent = Bundle().apply {
                            putString("custom_scan_keyword", resultData.data.toString())
                        }
                        saveBundleToAnalytics("custom_scan_search", scanEvent)
                    }
                }, this, data)
            }
        }
    }

    override fun onBackPressed() {
        if (searchRecyclerView.visibility == View.VISIBLE) {
            searchRecyclerView.visibility = View.GONE
        } else {
            super.onBackPressed()
        }
    }

    private fun saveBundleToAnalytics(keyword: String, bundleData: Bundle) {
        AnalyticsKit(this@MainActivity).saveEvent(keyword, bundleData)
    }
}