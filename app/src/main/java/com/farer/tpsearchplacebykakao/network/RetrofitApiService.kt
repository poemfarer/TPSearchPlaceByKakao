package com.farer.tpsearchplacebykakao.network

import com.farer.tpsearchplacebykakao.data.KakaoSearchPlaceResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Query

interface RetrofitApiService {

    //카카오 로컬 검색 API 요청해주는 코드 만들어줘. 우선 응답 type: String
    @Headers("Authorization: KakaoAK 037db85e5a5d8f616669e566a2403a71")
    @GET("/v2/local/search/keyword.json")
    fun searchPlaceToString(@Query("query") query:String, @Query("x") longitude:String, @Query("y") latitude:String) : Call<String>


    //카카오 로컬 검색 API 요청해주는 코드 만들어줘. 우선 응답 type: KakaoSearchPlaceResponse
    @Headers("Authorization: KakaoAK 037db85e5a5d8f616669e566a2403a71")
    @GET("/v2/local/search/keyword.json?sort=distance")
    fun searchPlace(@Query("query") query:String, @Query("x") longitude:String, @Query("y") latitude:String) : Call<KakaoSearchPlaceResponse>


    //네아로 회원정보 프로필 API 요청
    @GET("/v1/nid/me")
    fun getNidUserInfo(@Header("Authorization") authorization: String) : Call<String>
}