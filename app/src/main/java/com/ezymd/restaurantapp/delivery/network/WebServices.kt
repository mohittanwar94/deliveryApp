package com.ezymd.restaurantapp.network


import com.ezymd.restaurantapp.delivery.ServerConfig
import com.ezymd.restaurantapp.delivery.home.profile.LogoutModel
import com.ezymd.restaurantapp.delivery.login.model.LoginModel
import com.ezymd.restaurantapp.delivery.login.model.OtpModel
import com.ezymd.restaurantapp.delivery.order.model.OrderAcceptResponse
import com.ezymd.restaurantapp.delivery.order.model.OrderBaseModel
import com.ezymd.restaurantapp.delivery.utils.BaseResponse
import com.google.gson.JsonObject
import retrofit2.http.*

interface WebServices {
    @FormUrlEncoded
    @POST(ServerConfig.GENERATE_OTP)
    suspend fun sendOtp(
        @FieldMap commonParameters: Map<String, String>
    ): OtpModel

    @GET(ServerConfig.LOGOUT)
    suspend fun logout(
        @Header("Authorization") token: String
    ): LogoutModel

    @FormUrlEncoded
    @POST(ServerConfig.LOGIN_USER)
    suspend fun loginUser(
        @FieldMap commonParameters: Map<String, String>
    ): LoginModel


    @GET(ServerConfig.DIRECTION_API)
    suspend fun downloadRoute(
        @Query("origin") url: String,
        @Query("sensor") sensor: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String,
        @Query("key") key: String,
        @Query("waypoints") waypoints: String
    ): JsonObject

    @FormUrlEncoded
    @POST(ServerConfig.UPDATED_COORDINATES)
    suspend fun locationUpdates(
        @FieldMap commonParameters: Map<String, String>,
        @Header("Authorization") accessToken: String
    ): BaseResponse


    @GET(ServerConfig.CREATE_ORDER)
    suspend fun orderList(@Query("device_token") device_token:String,@Query("device_id") device_id:String,@Query("order_status") order_status:String, @Header("Authorization") accessToken: String): OrderBaseModel

    @GET(ServerConfig.CREATE_ORDER)
    suspend fun cancelOrderList(@Query("device_token") device_token:String,@Query("device_id") device_id:String,@Query("order_status") order_status:String,@Query("delivery_boy_id") delivery_boy_id:String, @Header("Authorization") accessToken: String): OrderBaseModel

    @FormUrlEncoded
    @POST(ServerConfig.ACCEPT_ORDER)
    suspend fun acceptOrder(
        @FieldMap commonParameters: Map<String, String>,
        @Header("Authorization") accessToken: String
    ): OrderAcceptResponse

    @FormUrlEncoded
    @POST(ServerConfig.ASSIGN_ORDER_DELIVERY)
    suspend fun assignOrder(
        @Field("order_id") restaurant_id: String,
        @Header("Authorization") accessToken: String
    ): OrderAcceptResponse

}

