package com.myprojects.plantchronicle.dao

import com.myprojects.plantchronicle.dao.dto.Plant
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface IPlaintDAO {

    @GET("/perl/mobile/viewplantsjson.pl")
    fun getAllPlants(): Call<ArrayList<Plant>>

    @GET("/perl/mobile/viewplantsjson.pl")
    fun getPlants(
        @Query("Combined_Name") plantName: String
    ) : Call<ArrayList<Plant>>
}