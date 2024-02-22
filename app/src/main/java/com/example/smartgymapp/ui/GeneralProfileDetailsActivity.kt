package com.example.smartgymapp.ui

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.example.smartgymapp.R
import com.example.smartgymapp.databinding.ActivityGeneralProfileDetailsBinding
import com.example.smartgymapp.model.UserModel
import com.example.smartgymapp.util.CommonActivity
import com.example.smartgymapp.util.CommonActivity.updateLocale
import com.example.smartgymapp.util.MyContextWrapper
import com.example.smartgymapp.util.MyPreference

class GeneralProfileDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGeneralProfileDetailsBinding
    private lateinit var myPreference: MyPreference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGeneralProfileDetailsBinding.inflate(layoutInflater)
        CommonActivity.init(this)
        setContentView(binding.root)

        // Get the data from the intent
        val otherUser = CommonActivity.getUserModelFromIntent(intent)
        setUserDataToViews(otherUser)

        binding.backBtn.setOnClickListener {
            finish()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateLocale()
    }

    private fun setUserDataToViews(otherUser: UserModel) {
        binding.apply {
            Glide.with(this@GeneralProfileDetailsActivity)
                .load(otherUser.profile_picture)
                .placeholder(R.drawable.man_user)
                .into(imageUser)

            edFirstName.setText(otherUser.firstName)
            edLastName.setText(otherUser.lastName)
            edEmail.setText(otherUser.email)
            edUserType.setText(otherUser.userType)
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        myPreference = MyPreference(newBase!!)
        val lang = myPreference.getLanguage()
        super.attachBaseContext(MyContextWrapper.wrap(newBase,lang))
    }
}