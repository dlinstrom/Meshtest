package com.geeksville.mesh.util

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Parcel
import android.os.Parcelable
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.os.ParcelCompat

object PendingIntentCompat {
    val FLAG_IMMUTABLE = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        PendingIntent.FLAG_IMMUTABLE
    } else {
        0
    }
}

inline fun <reified T : Parcelable> Parcel.readParcelableCompat(loader: ClassLoader?): T? =
    ParcelCompat.readParcelable(this, loader, T::class.java)

inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String?): T? =
    IntentCompat.getParcelableExtra(this, key, T::class.java)

fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int = 0): PackageInfo =
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION") getPackageInfo(packageName, flags)
    }

fun Context.registerReceiverCompat(
    receiver: BroadcastReceiver,
    filter: IntentFilter,
    flag: Int = ContextCompat.RECEIVER_EXPORTED,
) {
    ContextCompat.registerReceiver(this, receiver, filter, flag)
}
