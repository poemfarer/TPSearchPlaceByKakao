package com.farer.tpsearchplacebykakao.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.farer.tpsearchplacebykakao.G
import com.farer.tpsearchplacebykakao.R
import com.farer.tpsearchplacebykakao.data.UserAccout
import com.farer.tpsearchplacebykakao.databinding.ActivityEmailLoginBinding
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class EmailLoginActivity : AppCompatActivity() {

    private val binding by lazy { ActivityEmailLoginBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnSignin.setOnClickListener { clickSignIn() }
    }

    private fun clickSignIn() {

        val email= binding.inputLayoutEmail.editText!!.text.toString()
        val password= binding.inputLayoutPassword.editText!!.text.toString()

        //Firebase Firestore DB에서 이메일 로그인 확인
        val userRef: CollectionReference = Firebase.firestore.collection("emailUsers")
        userRef
            .whereEqualTo("email", email)
            .whereEqualTo("password", password)
            .get().addOnSuccessListener {
                if (it.documents.size>0){ //[이메일, 비밀번호] 검색결과가 1개 이상이므로 찾았음을 의미하며, 로그인 성공

                    //다른 화면에서도 회원정보를 사용할 수 있으므로 전역변수처럼 G클래스에 저장
                    val id:String = it.documents[0].id //랜덤하게 만들어진 document 명을 id로 활용!!
                    G.userAccont= UserAccout(id, email)

                    //로그인에 성공했으니 Main 화면으로 이동
                    val intent= Intent(this, MainActivity::class.java)

                    //기존 작업의 모든 액티비티를 제거하고 새로운 task 시작
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)

                }else{
                    //이메일과 비밀번호에 해당하는 document가 없음, 로그인 실패
                    AlertDialog.Builder(this).setMessage("이메일과 비밀번호를 다시 확인해주세요").create().show()
                    binding.inputLayoutEmail.editText!!.requestFocus()
                    binding.inputLayoutEmail.editText!!.selectAll()
                }
            }

    }
}