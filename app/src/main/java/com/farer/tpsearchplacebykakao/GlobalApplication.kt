package com.farer.tpsearchplacebykakao

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        //카카오 SDK 초기화 작업
        KakaoSdk.init(this, "6e68c207200bd83c4b974c0ac9abe88f")
    }
}