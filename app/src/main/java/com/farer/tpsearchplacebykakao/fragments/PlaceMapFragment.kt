package com.farer.tpsearchplacebykakao.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.farer.tpsearchplacebykakao.R
import com.farer.tpsearchplacebykakao.activities.MainActivity
import com.farer.tpsearchplacebykakao.activities.PlaceDetailActivity
import com.farer.tpsearchplacebykakao.data.Place
import com.farer.tpsearchplacebykakao.databinding.FragmentPlaceListBinding
import com.farer.tpsearchplacebykakao.databinding.FragmentPlaceMapBinding
import com.google.gson.Gson
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.camera.CameraUpdate
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.mapwidget.InfoWindowOptions
import com.kakao.vectormap.mapwidget.component.GuiLayout
import com.kakao.vectormap.mapwidget.component.GuiText
import com.kakao.vectormap.mapwidget.component.Orientation

class PlaceMapFragment : Fragment() {

    private val binding: FragmentPlaceMapBinding by lazy { FragmentPlaceMapBinding.inflate(layoutInflater) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //카카오 지도 start
        binding.mapView.start(mapReadyCallback)
    }

    private val mapReadyCallback: KakaoMapReadyCallback = object : KakaoMapReadyCallback(){
        override fun onMapReady(kakaoMap: KakaoMap) {

            //현재 내 위치를 지도의 중심 위치로 설정
            val latitude: Double= (activity as MainActivity).myLocation?.latitude ?: 37.5666
            val longitude: Double= (activity as MainActivity).myLocation?.longitude ?: 126.9782
            val myPos: LatLng= LatLng.from(latitude, longitude)

            //내 위치로 지도 카메라 이동
            val cameraUpdate: CameraUpdate= CameraUpdateFactory.newCenterPosition(myPos, 16)
            kakaoMap.moveCamera(cameraUpdate)

            //내 위치 마커(라벨) 추가
            val labelOptions: LabelOptions= LabelOptions.from(myPos).setStyles(R.drawable.ic_mypin) //Vector 이미지 불가
            //라벨이 그려진 레이어 객체 소환
            val labelLayer: LabelLayer= kakaoMap.labelManager!!.layer!!
            //라벨 레이어에 라벨 추가
            labelLayer.addLabel(labelOptions)
            //--------------------------------------------------------------------------------------

            //주변 검색 장소에 마커 추가
            val placeList: List<Place>? = (activity as MainActivity).searchPlaceResponse?.documents
            placeList?.forEach {
                //마커(라벨) 옵션 객체 생성
                val pos= LatLng.from(it.y.toDouble(), it.x.toDouble())
                val options= LabelOptions.from(pos).setStyles(R.drawable.ic_pin).setTexts(it.place_name, "${it.distance}m")
                kakaoMap.labelManager!!.layer!!.addLabel(options)
            }

            //라벨 클릭에 반응
            kakaoMap.setOnLabelClickListener { kakaoMap, layer, label ->

                label.apply {
                    //정보창[infoWindow] 보여주기
                    val layout= GuiLayout(Orientation.Vertical)
                    layout.setPadding(16, 16, 16, 16)
                    layout.setBackground(R.drawable.base_msg, true)

                    texts.forEach {
                        val guiText= GuiText(it)
                        guiText.setTextSize(30)
                        guiText.setTextColor(Color.WHITE)
                        layout.addView(guiText)
                    }

                    //정보창[infoWindow] 객체 만들기
                    val options: InfoWindowOptions= InfoWindowOptions.from(position)
                    options.body= layout
                    options.setBodyOffset(0f, -10f)

                    kakaoMap.mapWidgetManager!!.infoWindowLayer.removeAll()
                    kakaoMap.mapWidgetManager!!.infoWindowLayer.addInfoWindow(options)
                }

            }

            //정보창 클릭에 반응
            kakaoMap.setOnInfoWindowClickListener { kakaoMap, infoWindow, guiId ->
                //장소에 대한 상세 소개 웹페이지를 보여주는 화면으로 이동
                val intent= Intent(requireContext(), PlaceDetailActivity::class.java)

                //클릭한 장소에 대한 정보를 json 문자열로 변환하여 전달
                val place: Place= infoWindow.tag as Place
                val json: String= Gson().toJson(place)
                intent.putExtra("place", json)

                startActivity(intent)
            }
        }

    }
}