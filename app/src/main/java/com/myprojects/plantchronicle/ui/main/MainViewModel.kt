package com.myprojects.plantchronicle.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.myprojects.plantchronicle.dto.Plant
import com.myprojects.plantchronicle.service.PlantService

class MainViewModel : ViewModel() {
    var plants: MutableLiveData<ArrayList<Plant>> = MutableLiveData<ArrayList<Plant>>()
    var plantService: PlantService = PlantService()
    fun fetchPlants(plantName: String) {
        plants = plantService.fetchPlants(plantName)
    }
}