package com.thishkt.pharmacydemo.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.thishkt.pharmacydemo.databinding.InfoWindowBinding

// 改寫原本的視窗，繼承GoogleMap.InfoWindowAdapter
class MyInfoWindowAdapter(_context: Context) : GoogleMap.InfoWindowAdapter {

    private val context = _context

    private fun render(marker: Marker, infoWindowBinding: InfoWindowBinding) {
        val mask = marker.snippet.toString().split(",")
        infoWindowBinding.tvName.text = marker.title
        infoWindowBinding.tvAdultAmount.text = mask[0]
        infoWindowBinding.tvChildAmount.text = mask[1]

    }

    // 覆寫在GoogleMap.InfoWindowAdapter的這兩個方法

    // 拿到info_window的layout
    override fun getInfoContents(marker: Marker): View {
        val infoWindowBinding =
            InfoWindowBinding.inflate(context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)

        // 渲染畫面(maker(圖釘), infoWindowBinding)
        render(marker, infoWindowBinding)
        return infoWindowBinding.root

    }

    // 這用不到所以return null
    override fun getInfoWindow(marker: Marker): View? {
        return null
    }
}