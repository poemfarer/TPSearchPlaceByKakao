package com.farer.tpsearchplacebykakao.activities

import android.Manifest
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.farer.tpsearchplacebykakao.R
import com.farer.tpsearchplacebykakao.data.KakaoSearchPlaceResponse
import com.farer.tpsearchplacebykakao.data.Place
import com.farer.tpsearchplacebykakao.data.PlaceMeta
import com.farer.tpsearchplacebykakao.databinding.ActivityMainBinding
import com.farer.tpsearchplacebykakao.fragments.PlaceFavorFragment
import com.farer.tpsearchplacebykakao.fragments.PlaceListFragment
import com.farer.tpsearchplacebykakao.fragments.PlaceMapFragment
import com.farer.tpsearchplacebykakao.network.RetrofitHelper
import com.farer.tpsearchplacebykakao.network.RetrofitApiService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import retrofit2.Callback
import retrofit2.Call
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    //버전3 수정

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    //카카오 검색에 필요한 요청 데이터 : query(검색어), x(경도-longitude), y(위도-latitude)
    //1. 검색 장소명
    var searchQuery: String= "화장실" //앱 초기 검색어 - 내 주변 개방 화장실
    //2. 현재 내 위치 정보 객체 (위도, 경도 정보를 멤버로 보유)
    var myLocation:Location?= null

    //[Google Fused Location API 사용 : play-services-location]
    val locationProviderClient: FusedLocationProviderClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    //Kakao search API 응답 결과 객체 참조변수
    var searchPlaceResponse: KakaoSearchPlaceResponse?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        openOrCreateDatabase("place", Activity.MODE_PRIVATE, null)

        //처음 보여질 Fragment 화면에 붙이기
        supportFragmentManager.beginTransaction().add(R.id.container_fragment, PlaceListFragment()).commit()

        //bnv의 선택에 따라 Fragment를 동적으로 교체
        binding.bnv.setOnItemSelectedListener{

            when(it.itemId){
                R.id.menu_bnv_list-> supportFragmentManager.beginTransaction().replace(R.id.container_fragment, PlaceListFragment()).commit()
                R.id.menu_bnv_map-> supportFragmentManager.beginTransaction().replace(R.id.container_fragment, PlaceMapFragment()).commit()
                R.id.menu_bnv_favor-> supportFragmentManager.beginTransaction().replace(R.id.container_fragment, PlaceFavorFragment()).commit()
                R.id.menu_bnv_option-> Toast.makeText(this, "option", Toast.LENGTH_SHORT).show()
            }

            true //OnItemSelectedListener의 추상 메소드는 리턴값을 가지고 있음. SAM 변환시 return 키워드 사용 X
        }

        //bnv 아이템 선택 리플 효과의 범위를 제한하지 않기 위해 배경 영역 없애기
        binding.bnv.background= null

        //소프트 키보드의 검색 버튼을 클릭하였을 때
        binding.etSearch.setOnEditorActionListener { v, actionId, event ->
            searchQuery= binding.etSearch.text.toString()
            //키워드로 장소 검색 요청
            searchPlaces()

            //액션 버튼이 클릭되었을 때 여기서 모든 액션을 소모하지 않았다는 뜻
            false
        }

        //특정 키워드 단축 choice 버튼들에 리스너 처리하는 코드를 별도의 메소드에
        setChoiceButtonsListener()

        //위치 정보 제공에 대한 퍼미션 체크
        val permissionState: Int= checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissionState==PackageManager.PERMISSION_DENIED){
            //퍼미션을 요청하는 다이얼로그를 보여주고 그 결과를 받아오는 작업을 대신해주는 대행사 이용
            permissionResultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }else{
            //위치 정보 수집이 허가되어 있다면 위치 정보 얻어오기 작업을 수행
            requestMyLocation()
        }

        //내 위치 갱신 버튼
        binding.toolbar.setNavigationOnClickListener { requestMyLocation() }

        //새로고침 버튼
        binding.fabRefresh.setOnClickListener {
            requestMyLocation()
            ObjectAnimator.ofFloat(it, "translationY", -100f).start()
            ObjectAnimator.ofFloat(it, "rotationX", 360f).start()
        }



    }//onCreate method

    //퍼미션 요청 및 결과 받아오기 작업을 대신하는 대행사 등록
    val permissionResultLauncher: ActivityResultLauncher<String> = registerForActivityResult(ActivityResultContracts.RequestPermission()){
        if (it) requestMyLocation()
        else Toast.makeText(this, "내 위치 정보를 제공하지 않아, 검색 기능 사용이 제한됩니다.", Toast.LENGTH_SHORT).show()
    }

    //현재 위치를 얻어오는 작업 요청 코드가 있는 기능메소드
    private fun requestMyLocation(){

        //요청 객체 생성
        val request: LocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).build()

        //실시간 위치정보 갱신 요청 - 퍼미션 체크 코드가 필요
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) { return
        }
        locationProviderClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    //위치 정보 갱신 때마다 발동하는 콜백 객체
    private val locationCallback= object : LocationCallback(){
        override fun onLocationResult(p0: LocationResult) {
            super.onLocationResult(p0)

            myLocation= p0.lastLocation

            //위치 탐색이 종료되었으므로 내 위치 정보 업데이트 중지
            locationProviderClient.removeLocationUpdates(this) //this: locationCallback 객체

            //위치 정보를 얻었으면 키워드 장소 검색 작업 시작
            searchPlaces()
        }
    }

    private fun setChoiceButtonsListener(){
        binding.layoutChoice.choice01.setOnClickListener { clickChoice(it) }
        binding.layoutChoice.choice02.setOnClickListener { clickChoice(it) }
        binding.layoutChoice.choice03.setOnClickListener { clickChoice(it) }
        binding.layoutChoice.choice04.setOnClickListener { clickChoice(it) }
        binding.layoutChoice.choice05.setOnClickListener { clickChoice(it) }
        binding.layoutChoice.choice06.setOnClickListener { clickChoice(it) }
        binding.layoutChoice.choice07.setOnClickListener { clickChoice(it) }
        binding.layoutChoice.choice08.setOnClickListener { clickChoice(it) }
        binding.layoutChoice.choice09.setOnClickListener { clickChoice(it) }
        binding.layoutChoice.choice10.setOnClickListener { clickChoice(it) }
        binding.layoutChoice.choice11.setOnClickListener { clickChoice(it) }

    }

    //멤버변수(property)
    var choiceID= R.id.choice_01

    private fun clickChoice(view:View){

        //기존에 선택되었던 ImageView를 찾아서 배경 이미지를 선택되지 않은 하얀색 원그림으로 변경
        findViewById<ImageView>(choiceID).setBackgroundResource(R.drawable.bg_choice)

        //현재 클릭한 ImageView의 배경을 선택된 회색 원그림으로 변경
        view.setBackgroundResource(R.drawable.bg_choice_selected)

        //클릭한 뷰의 id를 저장
        choiceID= view.id

        when(choiceID){
            R.id.choice_01->searchQuery="화장실"
            R.id.choice_02->searchQuery="약국"
            R.id.choice_03->searchQuery="주유소"
            R.id.choice_04->searchQuery="공원"
            R.id.choice_05->searchQuery="주차장"
            R.id.choice_06->searchQuery="전기차 충전소"
            R.id.choice_07->searchQuery="편의점"
            R.id.choice_08->searchQuery="기타"
            R.id.choice_09->searchQuery="병원"
            R.id.choice_10->searchQuery="ATM"
            R.id.choice_11->searchQuery="기타2"
        }

        //바뀐 검색 장소명으로 검색 요청
        searchPlaces()

        //검색창에 글자가 있다면 삭제
        binding.etSearch.text.clear()
        binding.etSearch.clearFocus()

    }

    //카카오 로컬 검색 API를 활용하여 키워드로 장소를 검색하는 기능 메소드
    private fun searchPlaces(){
        Toast.makeText(this, "$searchQuery", Toast.LENGTH_SHORT).show()

        //레트로핏을 이용한 REST API 작업 수행 - GET 방식
        val retrofit= RetrofitHelper.getRetrofitInstance("https://dapi.kakao.com")
        val retrofitApiService= retrofit.create(RetrofitApiService::class.java)
        val call= retrofitApiService.searchPlace(searchQuery, myLocation?.longitude.toString(), myLocation?.latitude.toString())
        call.enqueue(object : Callback<KakaoSearchPlaceResponse>{
            override fun onResponse(
                call: Call<KakaoSearchPlaceResponse>,
                response: Response<KakaoSearchPlaceResponse>
            ) {
                //응답받은 json을 파싱한 객체 참조하기
                searchPlaceResponse= response.body()

                //먼저 데이터가 온전히 잘 왔는지 파악
                val meta: PlaceMeta? = searchPlaceResponse?.meta
                val documents: List<Place>? = searchPlaceResponse?.documents

                //AlertDialog.Builder(this@MainActivity).setMessage("${meta?.total_count}\n${documents?.get(0).place_name}").create().show()

                //무조건 검색이 완료되면 '리스트' 형태로 먼저 보여주도록
                binding.bnv.selectedItemId= R.id.menu_bnv_list

                //fabRefresh 버튼 원위치
                ObjectAnimator.ofFloat(binding.fabRefresh, "translationY", 0f).start()
                ObjectAnimator.ofFloat(binding.fabRefresh, "rotationX", 0f).start()
            }

            override fun onFailure(call: Call<KakaoSearchPlaceResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "서버 오류가 있습니다.", Toast.LENGTH_SHORT).show()
            }

        })

//        val call= retrofitApiService.searchPlaceToString(searchQuery, myLocation?.longitude.toString(), myLocation?.latitude.toString())
//        call.enqueue(object : Callback<String>{
//            override fun onResponse(call: Call<String>, response: Response<String>) {
//                val s= response.body()
//                AlertDialog.Builder(this@MainActivity).setMessage(s).create().show()
//            }
//
//            override fun onFailure(call: Call<String>, t: Throwable) {
//                Toast.makeText(this@MainActivity, "error: ${t.message}", Toast.LENGTH_SHORT).show()
//            }
//
//        })

    }
}//MainActivity class