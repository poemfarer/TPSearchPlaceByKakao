package com.farer.tpsearchplacebykakao.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.farer.tpsearchplacebykakao.activities.PlaceDetailActivity
import com.farer.tpsearchplacebykakao.data.Place
import com.farer.tpsearchplacebykakao.databinding.RecyclerItemListFragmentBinding
import com.google.gson.Gson

class PlaceListRecyclerAdapter(val context: Context, val documents: List<Place>) : Adapter<PlaceListRecyclerAdapter.VH>() {

    inner class VH(val binding: RecyclerItemListFragmentBinding) : ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val layoutInflater= LayoutInflater.from(context)
        val binding= RecyclerItemListFragmentBinding.inflate(layoutInflater, parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int {
        return documents.size
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val place: Place = documents[position]

        holder.binding.tvPlaceName.text= place.place_name
        holder.binding.tvAddress.text= if (place.road_address_name=="") place.address_name else place.road_address_name
        holder.binding.tvDistance.text= "${place.distance}m"

        //아이템뷰를 클릭했을 때 상세정보 페이지 url을 보여주는 화면으로 이동
        holder.binding.root.setOnClickListener {
            val intent= Intent(context, PlaceDetailActivity::class.java)

            //장소 정보에 대한 데이터를 추가로 보내기 [ 객체는 추가 데이터로 전송 불가 --> json 문자열로 변환 ]
            val gson= Gson()
            val s:String = gson.toJson(place)
            intent.putExtra("place", s)


            context.startActivity(intent)
        }
    }
}