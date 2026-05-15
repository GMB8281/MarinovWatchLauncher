package com.marinov.watchlauncher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2

class CenterFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_center, container, false)
        val viewPager: ViewPager2 = view.findViewById(R.id.viewPagerVertical)

        // Define a navegação para ser Vertical (para cima / para baixo)
        viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL

        // Adicionamos este bloco para travar a rolagem horizontal da Activity Principal
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val parentViewPager = requireActivity().findViewById<ViewPager2>(R.id.viewPager)
                // Se a posição for 0 (Relógio), libera o deslize pros lados. Se for 1 (Widgets), bloqueia.
                parentViewPager?.isUserInputEnabled = (position == 0)
            }
        })

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> ClockFragment()     // Posição 0: Relógio em cima
                    1 -> WidgetFragment()    // Posição 1: Widgets em baixo (deslizar para cima revela)
                    else -> ClockFragment()
                }
            }
        }

        return view
    }
}