package com.example.ermes.utils

import com.karumi.dexter.Dexter
import android.app.Activity
import android.content.Context
import com.karumi.dexter.listener.single.PermissionListener
import com.karumi.dexter.PermissionToken
import android.widget.Toast
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.MultiplePermissionsReport
import android.text.TextUtils
import androidx.appcompat.app.AlertDialog
import com.example.ermes.R
import com.karumi.dexter.listener.*
import java.util.ArrayList

class PermissionsHandler {
    private val mContext: Context
    private var endPoint: String? = null
    private var listener: PermissionManagerListener? = null

    constructor(mContext: Context) {
        this.mContext = mContext
    }

    constructor(mContext: Context, endPoint: String?) {
        this.mContext = mContext
        this.endPoint = endPoint
    }

    constructor(mContext: Context, listener: PermissionManagerListener?) {
        this.mContext = mContext
        this.listener = listener
    }

    constructor(mContext: Context, endPoint: String?, listener: PermissionManagerListener?) {
        this.mContext = mContext
        this.endPoint = endPoint
        this.listener = listener
    }

    //region Check Single Permission by Type
    fun checkSinglePermission(Type: String?) {
        Dexter.withContext(mContext as Activity)
            .withPermission(Type)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    if (listener == null) listener = mContext as PermissionManagerListener
                    listener!!.onSinglePermissionGranted(response.permissionName)
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    if (response.isPermanentlyDenied) showSettingsDialog()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            })
            .withErrorListener { error: DexterError ->
                Toast.makeText(
                    mContext,
                    "Error occurred! $error",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .check()
    }

    //endregion
    //region Check Multiple Permissions
    fun setSinglePermission(permission: String?) {
        Dexter.withContext(mContext as Activity)
            .withPermission(permission)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    if (listener == null) {
                        listener = mContext as PermissionManagerListener
                    }
                    if (isValidString(endPoint)) {
                        listener!!.onSinglePermissionGranted(response.permissionName, endPoint)
                    } else {
                        listener!!.onSinglePermissionGranted(response.permissionName)
                    }
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    if (response.isPermanentlyDenied) {
                        showSettingsDialog()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            })
            .withErrorListener { error: DexterError? -> }
            .check()
    }

    //endregion
    //region Display GOTO SETTINGS dialog if Permission is denied permanently
    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(mContext)
        builder.setTitle("Need Permissions")
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.")
        builder.setPositiveButton(mContext.resources.getString(R.string.btn_title_go_to_settings)) { dialog: DialogInterface, which: Int ->
            dialog.cancel()
            openSettings()
        }
        builder.setNegativeButton(mContext.resources.getString(R.string.btn_title_cancel)) { dialog: DialogInterface, which: Int -> dialog.cancel() }
        builder.show()
    }

    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", mContext.packageName, null)
        intent.data = uri
        if (mContext is Activity) {
            mContext.startActivityForResult(intent, 101)
        } else {
            Toast.makeText(
                mContext,
                mContext.resources.getString(R.string.err_msg_api_response_failure),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    //endregion
    fun setMultiplePermission(permissions: Array<String>) {
        Dexter.withContext(mContext as Activity)
            .withPermissions(*permissions)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        val grantedPermission = ArrayList<String>()
                        for (i in report.grantedPermissionResponses.indices) grantedPermission.add(
                            report.grantedPermissionResponses[i].permissionName
                        )
                        if (listener == null) listener = mContext as PermissionManagerListener
                        if (isValidString(endPoint)) {
                            listener!!.onMultiplePermissionGranted(grantedPermission, endPoint)
                        } else {
                            listener!!.onMultiplePermissionGranted(grantedPermission)
                        }
                    }
                    if (report.isAnyPermissionPermanentlyDenied) {
                        showSettingsDialog()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            })
            .withErrorListener { error: DexterError? -> }
            .onSameThread()
            .check()
    }

    companion object {
        fun isValidString(value: String?): Boolean {
            return value != null && !TextUtils.isEmpty(value) && !value.equals(
                "null",
                ignoreCase = true
            )
        }
    }
}