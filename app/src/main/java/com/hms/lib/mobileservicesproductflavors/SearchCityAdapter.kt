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


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hms.lib.mobileservicesproductflavors.model.City


class SearchCityAdapter(
    private val cityList: ArrayList<City>,
    private val itemClickCallback :(city: City) -> Unit
) : RecyclerView.Adapter<SearchCityAdapter.FeedViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(
            parent.context
        ).inflate(R.layout.search_city_item_list, parent, false)
        return FeedViewHolder(view)
    }

    override fun getItemCount(): Int {
        return cityList.size
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        holder.itemView.findViewById<TextView>(R.id.city_name).text = cityList[position].name
        holder.itemView.findViewById<TextView>(R.id.city_name).setOnClickListener {
            itemClickCallback.invoke(cityList[position])
        }
    }

    fun refreshSearchData(cityNewList: List<City>){
        cityList.clear()
        cityList.addAll(cityNewList)
        notifyDataSetChanged()
    }

    class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}

