package com.example.ermes.utils

interface PermissionManagerListener {
    fun onSinglePermissionGranted(permissionName: String, vararg endPoint: String?)
    fun onMultiplePermissionGranted(permissionName: ArrayList<String>, vararg endPoint: String?)
}