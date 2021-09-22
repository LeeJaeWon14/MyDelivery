package com.example.mydelivery.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mydelivery.DeliveryService
import com.example.mydelivery.R
import com.example.mydelivery.adapter.MyRecyclerAdapter
import com.example.mydelivery.databinding.ActivityMainBinding
import com.example.mydelivery.dto.CarrierDTO
import com.example.mydelivery.dto.TrackerDTO
import com.example.mydelivery.model.NetworkConstants
import com.example.mydelivery.util.MyLogger
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

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

        binding.toolbar.setBackgroundColor(getColor(R.color.purple_700))
        binding.toolbar.setTitleTextColor(Color.WHITE)
        binding.toolbar.setSubtitleTextColor(Color.WHITE)
        binding.toolbar.title = "배송조회"
        setSupportActionBar(binding.toolbar)

        binding.btnInputOk.setOnLongClickListener {
            binding.edtInput.setText("640818406945")
            false
        }

        binding.slLayout.setProgressBackgroundColorSchemeColor(getColor(R.color.purple_500))
        binding.slLayout.setColorSchemeColors(Color.WHITE)
        binding.slLayout.setOnRefreshListener {
            showTracking()
            Toast.makeText(this, "갱신되었습니다.", Toast.LENGTH_SHORT).show()
            binding.slLayout.isRefreshing = false
        }

        getCarriers() //with initSpinner()
        binding.btnInputOk.setOnClickListener(showTrackListener)

        when (intent.action) {
            Intent.ACTION_SEND -> {
                if("text/plain" == intent.type) {
                    handleSendText(intent)
                }
            }
        }
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
                if(response.body() == null) {
                    Snackbar.make(binding.btnInputOk, "송장번호를 다시 확인해주세요", Snackbar.LENGTH_SHORT).show()
                    return
                }
                runOnUiThread(Runnable {
                    MyLogger.i("response >> ${response.body()}")
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
        if(binding.edtInput.text.toString() == "") {
            Snackbar.make(it, "송장번호를 다시 확인해주세요", Snackbar.LENGTH_SHORT).show()
            return@OnClickListener
        }
        showTracking()
    }
    private fun showTracking() {
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

            val nameList = arrayListOf<String>()
            val request = initRetrofit().getCarriers()
            CoroutineScope(Dispatchers.IO).launch {
                request.enqueue(object : Callback<List<CarrierDTO>> {
                    override fun onResponse(
                        call: Call<List<CarrierDTO>>,
                        response: Response<List<CarrierDTO>>) {
                        for(dto in response.body()!!) {
                            nameList.add(dto.name)
                        }
                    }

                    override fun onFailure(call: Call<List<CarrierDTO>>, t: Throwable) {
                        TODO("Not yet implemented")
                    }
                })
            }
        }

    }

    private fun checkDelivNumber(number : String) {
        var list : ArrayList<String>? = null
        when(number.length) {
            9 -> {
                list = arrayListOf("밀양", "경동", "합동", "USPS", "EMS")
            }
            10 -> {
                list = arrayListOf("한진", "호남", "건영", "CU", "CVS", "한덱스", "USPS", "EMS", "DHL", "밀양")
            }
            11 -> {
                list = arrayListOf("로젠", "밀양", "천일")
            }
            12 -> {
                list = arrayListOf("Fedex", "한진", "롯데", "농협", "CU", "CVS", "대한통운")
            }
            13 -> {
                list = arrayListOf("우체국", "대신")
            }
            14 -> {
                list = arrayListOf("한덱스")
            }
            else -> {
                Toast.makeText(this, "유효하지 않은 번호입니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.more -> {
                startActivity(Intent(this, RecentActivity::class.java))
            }
        }

        return true
    }
}