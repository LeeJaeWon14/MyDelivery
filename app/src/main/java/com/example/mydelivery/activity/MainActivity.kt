package com.example.mydelivery.activity

import android.app.ActivityOptions
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
import kotlin.collections.ArrayList

class MainActivity : BaseActivity(), AdapterView.OnItemSelectedListener {
    private lateinit var binding : ActivityMainBinding
    private val companyCodeList = ArrayList<String>()
    private var isShared = false
    private var isRecent = false
    private lateinit var manager: InputMethodManager
    private var entity: RecentEntity? = null
    private var isTry = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // init keyboard manager
        manager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        initUi()
    }

    private fun initUi() {
        // Views initialize
        binding.apply {
            actionBar?.hide()
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            tvToolbarTitle.text = MyDateUtil.getDate(MyDateUtil.HANGUEL)

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
                if(binding.edtInput.text.isEmpty()) {
                    Snackbar.make(it, getString(R.string.str_invalid_code), Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                manager.hideSoftInputFromWindow(currentFocus?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
                try {
                    val inputNumber: Long = binding.edtInput.text.toString().trim().toLong()
                    entity?.trackNumber = inputNumber.toString()
                } catch (e: NumberFormatException) {
//                    Toast.makeText(this@MainActivity, getString(R.string.str_number_format_exception), Toast.LENGTH_SHORT).show()
                    Snackbar.make(it, getString(R.string.str_number_format_exception), Snackbar.LENGTH_SHORT).show()
                    requestInput()
                    return@setOnClickListener
                }

                showTracking()
            }
        }
        initSpinner()
    }

    private fun requestInput() {
        binding.edtInput.requestFocus()
        manager.showSoftInput(binding.edtInput, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun failureMessage(t: Throwable) {
        CoroutineScope(Dispatchers.Main).launch {
            when(t.message) {
                getString(R.string.str_not_connected_network_for_tracker) -> {
                    Toast.makeText(this@MainActivity, getString(R.string.str_need_connect_network), Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this@MainActivity, getString(R.string.str_unexpected_error_message), Toast.LENGTH_SHORT).show()
                }
            }
        }
        MyLogger.e(t.message.toString())
    }

    private fun initSpinner() {
        val request = RetroClient.getInstance().create(DeliveryService::class.java).getCarriers()
        lateinit var result: List<CarrierDTO>

        // ????????? UI ????????? ?????? ????????????
        CoroutineScope(Dispatchers.IO).launch {
            request.enqueue(object : Callback<List<CarrierDTO>> {
                override fun onFailure(call: Call<List<CarrierDTO>>, t: Throwable) {
                    // One more try Rest call when failed onFailure.
                    if(!isTry) {
                        call.clone().enqueue(this)
                        isTry = true
                    }
                    else { failureMessage(t) }
                }

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
                        onItemSelectedListener = this@MainActivity
                    }

                    // Started by RecentActivity
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

                    sharedAction(nameList)
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
                requestInput()
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
                        binding.rvTrackList.adapter = MyRecyclerAdapter(it.progresses)
                    })

                    // save to room
                    CoroutineScope(Dispatchers.IO).launch {
                        // Exist record is ignore.
                        try {
                            val list = MyRoomDatabase.getInstance(this@MainActivity).getRecentDAO().distinctCheck(entity?.trackNumber!!)
                            if(list.isEmpty()) {
                                MyRoomDatabase.getInstance(this@MainActivity).getRecentDAO()
                                    .insertRecent(entity!!)
                            }
                        } catch (e: SQLiteConstraintException) { }
                    }
                } ?: run {
                    Snackbar.make(binding.btnInputOk, getString(R.string.str_invalid_code), Snackbar.LENGTH_SHORT).show()
                    requestInput()
                }
            }
        })
    }

    private fun sharedAction(nameList: ArrayList<String>) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if("text/plain" == intent.type) {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                        binding.edtInput.setText(it)
                        isShared = true

                        val compList = checkDeliverNumber(it)
                        AlertDialog.Builder(this)
                            .setItems(compList?.toTypedArray(), object : DialogInterface.OnClickListener {
                                override fun onClick(dialog: DialogInterface?, which: Int) {
                                    for(idx in nameList.indices) {
                                        if(nameList[idx].contains(compList!![which])) {
                                            binding.spCarrier.setSelection(idx)
                                            break
                                        }
                                    }
                                    binding.btnInputOk.performClick()
                                }
                            })
                            .setNegativeButton("??????", object : DialogInterface.OnClickListener {
                                override fun onClick(dialog: DialogInterface?, which: Int) {
                                    finishAffinity()
                                }
                            })
                            .show()
                    }
                }
            }
        }
    }

    private fun checkDeliverNumber(number : String) : ArrayList<String>? {
        return when(number.length) {
            9 -> { arrayListOf("??????", "??????", "??????", "USPS", "EMS") }
            10 -> { arrayListOf("??????", "??????", "??????", "CU", "CVS", "?????????", "USPS", "EMS", "DHL", "??????") }
            11 -> { arrayListOf("??????", "??????", "??????") }
            12 -> { arrayListOf("Fedex", "??????", "??????", "??????", "CU", "CVS", "????????????") }
            13 -> { arrayListOf("?????????", "??????") }
            14 -> { arrayListOf("?????????") }
            else -> {
                Toast.makeText(this, "???????????? ?????? ???????????????.", Toast.LENGTH_SHORT).show()
                null
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.more -> startActivity(
                Intent(this, RecentActivity::class.java),
                ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
            )
        }

        return true
    }

    private var time : Long = 0
    override fun onBackPressed() { //???????????? ?????? ??? ?????? ?????????
        if(System.currentTimeMillis() - time >= 2000) {
            time = System.currentTimeMillis()
            Toast.makeText(this@MainActivity, "?????? ??? ????????? ???????????????", Toast.LENGTH_SHORT).show()
        }
        else if(System.currentTimeMillis() - time < 2000) {
            this.finishAffinity()
        }
    }
}