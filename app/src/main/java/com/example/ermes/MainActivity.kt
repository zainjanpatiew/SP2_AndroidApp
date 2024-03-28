package com.example.ermes

import adapters.RecordsAdapter
import android.app.Dialog
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.ermes.databinding.ActivityMainBinding
import com.example.ermes.databinding.DialogAdminBinding
import com.example.ermes.databinding.DialogRecordsListBinding
import com.example.ermes.utils.Helper
import com.example.ermes.utils.Helper.checkPermissions
import com.example.ermes.utils.Helper.processImage
import com.example.ermes.utils.PermissionManagerListener
import com.example.ermes.utils.Preferences
import com.google.firebase.firestore.FirebaseFirestore
import com.sunmi.authorizelibrary.SunmiAuthorizeSDK
import com.sunmi.authorizelibrary.bean.AuthorizeResult
import com.sunmi.authorizelibrary.constants.ErrorCode
import com.sunmi.facelib.SunmiFaceBoxSortMode
import com.sunmi.facelib.SunmiFaceCompareResult
import com.sunmi.facelib.SunmiFaceConfigParam
import com.sunmi.facelib.SunmiFaceSDK
import database.RoomDataBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import models.EmpRecord
import models.UserModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext


class MainActivity : AppCompatActivity(), CoroutineScope,
    PermissionManagerListener {

    private val TAG = "MainActivity"
    private lateinit var job: Job
    lateinit var binding: ActivityMainBinding
    var file:File?=null
    val db= FirebaseFirestore.getInstance()
    var checkedIn=false
    private lateinit var nfcAdapter: NfcAdapter
    val usersList= mutableListOf<UserModel>()

    private var pendingIntent: PendingIntent? = null
    private val intentFiltersArray: Array<IntentFilter?> = arrayOfNulls(1)
    private val techListsArray: Array<Array<String>?> = arrayOfNulls(1)


    override val coroutineContext: CoroutineContext
        get() = job+ Dispatchers.IO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db.clearPersistence()
        job= Job()
        // Copy the files from assets to a new location


        checkPermissions(this, Helper.permissions)

        binding.accountsBtn?.setOnClickListener {
            verifyAdmin()
        }


        initNFCAdapter()

    }

    private fun initNFCAdapter(){
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            FLAG_MUTABLE
        )

        intentFiltersArray[0] = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        techListsArray[0] = arrayOf(Ndef::class.java.name)

    }



    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }


    fun syncGetAuthorizeCode(context: Context): String {
        // Build request body
        val params: MutableMap<String, Any> = HashMap()

        // Unique identifier generated after creating application on the Sunmi partner platform
        params[SunmiAuthorizeSDK.APP_ID] = "5d611ce81d1145e4b52c573925248a0b"

        // Obtain the authorization code token such as: SunmiAuthorizeSDK.CATEGORY_TYPE_FACE
        params[SunmiAuthorizeSDK.CATEGORY_TYPE_KEY] = SunmiAuthorizeSDK.CATEGORY_TYPE_FACE

        // Whether to forcibly pull token from the server, default is false.
        // true: prioritize fetching from server, use local cache if fetching fails
        // false: use local cache first, request server if fetching from local cache fails
        params[SunmiAuthorizeSDK.IS_FORCE_REFRESH] = true

        // Obtain authorization
        val result: AuthorizeResult = SunmiAuthorizeSDK.syncGetAuthorizeCode(params)

        Log.v(TAG, "syncGetAuthorizeCode "+result.msg)

        return if (result.code == ErrorCode.IS_SUCCESS) {
            // Successful
            result.token
        } else {
            ""
        }
    }


    @Throws(IOException::class)
    private fun copyAssetsFiles(context: Context) {
        // List of files specified in the configuration
        val filesToCopy = arrayOf(
            "face.model",
            "detect.model",
            "rgb_liveness.model",
            "nir_liveness.model",
            "attribute.model",
            "face_occlusion.model",
            "head_pose.model",
            "depth_detector.yml",
            "sunmi_face.db",
            "config.json"
        )
        for (fileName in filesToCopy) {
            copyFile(context, fileName)
        }
    }


    private fun initSDK(newConfigPath: String) {
        try {
            SunmiFaceSDK.createHandle()
            val ret=SunmiFaceSDK.init(newConfigPath)

            launch {
                val license: String = syncGetAuthorizeCode(this@MainActivity)
                Log.v(TAG, "license "+license)
                val ret1 = SunmiFaceSDK.verifyLicense(this@MainActivity, license)
            }

            val param = SunmiFaceConfigParam()
            SunmiFaceSDK.getConfig(param)
            param.distanceThreshold =
                0.9f // When the SDK is employed in payment scenarios, the recommended value is 0.9; for identity verification scenarios, the advised value is 1.1. The lower the threshold, the stricter the SDK's judgement for the same person

            param.faceScoreThreshold =
                0.7f // face confidence threshold, face information below this threshold will be discarded

            param.minFaceSize =
                60 // The minimum face detection size, the cleaner and larger faces lead to more precise recognition

            param.threadNum = 2 // Using two CPU cores for face detection and recognition

            param.boxSortMode =
                SunmiFaceBoxSortMode.BoxSortMode_Score // Sorting method for face frame



            binding.checkInBtn?.setOnClickListener {
                checkedIn=true
                openCameraLauncher(1)
            }
            binding.checkOutBtn?.setOnClickListener {
                checkedIn=false
                openCameraLauncher(1)
            }


        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SDK: " + e.message)
            -1 // Replace with appropriate error code
        }
    }



    fun copyFile(context: Context,configFileName:String) {
        val assetsManager = context.assets


        try {
            // Path for the new config file in internal storage
            val newConfigFilePath = File(context.filesDir, configFileName)

            // Open the asset file
            val inputStream: InputStream = assetsManager.open(configFileName)

            // Copy the asset file to internal storage
            val outputStream = FileOutputStream(newConfigFilePath)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            if(configFileName.equals("config.json")){
                Preferences(this).configFilePath=newConfigFilePath.absolutePath
                Preferences(this).configFilePath?.let { initSDK(it) }

            }

            Log.v(TAG, "copied")

            // Now, newConfigFilePath contains the path to the copied config file in internal storage
        } catch (e: Exception) {
            Log.v(TAG, "not copied"+e.message)
            e.printStackTrace()
        }
    }







    fun getPixelsBGR(image: Bitmap): ByteArray {
        // Calculate how many bytes in the image
        val bytes = image.byteCount
        val buffer = ByteBuffer.allocate(bytes) // Create a new buffer
        image.copyPixelsToBuffer(buffer) // Move byte data to buffer
        val temp = buffer.array() // Get underlying array containing data
        val pixels = ByteArray(temp.size / 4 * 3) // Assign space to BGR

        // Recomposition of pixels
        for (i in 0 until temp.size / 4) {
            pixels[i * 3] = temp[i * 4 + 2] //B
            pixels[i * 3 + 1] = temp[i * 4 + 1] //G
            pixels[i * 3 + 2] = temp[i * 4] //R
        }
        return pixels
    }

    override fun onSinglePermissionGranted(permissionName: String, vararg endPoint: String?) {

    }

    override fun onMultiplePermissionGranted(
        permissionName: ArrayList<String>,
        vararg endPoint: String?
    ) {
        //openCameraLauncher(1)

        val path=Preferences(this).configFilePath
        if(path?.isEmpty() == true||path==null) {
            copyAssetsFiles(this)
            Log.v("FaceDetection","first")
        }else{
            initSDK(path)
            Log.v("FaceDetection","init")
        }


    }

    private fun openCameraLauncher(cameraType:Int) {

        file = Helper.createImageFile(
            this,System.currentTimeMillis().toString()+"img"
        )
        /*if(file?.exists() == true){
            file?.delete()
        }*/
        file?.also {
            val uri = Helper.getFileProvider(this, it)
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            intent.putExtra("android.intent.extras.CAMERA_FACING", cameraType)
            cameraLauncher.launch(intent)
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { data ->

        if(data.resultCode== RESULT_OK) {

            file?.let {
                launch {
                    checkFaceFound(it)
                }

            }

        }
    }



    private suspend fun checkFaceFound(file: File) {
        val feature = processImage(file.absolutePath).await()
        var userFound = false // Flag to indicate if user has been found

        RoomDataBase.getInstance(this@MainActivity)?.userDao()?.getUsers?.toList()?.let { users ->
            for (user in users) {
                val feature2 = processImage(user.file).await()
                if (feature2 != null && feature != null) {
                    val result = SunmiFaceCompareResult()
                    SunmiFaceSDK.compare1v1(feature, feature2, result)
                    if (result.isMatched) {
                        withContext(Dispatchers.Main) {
                            updateRecord(user.userId, user.name, user.configId)
                        }
                        userFound = true // Set flag to true as user is found
                        break // Stop iterating as we've found a match
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "File not found", Toast.LENGTH_LONG).show()
                    }
                    return // Exit the function if file is not found
                }
            }

            if (!userFound) {
                // Only show this message if no users were found after checking all
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "User not found", Toast.LENGTH_LONG).show()
                }
            }
        }
    }



    private fun updateRecord(id: String, name: String, configId: String,isNFCCard:Boolean=false) {



        db.collection("Config").document(configId).get().addOnSuccessListener {
            if(it==null){
                Toast.makeText(this, "Config data not found", Toast.LENGTH_LONG).show()
                return@addOnSuccessListener
            }

            val startTime = it.getString("startWork")
            val endTime = it.getString("endWork")
            val startDay = it.getString("startDay")
            val endDay = it.getString("endDay")

            if (isNFCCard&&startTime!=null) {
                checkedIn = Helper.isCheckIn(startTime)
            }

            if(startDay!=null&&endDay!=null){
                if(!Helper.isCurrentDayBetween(startDay,endDay)){
                    Toast.makeText(this, "You cant checkIn or Checkout today", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
            }

            val updateObj: MutableMap<String, Any> = HashMap()
            updateObj["id"] = id
            updateObj["uid"] = id
            updateObj["name"] = name
            updateObj["date"] = Helper.getDate()

            val docPath = db.collection("CheckIns").document(id + Helper.getTodayUniqueNumber())

            docPath.get().addOnSuccessListener { document ->
                if (checkedIn) {
                    if (document.exists() && document.get("checkedIn").toString() != "null") {
                        Toast.makeText(this, "You already checked in today", Toast.LENGTH_LONG).show()
                    } else {
                        updateObj["checkedIn"] = Helper.getTime()
                        updateObj["is_late"] = Helper.isLateForCheckIn(startTime ?: "00:00")
                        docPath.set(updateObj)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Checked in successfully", Toast.LENGTH_LONG).show()
                                showRecords(id)
                            }
                    }
                } else { // Checking out
                    if (document.exists() && document.get("checkedIn").toString() != "null") {
                        if (document.get("checkedOut").toString() == "null") {
                            updateObj["checkedOut"] = Helper.getTime()
                            updateObj["is_early"] = Helper.isEarlyForCheckOut(endTime ?: "23:59")
                            docPath.update(updateObj)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Checked out successfully", Toast.LENGTH_LONG).show()
                                    showRecords(id)
                                }
                        } else {
                            Toast.makeText(this, "You already checked out today", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this, "You need to check in before checking out", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Something went wrong, please try again", Toast.LENGTH_LONG).show()
        }
    }



    private fun showRecords(id:String){

        val records= mutableListOf<EmpRecord>()
        db.collection("CheckIns").limit(10).whereEqualTo("id",id).get().addOnSuccessListener {
            it.documents.forEach {
                val record = it.toObject(EmpRecord::class.java)
                if (record != null) {
                    records.add(record)
                }
            }
            showRecordsDialog(records,id)
        }

    }

    private fun showAllRecords(id:String){

        val records= mutableListOf<EmpRecord>()
        db.collection("CheckIns").whereEqualTo("id",id).get().addOnSuccessListener {
            it.documents.forEach {
                val record = it.toObject(EmpRecord::class.java)
                if (record != null) {
                    records.add(record)
                }
            }
            showRecordsDialog(records,id,true)
        }

    }

    private fun showRecordsDialog(records:List<EmpRecord>,id:String,viewAll:Boolean=false){
        Log.v("showrecords",records.size.toString())

        val dialog = Dialog(this)
        val binding = DialogRecordsListBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(binding.root)
        if(viewAll){
            binding.status?.setBackgroundResource(R.drawable.bg_button_white)
            binding.status?.text="Record"
            binding.viewAllBtn?.visibility=View.GONE
        }
        else
            if(checkedIn){
                binding.status?.setBackgroundResource(R.drawable.bg_button_green)
                binding.status?.text="Check In"
            }else{
                binding.status?.setBackgroundResource(R.drawable.bg_button_red)
                binding.status?.text="Check Out"
            }

        binding.viewAllBtn?.setOnClickListener {
            showAllRecords(id)
            dialog.dismiss()
        }
        val adapter= RecordsAdapter(records)
        binding.recordsRV?.adapter=adapter


        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = ViewGroup.LayoutParams.MATCH_PARENT
        dialog.window!!.setLayout(width, height)
        dialog.show()

    }



    private fun verifyAdmin(){

        val dialog = Dialog(this)
        val binding = DialogAdminBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(binding.root)
        binding.loginBtn.setOnClickListener {
            val mName=binding.nameBox.text.toString()
            val mPass=binding.passBox.text.toString()

            if(mName.isNotEmpty()&&mPass.isNotEmpty()){

                db.collection("Employee").whereEqualTo("first_name",mName)
                    .whereEqualTo("employeeid",mPass).get().addOnSuccessListener {
                        if(it.documents.isEmpty()){
                            Toast.makeText(this@MainActivity,"Invalid Credentials!",Toast.LENGTH_LONG).show()
                            return@addOnSuccessListener
                        }
                        val doc=it.documents[0]
                        if(doc.exists()){
                            if(doc.getBoolean("is_admin")==true){
                                startActivity(Intent(this,EmployeesActivity::class.java))
                                dialog.dismiss()
                            }
                            else{
                                Toast.makeText(this@MainActivity,"You are not admin!",Toast.LENGTH_LONG).show()

                            }
                        }else{
                            Toast.makeText(this@MainActivity,"Invalid Credentials!",Toast.LENGTH_LONG).show()
                        }

                    }.addOnFailureListener {
                        Toast.makeText(this@MainActivity,"Something wrong try again!",Toast.LENGTH_LONG).show()

                    }

            }else{
                Toast.makeText(this@MainActivity,"Name and Password Required",Toast.LENGTH_LONG).show()
            }




        }


        dialog.show()

    }


    override fun onResume() {
        super.onResume()
        getUsers()
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)

    }

    override fun onPause() {
        super.onPause()
        nfcAdapter.disableForegroundDispatch(this)
    }






    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        Log.v(TAG,intent?.extras.toString())

        val tag: Tag? = intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        val ndef = Ndef.get(tag)

        ndef.connect()
        val messages = ndef?.ndefMessage
        if (messages != null) {
            // Parse NDEF message and update TextView
            val payload = messages.records[0].payload
            val languageCodeLength: Int = payload[0].toInt() and 0x3F // Extracting the length of the language code
            val text = String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, Charsets.UTF_8)
            val userName=text.split("#")[0]
            val userId=text.split("#")[1]
            val configId=text.split("#")[2]
            updateRecord(userId,userName,configId,true)
        } else {

        }

    }




    private fun getUsers(){

        usersList.clear()
        db.collection("Employee").whereEqualTo("faceAdded",true).addSnapshotListener { value, error ->

            usersList.clear()
            value?.documents?.forEach {
                val user = it.toObject(UserModel::class.java)
                if (user != null) {
                    usersList.add(user)
                }
            }
        }
    }




}