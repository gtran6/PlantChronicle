package com.myprojects.plantchronicle.service

import androidx.lifecycle.MutableLiveData
import com.myprojects.plantchronicle.dto.Plant

class PlantService {

    fun fetchPlants(plantName: String) : MutableLiveData<ArrayList<Plant>> {
        return MutableLiveData<ArrayList<Plant>>()
    }
}