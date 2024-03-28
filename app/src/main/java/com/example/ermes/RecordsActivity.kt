package com.example.ermes

import adapters.RecordsAdapter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ermes.databinding.ActivityRecordsBinding
import com.google.firebase.firestore.FirebaseFirestore
import models.EmpRecord


class RecordsActivity : AppCompatActivity() {


    lateinit var binding: ActivityRecordsBinding
    val db= FirebaseFirestore.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val id=intent.extras?.getString("id").toString()
        showRecords(id)
    }



    private fun showRecords(id:String){

        val records= mutableListOf<EmpRecord>()
        db.collection("CheckIns").whereEqualTo("id",id).get().addOnSuccessListener {
            it.documents.forEach {
                val record = it.toObject(EmpRecord::class.java)
                if (record != null) {
                    records.add(record)
                }
            }

            val adapter=RecordsAdapter(records)
            binding.recordsRV?.adapter=adapter
        }

    }








}