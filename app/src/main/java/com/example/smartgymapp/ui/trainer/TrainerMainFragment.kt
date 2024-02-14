package com.example.smartgymapp.ui.trainer

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.smartgymapp.databinding.FragmentMainBinding
import com.example.smartgymapp.databinding.FragmentTrainerMainBinding


class TrainerMainFragment : Fragment() {
    private lateinit var binding: FragmentTrainerMainBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        binding = FragmentTrainerMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }


}