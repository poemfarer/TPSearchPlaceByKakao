package com.farer.tpsearchplacebykakao.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.farer.tpsearchplacebykakao.activities.MainActivity
import com.farer.tpsearchplacebykakao.adapter.PlaceListRecyclerAdapter
import com.farer.tpsearchplacebykakao.databinding.FragmentPlaceListBinding

class PlaceListFragment : Fragment() {

    private val binding: FragmentPlaceListBinding by lazy { FragmentPlaceListBinding.inflate(layoutInflater) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //리사이클러뷰에 MainActivity가 가지고 있는 대량의 장소 정보가 보여지도록
        val ma:MainActivity = activity as MainActivity
        ma.searchPlaceResponse ?: return //아직 서버 로딩이 완료되지 않았다면 return

        binding.recyclerView.adapter= PlaceListRecyclerAdapter(requireContext(), ma.searchPlaceResponse!!.documents)
    }
}