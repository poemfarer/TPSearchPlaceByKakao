package com.farer.tpsearchplacebykakao.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.farer.tpsearchplacebykakao.R
import com.farer.tpsearchplacebykakao.databinding.ActivitySignupBinding
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SignupActivity : AppCompatActivity() {

    private val binding by lazy { ActivitySignupBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //툴바의 업버튼 클릭시 돌아가기
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnSignup.setOnClickListener { clickSignup() }
    }

    private fun clickSignup() {
        //Firebase Firestore DB에 사용자 정보 저장

        var email= binding.inputLayoutEmail.editText!!.text.toString()
        var password= binding.inputLayoutPassword.editText!!.text.toString()
        var passwordConfirm= binding.inputLayoutPasswordConfirm.editText!!.text.toString()

        //유효성 검사 - 패스워드와 패스워드 확인 일치 검사
        if(password != passwordConfirm){
            AlertDialog.Builder(this).setMessage("패스워드가 일치하지 않습니다.\n다시 입력해주세요.").create().show()
            binding.inputLayoutPasswordConfirm.editText!!.selectAll()
            return
        }

        //Firebase Firestore DB에 저장하기 - 프로젝트 연동부터

        //"emailUsers" Collection(테이블명) 참조객체 소환
        val userRef: CollectionReference= Firebase.firestore.collection("emailUsers")

        //중복된 이메일은 저장하면 안되므로
        userRef.whereEqualTo("email", email).get().addOnSuccessListener {
            //혹시 같은 email 값을 가진 documet가 여러개일 수 있으므로
            if (it.documents.size>0){ //개수가 0ㅐ개 이상이면 같은 email이 있다는 것
                AlertDialog.Builder(this).setMessage("중복된 이메일이 있습니다. 다시 확인하여 입력해주시기 바랍니다.").create().show()
                binding.inputLayoutEmail.editText!!.requestFocus()
                binding.inputLayoutPassword.editText!!.selectAll()

            }else{ //중복된 이메일이 없다면 저장
                //저장할 값(이메일, 비밀번호)을 MutableMap으로 묶어주기
                val user: MutableMap<String, String> = mutableMapOf()
                user["email"] = email
                user["password"] = password

                userRef.document().set(user).addOnSuccessListener {
                    AlertDialog.Builder(this)
                        .setMessage("축하합니다.\n회원가입이 완료되었습니다.")
                        .setPositiveButton("확인", { p0, p1 ->
                            finish()
                        })
                        .create().show()
                }

            }
        }

    }
}