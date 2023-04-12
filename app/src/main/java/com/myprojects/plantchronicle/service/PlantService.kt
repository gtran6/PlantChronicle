package com.myprojects.plantchronicle.service

import androidx.lifecycle.MutableLiveData
import com.myprojects.plantchronicle.dao.IPlaintDAO
import com.myprojects.plantchronicle.dto.Plant
import com.myprojects.plantchronicle.extra.RetrofitClientInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PlantService {

    fun fetchPlants(plantName: String) : MutableLiveData<ArrayList<Plant>> {
        var _plant = MutableLiveData<ArrayList<Plant>>()
        val service = RetrofitClientInstance.retrofitInstance?.create(IPlaintDAO::class.java)
        val call = service?.getAllPlants()
        call?.enqueue(object : Callback<ArrayList<Plant>> {
            override fun onResponse(
                call: Call<ArrayList<Plant>>,
                response: Response<ArrayList<Plant>>
            ) {
                _plant.value = response.body()
            }

            override fun onFailure(call: Call<ArrayList<Plant>>, t: Throwable) {
            }
        })

        return _plant
    }
}