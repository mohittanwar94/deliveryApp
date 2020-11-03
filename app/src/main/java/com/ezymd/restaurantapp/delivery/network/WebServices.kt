package com.ezymd.restaurantapp.network


import com.ezymd.restaurantapp.delivery.ServerConfig
import com.ezymd.restaurantapp.delivery.login.model.LoginModel
import com.ezymd.restaurantapp.delivery.login.model.OtpModel
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface WebServices {
    @FormUrlEncoded
    @POST(ServerConfig.GENERATE_OTP)
    suspend fun sendOtp(
        @FieldMap commonParameters: Map<String, String>
    ): OtpModel


    @FormUrlEncoded
    @POST(ServerConfig.LOGIN_USER)
    suspend fun loginUser(
        @FieldMap commonParameters: Map<String, String>
    ): LoginModel


}

