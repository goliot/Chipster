package com.soundgram.chipster.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.view.View
import android.view.Window
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bumptech.glide.Glide
import com.soundgram.chipster.util.Constants.PERMISSION_REQUEST_CODE
import kotlin.collections.ArrayList
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

fun permissionCheck(context: Context, activity: Activity, onSuccess: () -> Unit) {

    val totalPermission = listOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.NFC,
    )
    val requirePermissions = ArrayList<String>()

    totalPermission.forEach { permission ->
        val granted = ActivityCompat.checkSelfPermission(
            context,
            permission
        )
        if (granted != PackageManager.PERMISSION_GRANTED) {
            requirePermissions.add(permission)
        }
    }

    if (requirePermissions.size > 0) {
        var strArray = arrayOfNulls<String>(requirePermissions.size)
        strArray = requirePermissions.toArray<String>(strArray)
        ActivityCompat.requestPermissions(activity, strArray, PERMISSION_REQUEST_CODE)
    }

    if (requirePermissions.size == 0) {
        onSuccess()
    }
}

fun ImageView.setImageWithUrl(
    context: Context,
    url: String?,
    @DrawableRes placeHolder: Int = -1,
    @DrawableRes errorImg: Int = -1,
    thumbnail: Float = 0.3f
) = this.apply {
    Glide.with(context)
        .load(url)
        .placeholder(placeHolder)
        .error(errorImg)
        .centerCrop()
//        .thumbnail(thumbnail)
        .into(this)
}


/**
 *  @param unit : kilometer, meter 중 하나
 */
fun distanceOf(
    lat1: Double,
    lon1: Double,
    lat2: Double?,
    lon2: Double?,
    unit: String
): Double {
    if (lat2 == null || lon2 == null) {
        return -1.0
    }
    val theta = lon1 - lon2
    var dist =
        sin(deg2rad(lat1)) * sin(deg2rad(lat2)) + cos(deg2rad(lat1)) * cos(
            deg2rad(lat2)
        ) * cos(deg2rad(theta))
    dist = acos(dist)
    dist = rad2deg(dist)
    dist *= 60 * 1.1515
    if (unit === "kilometer") {
        dist *= 1.609344
    } else if (unit === "meter") {
        dist *= 1609.344
    }
    return floor(dist)
}

private fun deg2rad(deg: Double): Double {
    return deg * Math.PI / 180.0
}

private fun rad2deg(rad: Double): Double {
    return rad * 180 / Math.PI
}


fun View.hide() {
    this.visibility = View.GONE
}

fun View.show() {
    this.visibility = View.VISIBLE
}

fun hideSystemUI(window: Window, mainContainer: View) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    WindowInsetsControllerCompat(window, mainContainer).let { controller ->
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

fun showSystemUI(window: Window, mainContainer: View) {
    WindowCompat.setDecorFitsSystemWindows(window, true)
    WindowInsetsControllerCompat(
        window, mainContainer
    ).show(WindowInsetsCompat.Type.systemBars())
}
