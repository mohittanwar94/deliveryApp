package com.ezymd.restaurantapp.delivery.login

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.animation.AlphaAnimation
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ezymd.restaurantapp.delivery.BaseActivity
import com.ezymd.restaurantapp.delivery.HomeScreen
import com.ezymd.restaurantapp.delivery.R
import com.ezymd.restaurantapp.delivery.login.model.LoginModel
import com.ezymd.restaurantapp.delivery.utils.*
import kotlinx.android.synthetic.main.login.*


class Login : BaseActivity() {
    lateinit var loginViewModel: LoginViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        loginViewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
        setEventListener()

    }

    override fun onStart() {
        super.onStart()
        slideUp(content)
    }

    private fun setEventListener() {
        next.setOnClickListener {
            SuspendKeyPad.suspendKeyPad(this)
            UIUtil.clickHandled(it)
            checkConditions()
        }
        loginViewModel.isLoading.observe(this, Observer {

        })


        loginViewModel.loginResponse.observe(this, Observer {

            if (it.status == ErrorCodes.SUCCESS) {
                setLoginUser(it)

            } else {
                showError(false, it.message, null)
            }
        })

        loginViewModel.showError().observe(this, Observer {
            showError(false, it, null)
        })

        loginViewModel.isLoading.observe(this, Observer {
            progress.visibility = if (it) View.VISIBLE else View.GONE
        })
    }

    fun CharSequence?.isValidEmail() =
        Patterns.EMAIL_ADDRESS.matcher(this).matches()

    private fun checkConditions() {
        if (userName.text.toString().trim().isEmpty() || !userName.text.isValidEmail()) {
            ShowDialog(this).disPlayDialog(
                getString(R.string.user_name_can_not_be_empty),
                false,
                false
            )
            return
        } else if (userName.text.toString().trim().isEmpty()) {
            ShowDialog(this).disPlayDialog(
                getString(R.string.password_can_not_be_empty),
                false,
                false
            )
            return
        }

        val baseRequest = BaseRequest()
        baseRequest.paramsMap["email"] = userName.text.toString().trim()
        baseRequest.paramsMap["password"] = password.text.toString().trim()
        baseRequest.paramsMap["role_id"] = "5"
        loginViewModel.login(baseRequest)
    }


    private fun slideUp(view: View) {
        view.visibility = View.VISIBLE
        val animate = AlphaAnimation(
            0.0f,                 // fromXDelta
            1f,                 // toXDelta

        )             // toYDelta
        animate.duration = 500
        animate.fillAfter = true
        view.startAnimation(animate)

    }

    override fun onResume() {
        super.onResume()
    }

    private fun setLoginUser(it: LoginModel) {
        userInfo?.accessToken = it.data.access_token
        userInfo?.userName = it.data.user.name
        userInfo?.email = it.data.user.email
        userInfo?.userID = it.data.user.id
        userInfo?.phoneNumber = it.data.user.phone_no
        userInfo?.profilePic = it.data.user.profile_pic
        startActivity(Intent(this, HomeScreen::class.java))
        finish()

    }


}