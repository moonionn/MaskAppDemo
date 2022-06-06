package com.thishkt.pharmacydemo.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.thishkt.pharmacydemo.PHARMACIES_DATA_URL
import com.thishkt.pharmacydemo.adapter.MainAdapter
import com.thishkt.pharmacydemo.data.Feature
import com.thishkt.pharmacydemo.data.PharmacyInfo
import com.thishkt.pharmacydemo.databinding.ActivityMainBinding
import com.thishkt.pharmacydemo.util.CountyUtil
import com.thishkt.pharmacydemo.util.OkHttpUtil
import com.thishkt.pharmacydemo.util.OkHttpUtil.Companion.mOkHttpUtil
import okhttp3.*

// MainAdapter.IItemClickListener實作
class MainActivity : AppCompatActivity(), MainAdapter.IItemClickListener {

    //定義全域變數
    private lateinit var viewAdapter: MainAdapter
    //ViewManager資料類型是RecyclerView裡的LayoutManager
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var binding: ActivityMainBinding

    // 宣告成變數，因為很多地方會用到，因還會在改變，所以要用var
    // 一開始預設的縣市及鄉鎮
    private var currentCounty: String = "臺東縣"
    private var currentTown: String = "池上鄉"
    private var pharmacyInfo: PharmacyInfo? = null


    // OnCreate後，先把整個畫面畫出來(binding 和 setContent那行)
    // initView()設定RecyclerView
    // getPharmacyData()下載口罩資料(下載完後須將資料傳給容器)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //setContentView(R.layout.activity_main)
        binding  = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //.root就是那個view

        initView()

        //用來取得所有要藥局口罩資料方法
        getPharmacyData()

    }

    private fun initView() {

        // ListView必須利用Adapter將資料載入
        // ArrayAdapter是最基本的方法 >> 宣告一個陣列把要值塞進去，接著ListView就會依照順序顯示出來
        // 此Adapter為縣市的Adapter
        // ArrayAdapter{this, 樣式, 資料來源(CountyUtil中getAllCountiesName()方法)}
        val adapterCounty = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            CountyUtil.getAllCountiesName() // 1.取得所有縣市名稱，並指定到adapterCounty
        )
        // 透過ViewBinding 將Adapter指定到Spinner上面
        binding.spinnerCounty.adapter = adapterCounty // 2.在指定到Spinner
        // 3. >>第一欄位可選擇縣市

        // 監聽「縣市」下拉選單選擇
        // Spinner使用的偵聽方式是setOnItemSelectedListener
        binding.spinnerCounty.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                // 將甚麼放入Spinner
                currentCounty = binding.spinnerCounty.selectedItem.toString()
                // 選擇縣市後去呼叫這個方法
                setSpinnerTown()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        // 監聽「鄉鎮」下拉選單選擇
        binding.spinnerTown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                currentTown = binding.spinnerTown.selectedItem.toString()
                if (pharmacyInfo != null) {
                    updateRecyclerView()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        setDefaultCountyWithTown()

        // 定義 LayoutManager 為 LinearLayoutManager
        // 初始化，是一個線性的Manager
        viewManager = LinearLayoutManager(this)

        // 自定義 Adapter 為 MainAdapter，稍後再定義 MainAdapter 這個類別
        viewAdapter = MainAdapter(this)

        // 定義從佈局當中，拿到 recycler_view 元件，
        // 透過Viewbinding的方式，來使用RecyclerView
        // binding從40行來的
        binding.recyclerView.apply {
            // 透過 kotlin 的 apply 語法糖，設定 LayoutManager 和 Adapter
            layoutManager = viewManager
            adapter = viewAdapter


            // 利用 addItemDecoration() 在 item_view每間藥局的分隔線(divider)
            addItemDecoration(
                DividerItemDecoration(
                    this@MainActivity,
                    DividerItemDecoration.VERTICAL
                )
            )
        }
    }

    // 在 MainActivity.kt 加入根據 Spinner 縣市鄉鎮選單顯示對應列表資料
    private fun setDefaultCountyWithTown() {
        // 預設縣市
        binding.spinnerCounty.setSelection(CountyUtil.getCountyIndexByName(currentCounty))
        setSpinnerTown()
    }

    //根據縣市，來選其他鄉鎮
    // ListView必須利用Adapter將資料載入
    // ArrayAdapter是最基本的方法 >> 宣告一個陣列把要值塞進去，接著ListView就會依照順序顯示出來
    // 此Adapter為鄉鎮的Adapter
    // ArrayAdapter{this, 樣式, 資料來源(CountyUtil中getTownsByCountyName()方法)}
    private fun setSpinnerTown() {
        val adapterTown = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            CountyUtil.getTownsByCountyName(currentCounty) // 1.取得資料，並指定到adapterTown
        )
        // 透過ViewBinding 將Adapter指定到Spinner上面
        binding.spinnerTown.adapter = adapterTown // 2.再指定到Spinner
        // 3. >>第二欄位可選擇縣市

        binding.spinnerTown.setSelection(CountyUtil.getTownIndexByName(currentCounty, currentTown))
    }


    //取得那6000筆資料
    private fun getPharmacyData() {
        //顯示忙碌圈圈(正在跑程式，讓使用者體驗好一點)
        //利用ViewbBinding取activity_main.xml其中一元件(@id+/progressBar)
        //預設屬性是看不到的，透過visiabilit和View.VISIBLE使他顯示
        binding.progressBar.visibility = View.VISIBLE

        //簡化OkHttp使用OkHttpUtil封裝
        //之後呼叫就這麼簡單
        mOkHttpUtil.getAsync(PHARMACIES_DATA_URL, object : OkHttpUtil.ICallback {
            //拿到口罩資料後會在這吐給我們，透過onResponse
            override fun onResponse(response: Response) {
                //將資料存在不可變的變數(val)pharmaciesData裡
                //資料都在body裡，?為資料不為空時
                val pharmaciesData = response.body?.string()

                //將JSON資料轉為GSON
                //fromJson("第一欄位", "第二欄位")
                // 第一欄位(資料來源):透過OkHttp或的的資料;
                // 第二欄位(要轉換為甚麼格式)：帶入我們自定義的類別
                pharmacyInfo = Gson().fromJson(pharmaciesData, PharmacyInfo::class.java)

                //希望將資料顯示在前端(透過runOnUiThread)
                runOnUiThread {
                    //將下載的口罩資料，指定給 MainAdapter
//                    viewAdapter.pharmacyList = pharmacyInfo.features
                    updateRecyclerView()

                    //關閉忙碌圈圈
                    binding.progressBar.visibility = View.GONE
                }
            }

            override fun onFailure(e: okio.IOException) {
                Log.d("HKT", "onFailure: $e")

                //關閉忙碌圈圈，跑完了
                binding.progressBar.visibility = View.GONE
            }
        })
    }


    // 在這接收MainAdapter傳回的資料
    override fun onItemClickListener(data: Feature) {

        // 透過intent跳頁 >> Intent( 當下頁 , 想前往的頁面 )
        val intent = Intent(this, PharmacyDetailActivity::class.java)
        // putExtra表示攜帶資料過去， putExtra( Key值 , 所收到的資料(data) )
        intent.putExtra("data", data)
        // 啟動跳頁(實作 val intent)
        startActivity(intent)
    }

    private fun updateRecyclerView() {

        // 使用.filter(漏斗)來過濾資料，要漏出甚麼資料由.filter{條件}大括號內決定
        // 這邊條件為 縣市正確、鄉鎮正確   ( 選單用)
        val filterData =
            pharmacyInfo?.features?.filter {
                it.properties.county == currentCounty && it.properties.town == currentTown
            }

        if (filterData != null) {
            viewAdapter.pharmacyList = filterData
        }
    }



}
