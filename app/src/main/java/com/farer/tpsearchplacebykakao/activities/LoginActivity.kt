package com.farer.tpsearchplacebykakao.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.farer.tpsearchplacebykakao.G
import com.farer.tpsearchplacebykakao.R
import com.farer.tpsearchplacebykakao.data.UserAccout
import com.farer.tpsearchplacebykakao.databinding.ActivityLoginBinding
import com.farer.tpsearchplacebykakao.network.RetrofitApiService
import com.farer.tpsearchplacebykakao.network.RetrofitHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.OAuthLoginCallback
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private val binding by lazy { ActivityLoginBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //둘러보기를 클릭하면 로그인 없이 Main 화면으로 이동
        binding.tvGo.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        //회원가입 버튼 클릭
        binding.tvSignup.setOnClickListener { startActivity(Intent(this, SignupActivity::class.java)) }


        //이메일 로그인 버튼 클릭
        binding.layoutEmailLogin.setOnClickListener { startActivity(Intent(this, EmailLoginActivity::class.java)) }


        //간편로그인 버튼 클릭
        binding.btnLoginKakao.setOnClickListener { clickKakao() }
        binding.btnLoginGoogle.setOnClickListener { clickGoogle() }
        binding.btnLoginNaver.setOnClickListener { clickNaver() }
    }

    private fun clickKakao(){
        //Toast.makeText(this, "카카오 로그인", Toast.LENGTH_SHORT).show()

        //로그인 요청 콜백 함수
        val callback:(OAuthToken?, Throwable?)->Unit = { token, error ->
            if (error != null){
                Toast.makeText(this, "카카오 로그인 실패", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this, "카카오 로그인 성공", Toast.LENGTH_SHORT).show()

                //사용자 정보 요청
                UserApiClient.instance.me { user, error ->
                    if (user!=null){
                        val id:String= user.id.toString()
                        val nickname:String = user.kakaoAccount?.profile?.nickname ?: ""

                        Toast.makeText(this, "$id\n$nickname", Toast.LENGTH_SHORT).show()
                        G.userAccont= UserAccout(id, nickname)

                        //로그인되었다면
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                }

            }

        }

        //카카오톡이 사용 가능하면 이를 이용하여 로그인, 없다면 카카오계정으로 로그인
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)){
            UserApiClient.instance.loginWithKakaoTalk(this, callback = callback)
        }else{
            UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
        }

    }

    private fun clickGoogle(){
        //Toast.makeText(this, "구글 로그인", Toast.LENGTH_SHORT).show()

        //로그인 옵션객체 생성 - Builder - 이메일 요청
        val signInOptions: GoogleSignInOptions= GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build()

        //구글 로그인 화면 Activity를 실행하는 Intent 객체로 로그인
        val intent:Intent= GoogleSignIn.getClient(this, signInOptions).signInIntent
        resultLauncher.launch(intent)
    }

    //구글 로그인 화면 결과를 받아주는 대행사 등록 - startActivity로는 결과를 받아올 수 없기 때문
    val resultLauncher= registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        //로그인 결과를 가져온 인텐트 소환
        val intent:Intent?= it.data
        //인텐트로부터 구글 계정 정보를 가져오는 작업자 객체를 소환
        val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(intent)

        //작업자로부터 계정객체 받기
        val account: GoogleSignInAccount= task.result
        val id:String= account.id.toString()
        val email:String= account.email ?: ""

        Toast.makeText(this, "$id\n$email", Toast.LENGTH_SHORT).show()
        G.userAccont= UserAccout(id, email)

        //main 화면으로 이동
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun clickNaver(){
        //Toast.makeText(this, "네이버 로그인", Toast.LENGTH_SHORT).show()

        //네아로 SDK 초기화
        NaverIdLoginSDK.initialize(this, "cHxRRyGAggYed3gQm2bG", "RsyfmTtsI9", "서치워크")

        //로그인 요청
        NaverIdLoginSDK.authenticate(this, object : OAuthLoginCallback{
            override fun onError(errorCode: Int, message: String) {
                Toast.makeText(this@LoginActivity, "$message", Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(httpStatus: Int, message: String) {
                Toast.makeText(this@LoginActivity, "$message", Toast.LENGTH_SHORT).show()
            }

            override fun onSuccess() {
                Toast.makeText(this@LoginActivity, "로그인 성공", Toast.LENGTH_SHORT).show()

                //사용자 정보를 받아오기 -- REST API로 받아야 함
                //로그인에 성공하면 REST API로 요청할 수 있는 토큰(token)을 발급 받음
                val accessToken:String? = NaverIdLoginSDK.getAccessToken()

                //Retrofit 작업을 통해 사용자 정보 가져오기
                val retrofit= RetrofitHelper.getRetrofitInstance("https://openapi.naver.com")
                val retrofitApiService= retrofit.create(RetrofitApiService::class.java)
                val call= retrofitApiService.getNidUserInfo("Bearer $accessToken")
                call.enqueue(object : Callback<String>{
                    override fun onResponse(call: Call<String>, response: Response<String>) {
                        val s= response.body()
                        AlertDialog.Builder(this@LoginActivity).setMessage(s).create().show()

                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }

                    override fun onFailure(call: Call<String>, t: Throwable) {
                        Toast.makeText(this@LoginActivity, "${t.message}", Toast.LENGTH_SHORT).show()
                    }

                })
            }

        })

    }
}