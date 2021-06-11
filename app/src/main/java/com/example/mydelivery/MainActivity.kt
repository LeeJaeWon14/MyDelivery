package com.example.mydelivery

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mydelivery.adapter.MyRecyclerAdapter
import com.example.mydelivery.databinding.ActivityMainBinding
import com.example.mydelivery.databinding.SharedLayoutBinding
import com.example.mydelivery.dto.CarrierDTO
import com.example.mydelivery.dto.TrackerDTO
import com.example.mydelivery.model.NetworkConstants
import com.example.mydelivery.util.MyLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private var _binding : ActivityMainBinding? = null
    private val binding
        get() = _binding!!
    private val companyCodeList = ArrayList<String>()
    private val companyInfo = HashMap<String, String>()
    private var isShared = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        when (intent.action) {
            Intent.ACTION_SEND -> {
                if("text/plain" == intent.type) {
                    handleSendText(intent)
                }
            }
        }

        binding.btnInputOk.setOnLongClickListener {
            binding.edtInput.setText("640818406945")
            false
        }

        getCarriers() //with initSpinner()
        binding.btnInputOk.setOnClickListener(showTrackListener)
    }

    private fun initRetrofit() : DeliveryService {
        val retrofit = Retrofit.Builder()
            .baseUrl(NetworkConstants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(DeliveryService::class.java)
    }

    private fun getCarriers() {
        val request = initRetrofit().getCarriers()

        CoroutineScope(Dispatchers.IO).launch {
            request.enqueue(object : Callback<List<CarrierDTO>> {
                override fun onFailure(call: Call<List<CarrierDTO>>, t: Throwable) {
                    runOnUiThread(Runnable { Toast.makeText(this@MainActivity, "Response 실패", Toast.LENGTH_SHORT).show() })
                    MyLogger.e(t.message.toString())
                }

                override fun onResponse(
                    call: Call<List<CarrierDTO>>,
                    response: Response<List<CarrierDTO>>
                ) {
                    runOnUiThread(Runnable {
                        initSpinner(response.body()!!)
                    })
                }
            })
        }
    }

    private fun deliveryTracking(company : String, deliveryNumber : String) {
        val request = initRetrofit().getTracker(company, deliveryNumber)

        request.enqueue(object : Callback<TrackerDTO> {
            override fun onFailure(call: Call<TrackerDTO>, t: Throwable) {
                runOnUiThread(Runnable {
                    Toast.makeText(this@MainActivity, "Response 실패", Toast.LENGTH_SHORT).show()
                })
                MyLogger.e(t.message.toString())
            }

            override fun onResponse(call: Call<TrackerDTO>, response: Response<TrackerDTO>) {
                MyLogger.i(response.body().toString())
                runOnUiThread(Runnable {
                    initRecycler(response.body()!!)
                })
            }
        })
    }

    private fun initSpinner(result : List<CarrierDTO>) {
        val nameList = ArrayList<String>()
        for(dto in result) {
            nameList.add(dto.name)
            companyCodeList.add(dto.id)
        }
        val adapter = ArrayAdapter<String>(this, R.layout.spinner_item, nameList)
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
        binding.spCarrier.adapter = adapter
        binding.spCarrier.setSelection(0, false)

        binding.spCarrier.onItemSelectedListener = this
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Do not use
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        binding.llLayout.visibility = View.VISIBLE
        if(!isShared) binding.edtInput.setText("")
        binding.rvTrackList.adapter = null
        isShared = false

        val code = companyCodeList[position]
        companyInfo["name"] = code
    }

    private val showTrackListener = View.OnClickListener {
        companyInfo["number"] = binding.edtInput.text.toString().trim()
        deliveryTracking(companyInfo["name"]!!, companyInfo["number"]!!)

        MyLogger.i("${companyInfo["name"]}, ${companyInfo["number"]}")
    }

    private fun initRecycler(trackerInfo : TrackerDTO) {
        binding.rvTrackList.layoutManager = LinearLayoutManager(this)
        binding.rvTrackList.adapter = MyRecyclerAdapter(trackerInfo.progresses)
    }

    private fun handleSendText(intent : Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT).let {
            binding.edtInput.setText(it)
            isShared = true
        }
    }
}