package com.ezymd.restaurantapp.delivery

/**
 * Created by VTSTN-27 on 8/17/2016.
 */
interface ServerConfig {
    companion object {

        const val IS_TESTING = false
        const val BASE_URL = "http://app.ezymd.com/api/"

        const val OTP_HASH = "FCWFd3gBNu0"


        const val GENERATE_OTP = "sendOtp"
        const val LIST_BANNER = "nearByRestaurantBanner"
        const val LOGOUT = "logout"
        const val LIST_RESTURANTS = "nearByRestaurant"
        const val SEARCH_RESTURANTS = "nearByRestaurant"
        const val RESTURANT_DETAILS = "restaurantItems/{id}"
        const val SOCIAL_LOGIN_USER = "socialLoginRegister"
        const val LOGIN_USER = "agentlogin"
        const val LIST_TRENDING = "trendingFoods"


        const val DIRECTION_API = "https://maps.googleapis.com/maps/api/directions/json"
        const val CREATE_ORDER = "order"
        const val UPDATED_COORDINATES = "updateOrderLocation"
        const val ASSIGN_ORDER_DELIVERY = "assignOrderToDeliveryAgent"
        const val ACCEPT_ORDER = "updateOrderStatus"
        const val CHANGE_DUTY_STATUS = "updateUserDuty"
        const val UPDATE_PROFILE = "updateProfile"
        const val CHANGE_PASSWORD = "forgotPassword"
        const val FAQ_URL = "http://app.ezymd.com/faq"
    }
}
