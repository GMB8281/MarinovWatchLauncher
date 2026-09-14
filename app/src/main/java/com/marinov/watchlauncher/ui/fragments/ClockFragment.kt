package com.marinov.watchlauncher.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import com.marinov.watchlauncher.R
import com.marinov.watchlauncher.ui.activities.LauncherActivity

class ClockFragment : Fragment() {

    private lateinit var gestureDetector: GestureDetectorCompat

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_clock, container, false)

        gestureDetector = GestureDetectorCompat(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                handleDoubleTap()
                return true
            }
        })

        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
        return view
    }

    private fun handleDoubleTap() {
        val activity = requireActivity() as LauncherActivity
        val prefs = activity.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
        val doubleTapEnabled = prefs.getBoolean("double_tap_lock", true)

        if (!doubleTapEnabled) return

        if (activity.isDeviceAdminActive()) {
            activity.lockScreen()
        } else {
            AlertDialog.Builder(requireContext())
                .setTitle("Administrador necessário")
                .setMessage("O bloqueio de tela requer permissão de administrador. Deseja ativar agora?")
                .setPositiveButton("Sim") { _, _ ->
                    val intent = android.content.Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, activity.getAdminComponentName())
                    intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Permita que o launcher bloqueie a tela.")
                    startActivity(intent)
                }
                .setNegativeButton("Não", null)
                .show()
        }
    }
}