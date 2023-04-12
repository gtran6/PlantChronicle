package com.myprojects.plantchronicle.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.myprojects.plantchronicle.dao.dto.Plant
import com.myprojects.plantchronicle.service.PlantService

class MainViewModel : ViewModel() {
    private var _plants: MutableLiveData<ArrayList<Plant>> = MutableLiveData<ArrayList<Plant>>()
    var plantService: PlantService = PlantService()

    init {
        fetchPlants("e")
    }
    fun fetchPlants(plantName: String) {
        _plants = plantService.fetchPlants(plantName)
    }

    internal var plants: MutableLiveData<ArrayList<Plant>>
        get() {return _plants}
        set(value) {_plants = value}
}