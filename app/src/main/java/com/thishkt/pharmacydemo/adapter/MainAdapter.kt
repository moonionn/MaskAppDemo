package com.thishkt.pharmacydemo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thishkt.pharmacydemo.data.Feature
import com.thishkt.pharmacydemo.databinding.ItemViewBinding

//Adapter容器寫法


//新增MainAdapter 繼承 RecyclerView.Adapter<我們自己的畫面>
//(MainAdapter.MyViewHolder(MyViewHolder(我們自己定義)，在MainAdapter裡))
class MainAdapter(private val itemClickListener: IItemClickListener) :

    //RecycvleView.Adapter<要怎麼樣的View>
    RecyclerView.Adapter<MainAdapter.MyViewHolder>() {

    //設定等等項目會有幾個
    //List<資料類型(featrure那一列)> >> 一開始先設空的
    var pharmacyList: List<Feature> = emptyList()
        set(value) {    //value表示pharmacyList
            field = value
            notifyDataSetChanged()  //如果資料有指定近來，就可以通知RecycleView去做資料更新
        }


    //三個override是用來定義MainAdapter


    //現在有一樣式(item_view)，透過ViewBinding的寫法
    //onCreateViewHolder用來判斷樣哪個樣式、資料類型(但目前只有一種資料型態(item_view))
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        //拿到view後存到此變數(itemViewBinding)
        val itemViewBinding =
            //固定寫法(from=來自於哪裡)
            ItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        //onCreateViewHolder要傳回MyViewHolder，要有回傳直
        return MyViewHolder(itemViewBinding)
    }

    // position為位置(一排就是一個)
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        // 因為 ViewHolder 會重複使用，
        // 我們要在這個 function 依據 position
        // 把正確的資料跟 ViewHolder 綁定在一起。

        // tvName=item_view的id，.text為顯示的文字，每一列顯示每個pharmacyList裡properties欄的name(藥局名稱)
        holder.itemViewBinding.tvName.text = pharmacyList[position].properties.name
        // tvAdultAmount=item_view的id，.text為顯示的文字，每一列顯示每個pharmacyList裡properties欄的mask_adult(成人口罩數)，將其字串化
        holder.itemViewBinding.tvAdultAmount.text = pharmacyList[position].properties.mask_adult.toString()
        holder.itemViewBinding.tvChildAmount.text = pharmacyList[position].properties.mask_child.toString()

        // 點擊事件
        // 透過這介面回傳給MainActivity
        holder.itemViewBinding.layoutItem.setOnClickListener {
            itemClickListener.onItemClickListener(pharmacyList[position])
        }
    }

    override fun getItemCount(): Int {
        // 回傳整個 Adapter 包含幾筆資料。

        return pharmacyList.size
        //return list的大小
    }


    //定義ViewBinding
    class MyViewHolder(val itemViewBinding: ItemViewBinding) :
        //繼承RecycleView的ViewHolder
        RecyclerView.ViewHolder(itemViewBinding.root)


    // 定義 CallBack 介面
    // 項目被點擊時，希望回傳資料回去(項目監聽事件)
    interface IItemClickListener {
        fun onItemClickListener(data: Feature)
    }

}
