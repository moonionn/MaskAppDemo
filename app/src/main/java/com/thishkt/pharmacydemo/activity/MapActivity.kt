package com.thishkt.pharmacydemo.activity

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.get
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.thishkt.pharmacydemo.PHARMACIES_DATA_URL
import com.thishkt.pharmacydemo.R
import com.thishkt.pharmacydemo.REQUEST_ENABLE_GPS
import com.thishkt.pharmacydemo.REQUEST_LOCATION_PERMISSION
import com.thishkt.pharmacydemo.adapter.MyInfoWindowAdapter
import com.thishkt.pharmacydemo.data.PharmacyInfo
import com.thishkt.pharmacydemo.databinding.ActivityMapBinding
import com.thishkt.pharmacydemo.util.OkHttpUtil
import okhttp3.Response


class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {


    private lateinit var binding: ActivityMapBinding

    // 記錄獲取到權限
    private var locationPermissionGranted = false
    private var mCurrLocationMarker: Marker? = null

    private var pharmacyInfo: PharmacyInfo? = null

    private lateinit var mContext: Context
    private var googleMap: GoogleMap? = null
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    //台北101
    private val defaultLocation = LatLng(25.0338483, 121.5645283)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_map)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.navView.setNavigationItemSelectedListener { item->
            when (item.itemId) {
                R.id.nav_home -> {
                    //button1
                    startActivity(Intent(this, MainActivity::class.java))
                    // handle click
                    true
                }

                else -> false
            }
        }

        mContext = this
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        getPharmacyData()


    }


    // 獲取藥局資料
    private fun getPharmacyData() {
        //顯示忙碌圈圈
        binding.progressBar.visibility = View.VISIBLE

        // 將口罩資料下載回來
        OkHttpUtil.mOkHttpUtil.getAsync(PHARMACIES_DATA_URL, object : OkHttpUtil.ICallback {
            override fun onResponse(response: Response) {
                val pharmaciesData = response.body?.string()

                Log.d("QQQ", "pharmaciesData:$pharmaciesData")

                // 轉換為GSON(做存取比較方便)
                // 將資料放到pharmacyInfo
                pharmacyInfo = Gson().fromJson(pharmaciesData, PharmacyInfo::class.java)

                // 把要處理的UI程序放到runOnUiThread{}
                runOnUiThread {
                    //關閉忙碌圈圈
                    binding.progressBar.visibility = View.GONE
                    addAllMaker() //在地圖上所有Maker加上去(跟UI有關)
                }
            }

            override fun onFailure(e: okio.IOException) {
                Log.d("HKT", "onFailure: $e")

                //關閉忙碌圈圈
                binding.progressBar.visibility = View.GONE
            }
        })
    }

    // 檢查權限是否有權限
    // 如有，locationPermissionGranted紀錄為true，執行checkGPSState()
    // 如無，跳到requestLocationPermission()
    private fun getLocationPermission() {

        // PERMISSION_GRANTED是否被同意
        // 用ActivityCompat裡的checkSelfPermission方法
        // checkSelfPermission(this, 想檢查權限的名字)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // 記錄獲取到權限
            locationPermissionGranted = true
            checkGPSState()
        } else {
            //詢問要求獲取權限
            requestLocationPermission()
        }
    }

    //檢查GPS狀態
    private fun checkGPSState() {
        val locationManager = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // 假如沒被開啟
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder(mContext)
                .setTitle("GPS 尚未開啟")
                .setMessage("使用此功能需要開啟 GSP 定位功能")
                .setPositiveButton("前往開啟",
                    DialogInterface.OnClickListener { _, _ ->
                        startActivityForResult(
                            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_ENABLE_GPS
                        )
                    })
                .setNegativeButton("取消", null)
                .show()
        } else {
            Toast.makeText(this, "已獲取到位置權限且GPS已開啟，可以準備開始獲取經緯度", Toast.LENGTH_SHORT).show()
            getDeviceLocation()
        }
    }

    // 獲取經緯度的function
    private fun getDeviceLocation() {
        try {
            // 利用locationPermissionGranted確認是否拿到權限
            if (locationPermissionGranted
            ) {
                val locationRequest = LocationRequest()
                // 定位準確度
                locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                // 更新頻率(1秒)
                locationRequest.interval = 1000
                // 更新次數，若沒設定，會持續更新
                // locationRequest.numUpdates = 1
                mFusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult?) {
                            // 檢查locationResult。Null > return(不會往下執行)
                            locationResult ?: return
                            Log.d(
                                "QQQ",
                                "緯度:${locationResult.lastLocation.latitude} , 經度:${locationResult.lastLocation.longitude} "
                            )
                        }
                    },
                    null
                )

            } else {
                //沒拿到權限，去詢問權限
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    // 詢問權限
    private fun requestLocationPermission() {
        // 用ActivityCompat裡的shouldShowRequestPermissionRationale方法
        // shouldShowRequestPermissionRationale(this, 想檢查權限的名字)
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            AlertDialog.Builder(this)
                .setMessage("需要位置權限") // 想顯示甚麼訊息給使用者看
                    // 確定的話，用requestPermission詢問使用者獲取權限
                // ActivityCompat.requestPermissions(this, 想要的權限, CallBack(自己定義))
                .setPositiveButton("確定") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_LOCATION_PERMISSION
                    )
                }
                // 取消的話，再call一次此方法，詢問使用者
                .setNegativeButton("取消") { _, _ -> requestLocationPermission() }
                .show()
        } else {
            // 可以詢問使用者獲取權限 == 按下確定
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION
            )
        }
    }

    // 使用者做任何一項選擇都會呼叫這方法，看是不是我們定義的requestCode。如果是，就去做對應的處理
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // 已獲取權限，checkGPS開沒
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                // 假設grantResults不是空的
                if (grantResults.isNotEmpty()) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        //已獲取到權限
                        locationPermissionGranted = true
                        checkGPSState()
                    } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        ) {
                            //權限被永久拒絕
                            Toast.makeText(this, "位置權限已被關閉，功能將會無法正常使用", Toast.LENGTH_SHORT).show()

                            AlertDialog.Builder(this)
                                .setTitle("開啟位置權限")
                                .setMessage("此應用程式，位置權限已被關閉，需開啟才能正常使用")

                                    // 按確定的話，可以自動導到設定頁(ACTION_LOCATION_SOURCE_SETTINGS)
                                .setPositiveButton("確定") { _, _ ->
                                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                    startActivityForResult(intent, REQUEST_LOCATION_PERMISSION)
                                }
                                .setNegativeButton("取消") { _, _ -> requestLocationPermission() }
                                .show()
                        } else {
                            //權限被拒絕(暫時被拒絕)
                            Toast.makeText(this, "位置權限被拒絕，功能將會無法正常使用", Toast.LENGTH_SHORT).show()
                            requestLocationPermission()
                        }
                    }
                }
            }
        }
    }

    //當GPS狀態未開啟時，會導引使用者去設定頁開啟GPS
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                getLocationPermission()
            }
            REQUEST_ENABLE_GPS -> {
                checkGPSState()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                defaultLocation, 15f
            )
        )
        googleMap?.setInfoWindowAdapter(MyInfoWindowAdapter(mContext))
        googleMap?.setOnInfoWindowClickListener(this)

//        getLocationPermission()
    }

    // 用forEach方法將所有藥局資料一個一個取出來
    private fun addAllMaker() {
        pharmacyInfo?.features?.forEach { feature ->
            val pinMarker = googleMap?.addMarker(  // 使用googleMap裡的addMaker加Maker
                MarkerOptions()
                    .position(  // 要加入Maker的位置
                        LatLng(  // 透過LatLng加入經緯度(從CountyUtil取得)
                            feature.geometry.coordinates[1],
                            feature.geometry.coordinates[0],
                        )
                    )
                    .title(feature.properties.name)  // title顯示藥局名稱(properties.name)
                    .snippet(    // 顯示藥局成人小孩口罩數量
                        "${feature.properties.mask_adult}," +
                                "${feature.properties.mask_child}"
                    )
            )
        }

    }


    // 點擊動作
    // 帶著參數Maker過來
    override fun onInfoWindowClick(marker: Marker?) {
        // 比對maker的title與pharmacyInfo的feature的properties.name比對
        marker?.title?.let { title ->
//            Log.d("HKT", title)

            // 找出對應藥局的資料
            // 用filter(漏斗)方法，比對找到該藥局
            // 結果指定到filterData，就能從一堆藥局中找到該藥局資料
            val filterData =
                pharmacyInfo?.features?.filter {
                    it.properties.name == (title)
                }

            // filterData有資料，跳轉至詳細頁PharmacyDetailActivity
            if (filterData?.size!! > 0) {
                val intent = Intent(this, PharmacyDetailActivity::class.java)
                // 透過intent.putExtra方式，將資料到至下一頁
                intent.putExtra("data", filterData.first())
                startActivity(intent)
            } else {
                Log.d("QQQ", "查無資料")
            }
        }
    }
}