package com.marinov.watchlauncher.data.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val icon: Drawable,
    val packageName: String,
    val className: String
)