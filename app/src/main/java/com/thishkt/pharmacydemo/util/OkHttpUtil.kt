package com.thishkt.pharmacydemo.util
import okhttp3.*
import okio.IOException
//利用封裝使用參數的方式，帶入其他
//不用每次呼叫都需要打一堆完整的程式碼

class OkHttpUtil {
    private var mOkHttpClient: OkHttpClient? = null

    companion object {
        val mOkHttpUtil: OkHttpUtil by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            OkHttpUtil()
        }
    }

    init {
        //Part 1: 宣告 OkHttpClient(初始化)
        mOkHttpClient = OkHttpClient().newBuilder().build()
    }
    //Get 非同步
    //我們只用到GET，所以只載入GET功能
    //getAsync(自己命名的function)
    //Android只能用非同步的方式
    fun getAsync(url: String, callback: ICallback) {
        //Part 2: 宣告 Request，要求要連到指定網址
        //先Request
        val request = with(Request.Builder()) {
            url(url)
            //再get
            get()
            build()
        }

        //Part 3: 宣告 Call
        val call = mOkHttpClient?.newCall(request)

        //執行 Call 連線後，採用 enqueue 非同步方式，獲取到回應的結果資料
        call?.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure(e)
                //透過callback把資料丟回去
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                callback.onResponse(response)
            }
        })
    }
    //透過這個INTERFACE，把錯誤或成功的訊息接收回來
    interface ICallback {
        fun onResponse(response: Response)
        fun onFailure(e: IOException)
    }
}