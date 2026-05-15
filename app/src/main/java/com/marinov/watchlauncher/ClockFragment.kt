package com.marinov.watchlauncher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class ClockFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Apenas infla o layout, o gesto de deslizar agora é nativo pelo ViewPager2
        return inflater.inflate(R.layout.fragment_clock, container, false)
    }
}