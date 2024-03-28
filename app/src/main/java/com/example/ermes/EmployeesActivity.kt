package com.example.ermes

import adapters.UsersAdapter
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.ermes.databinding.ActivityEmployeesBinding
import com.example.ermes.utils.ByteUtils
import com.example.ermes.utils.FixingImageRotation
import com.example.ermes.utils.Helper
import com.example.ermes.utils.Helper.getPixelsBGR
import com.example.ermes.utils.Helper.processImage
import com.example.ermes.utils.Helper.toBase64
import com.google.firebase.firestore.FirebaseFirestore
import com.sunmi.facelib.SunmiFaceCompareResult
import com.sunmi.facelib.SunmiFaceFeature
import com.sunmi.facelib.SunmiFaceImage
import com.sunmi.facelib.SunmiFaceImageFeatures
import com.sunmi.facelib.SunmiFaceLib
import com.sunmi.facelib.SunmiFaceSDK
import database.RoomDataBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import models.User
import models.UserModel
import java.io.File
import kotlin.coroutines.CoroutineContext


class EmployeesActivity : AppCompatActivity(), CoroutineScope {


    private lateinit var job: Job
    lateinit var binding: ActivityEmployeesBinding
    var file:File?=null
    val db=FirebaseFirestore.getInstance()
    val usersList= mutableListOf<UserModel>()
    lateinit var user: UserModel


    override val coroutineContext: CoroutineContext
        get() = job+ Dispatchers.IO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmployeesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db.clearPersistence()
        job= Job()
        getUsers()
        binding.backBtn?.setOnClickListener { finish() }


    }

    private fun getUsers(){
        db.collection("Employee").whereEqualTo("faceAdded",false).addSnapshotListener { value, error ->

            usersList.clear()
            value?.documents?.forEach {
                val user = it.toObject(UserModel::class.java)
                if (user != null) {
                    usersList.add(user)
                }
            }

            val adapter=UsersAdapter(usersList)
            adapter.setOnClickListener(object :UsersAdapter.UserClickListener{
                override fun onUserClick(position: Int) {
                    user=usersList[position]
                    openCameraLauncher(1)
                }

            })
            binding.usersRV?.adapter=adapter
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }








    private fun openCameraLauncher(cameraType:Int) {

        file = Helper.createImageFile(
            this,System.currentTimeMillis().toString()+"img"
        )

        file?.also {
            val uri = Helper.getFileProvider(this, it)
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            intent.putExtra("android.intent.extras.CAMERA_FACING", cameraType)
            cameraLauncher.launch(intent)
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { data ->
        if (data.resultCode == RESULT_OK) {
            file?.let {
                launch {
                    val userExists = checkFaceFound(it)
                    withContext(Dispatchers.Main) {
                        if (userExists) {
                            Toast.makeText(this@EmployeesActivity, "User already exists", Toast.LENGTH_LONG).show()
                        } else {
                            saveUserToDB(it)
                        }
                    }
                }
            }
        }
    }



    private fun saveUserToDB(file: File) {
        val user = User()
        user.userId = this.user.id
        user.configId = this.user.config_id
        user.name = this.user.first_name
        user.file = file.absolutePath

        val updateObj: MutableMap<String, Any> = HashMap()
        updateObj["faceAdded"] = true
        db.collection("Employee").document(this.user.id).update(updateObj)
            .addOnSuccessListener {
                // Toast needs to be shown on the main thread
                runOnUiThread {
                    Toast.makeText(this, "User added", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                // Toast needs to be shown on the main thread
                runOnUiThread {
                    Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                }
            }

        launch {
            RoomDataBase.getInstance(this@EmployeesActivity)?.userDao()?.insert(user)
        }
    }





    private suspend fun checkFaceFound(file: File):Boolean {
        val feature = processImage(file.absolutePath).await()
        var userFound = false // Flag to indicate if user has been found

        RoomDataBase.getInstance(this@EmployeesActivity)?.userDao()?.getUsers?.toList()?.let { users ->
            for (user in users) {
                val feature2 = processImage(user.file).await()
                if (feature2 != null && feature != null) {
                    val result = SunmiFaceCompareResult()
                    SunmiFaceSDK.compare1v1(feature, feature2, result)
                    if (result.isMatched) {
                        userFound = true // Set flag to true as user is found
                        break // Stop iterating as we've found a match
                    }
                }
            }
        }

        return userFound
    }





}