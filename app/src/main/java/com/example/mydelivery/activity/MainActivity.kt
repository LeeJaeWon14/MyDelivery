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
import com.example.mydelivery.R
import com.example.mydelivery.adapter.MyRecyclerAdapter
import com.example.mydelivery.databinding.ActivityMainBinding
import com.example.mydelivery.network.DeliveryService
import com.example.mydelivery.network.RetroClient
import com.example.mydelivery.network.dto.CarrierDTO
import com.example.mydelivery.network.dto.TrackerDTO
import com.example.mydelivery.util.MyLogger
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private lateinit var binding : ActivityMainBinding
    private val companyCodeList = ArrayList<String>()
    private val companyInfo = HashMap<String, String>()
    private var isShared = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        actionBar?.hide()
        setSupportActionBar(binding.toolbar)

        // Views initialize
        binding.apply {
            // create sample code
            btnInputOk.setOnLongClickListener {
                edtInput.setText(getString(R.string.str_sample_code))
                false
            }

            // SwipeRefreshLayout init
            slLayout.apply {
                setProgressBackgroundColorSchemeColor(getColor(R.color.purple_500))
                setColorSchemeColors(Color.WHITE)
                setOnRefreshListener {
                    showTracking()
                    Snackbar.make(binding.btnInputOk, getString(R.string.str_update_tracking), Snackbar.LENGTH_SHORT).show()
                    binding.slLayout.isRefreshing = false
                }
            }

            btnInputOk.setOnClickListener {
                if(binding.edtInput.text.toString() == "") {
                    Snackbar.make(it, getString(R.string.str_invalid_code), Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                showTracking()
            }
        }

        getCarriers() //with initSpinner()

        when (intent.action) {
            Intent.ACTION_SEND -> {
                if("text/plain" == intent.type) {
                    handleSendText(intent)
                }
            }
        }
    }

    private fun getCarriers() {
        val request = RetroClient.getInstance().create(DeliveryService::class.java).getCarriers()

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

    private fun initSpinner(result : List<CarrierDTO>) {
        val nameList = ArrayList<String>()
        for(dto in result) {
            nameList.add(dto.name)
            companyCodeList.add(dto.id)
        }
        val adapter = ArrayAdapter<String>(this, R.layout.spinner_item, nameList)
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
        binding.spCarrier.apply {
            this.adapter = adapter
            setSelection(0, false)
        }
        binding.spCarrier.onItemSelectedListener = this
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Do not use
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        binding.apply {
            llLayout.visibility = View.VISIBLE
            if(!isShared) edtInput.setText("")
            rvTrackList.adapter = null
        }
        isShared = false

        val code = companyCodeList[position]
        companyInfo["name"] = code
    }

    private fun showTracking() {
        companyInfo["number"] = binding.edtInput.text.toString().trim()
        val request = RetroClient.getInstance().create(DeliveryService::class.java).getTracker(companyInfo["name"]!!, companyInfo["number"]!!)

        request.enqueue(object : Callback<TrackerDTO> {
            override fun onFailure(call: Call<TrackerDTO>, t: Throwable) {
                runOnUiThread(Runnable {
                    Toast.makeText(this@MainActivity, "Response 실패", Toast.LENGTH_SHORT).show()
                })
                MyLogger.e(t.message.toString())
            }

            override fun onResponse(call: Call<TrackerDTO>, response: Response<TrackerDTO>) {
                response.body()?.let {
                    runOnUiThread(Runnable {
                        MyLogger.i("response >> ${response.body()}")
                        binding.rvTrackList.layoutManager = LinearLayoutManager(this@MainActivity)
                        binding.rvTrackList.adapter = MyRecyclerAdapter(response.body()!!.progresses)
                    })
                } ?: run {
                    Snackbar.make(binding.btnInputOk, getString(R.string.str_invalid_code), Snackbar.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun handleSendText(intent : Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT).let {
            binding.edtInput.setText(it)
            isShared = true

            val nameList = arrayListOf<String>()
            val request = RetroClient.getInstance().create(DeliveryService::class.java).getCarriers()
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

    private var time : Long = 0
    override fun onBackPressed() { //뒤로가기 클릭 시 종료 메소드
        if(System.currentTimeMillis() - time >= 2000) {
            time = System.currentTimeMillis()
            Toast.makeText(this@MainActivity, "한번 더 누르면 종료합니다", Toast.LENGTH_SHORT).show()
        }
        else if(System.currentTimeMillis() - time < 2000) {
            this.finishAffinity()
        }
    }
}