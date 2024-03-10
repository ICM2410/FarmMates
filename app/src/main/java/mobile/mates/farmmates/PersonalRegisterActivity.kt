package mobile.mates.farmmates

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import mobile.mates.farmmates.databinding.ActivityPersonalRegisterBinding

class PersonalRegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPersonalRegisterBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonalRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO : Receive previous page information

        binding.signUpButton.setOnClickListener { handleRegister() }
        binding.goBackButton.setOnClickListener { goBack() }
    }

    private fun handleRegister() {
        // TODO : Handle register
        startActivity(Intent(baseContext, LoginActivity::class.java))
    }

    private fun goBack() {
        startActivity(Intent(baseContext, RegisterActivity::class.java))
    }
}