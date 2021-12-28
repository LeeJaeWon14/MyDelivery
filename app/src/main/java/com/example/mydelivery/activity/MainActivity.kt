package com.example.mydelivery.activity

import android.content.DialogInterface
import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mydelivery.R
import com.example.mydelivery.adapter.MyRecyclerAdapter
import com.example.mydelivery.databinding.ActivityMainBinding
import com.example.mydelivery.network.DeliveryService
import com.example.mydelivery.network.RetroClient
import com.example.mydelivery.network.dto.CarrierDTO
import com.example.mydelivery.network.dto.TrackerDTO
import com.example.mydelivery.room.MyRoomDatabase
import com.example.mydelivery.room.RecentEntity
import com.example.mydelivery.util.MyDateUtil
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
    private var isShared = false
    private var isRecent = false
    private lateinit var manager: InputMethodManager
    private var entity: RecentEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        actionBar?.hide()
        setSupportActionBar(binding.toolbar)

        initUi()

        // init keyboard manager
        manager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

//        getCarriers() //with initSpinner()
        sharedAction()
    }

    private fun initUi() {
        // Views initialize
        binding.apply {
            toolbar.title = MyDateUtil.getDate(MyDateUtil.HANGUEL)
            // create sample code
            btnInputOk.setOnLongClickListener {
                val sampleList = arrayOf("309247673652", getString(R.string.str_sample_code), "645412692946")
                AlertDialog.Builder(this@MainActivity)
                    .setItems(sampleList, object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            edtInput.setText(sampleList[which])
                        }
                    })
                    .setNegativeButton("취소", null)
                    .setCancelable(false)
                    .show()

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
                manager.hideSoftInputFromWindow(currentFocus?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
                entity?.trackNumber = binding.edtInput.text.toString().trim()
                showTracking()
            }
        }
        initSpinner()
    }

    private fun failureMessage(t: Throwable) {
        runOnUiThread(Runnable { Toast.makeText(this@MainActivity, "Response 실패", Toast.LENGTH_SHORT).show() })
        MyLogger.e(t.message.toString())
    }

    private fun initSpinner() {
        val request = RetroClient.getInstance().create(DeliveryService::class.java).getCarriers()
        lateinit var result: List<CarrierDTO>

        // 하단에 UI 작업을 위해 동기처리
        CoroutineScope(Dispatchers.IO).launch {
            request.enqueue(object : Callback<List<CarrierDTO>> {
                override fun onFailure(call: Call<List<CarrierDTO>>, t: Throwable) = failureMessage(t)

                override fun onResponse(
                    call: Call<List<CarrierDTO>>,
                    response: Response<List<CarrierDTO>>
                ) {
                    result = response.body()!!
                    val nameList = ArrayList<String>()
                    for(dto in result) {
                        nameList.add(dto.name)
                        companyCodeList.add(dto.id)
                    }
                    val adapter = ArrayAdapter<String>(this@MainActivity, R.layout.spinner_item, nameList)
                    adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
                    binding.spCarrier.apply {
                        this.adapter = adapter
                        setSelection(0, false)
                    }
                    binding.spCarrier.onItemSelectedListener = this@MainActivity

                    // Call from RecentActivity
                    (intent.getSerializableExtra("recentEntity") as? RecentEntity)?.let {
                        isRecent = true
                        runOnUiThread {
                            binding.run {
                                entity = it
                                edtInput.setText(it.trackNumber)
                                slLayout.visibility = View.VISIBLE
                                for(idx in 0 until nameList.size) {
                                    if(nameList.get(idx).contains(it.companyName)) {
                                        spCarrier.setSelection(idx)
                                        break
                                    }
                                }
                                showTracking()
                            }
                        }
                    } ?: run { entity = RecentEntity(0, "", "", "") }
                }
            })
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Do not use
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        binding.apply {
            slLayout.visibility = View.VISIBLE
            if(!isShared && !isRecent) {
                edtInput.setText("")
                binding.edtInput.requestFocus()
                manager.showSoftInput(binding.edtInput, InputMethodManager.SHOW_IMPLICIT)
            }
            rvTrackList.adapter = null
        }
        isShared = false; isRecent = false

        entity?.company = companyCodeList[position]
        entity?.companyName = binding.spCarrier.selectedItem.toString()
    }

    private fun showTracking() {
        val request = RetroClient.getInstance().create(DeliveryService::class.java).getTracker(entity?.company!!, entity?.trackNumber!!)

        request.enqueue(object : Callback<TrackerDTO> {
            override fun onFailure(call: Call<TrackerDTO>, t: Throwable) = failureMessage(t)

            override fun onResponse(call: Call<TrackerDTO>, response: Response<TrackerDTO>) {
                response.body()?.let {
                    runOnUiThread(Runnable {
                        binding.rvTrackList.layoutManager = LinearLayoutManager(this@MainActivity)
                        binding.rvTrackList.adapter = MyRecyclerAdapter(response.body()!!.progresses)
                    })

                    // save to room
                    CoroutineScope(Dispatchers.IO).launch {
                        // Exist record is ignore.
                        try {
                            val room = MyRoomDatabase.getInstance(this@MainActivity).getRecentDAO()
                            room.insertRecent(entity!!)
                        } catch (e: SQLiteConstraintException) { }
                    }
                } ?: run {
                    Snackbar.make(binding.btnInputOk, getString(R.string.str_invalid_code), Snackbar.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun sharedAction() {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if("text/plain" == intent.type) {
                    intent.getStringExtra(Intent.EXTRA_TEXT).let {
                        binding.edtInput.setText(it)
                        isShared = true
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.more -> startActivity(Intent(this, RecentActivity::class.java))
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