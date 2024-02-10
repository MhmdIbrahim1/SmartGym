package com.example.smartgymapp.ui.doctor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.smartgymapp.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DoctorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor)
    }
}