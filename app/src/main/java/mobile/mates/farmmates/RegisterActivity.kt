package mobile.mates.farmmates

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import mobile.mates.farmmates.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.goLogin.setOnClickListener { goLoginPage() }

        // TODO : Send page information to handle register
        binding.goNextButton.setOnClickListener { goNextRegisterPage() }
    }

    private fun goLoginPage() {
        startActivity(Intent(baseContext, LoginActivity::class.java))
    }

    private fun goNextRegisterPage() {
        startActivity(Intent(baseContext, PersonalRegisterActivity::class.java))
    }
}