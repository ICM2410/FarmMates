package mobile.mates.farmmates

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import mobile.mates.farmmates.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener { goToMainPage() }
    }

    private fun goToMainPage() {
        startActivity(Intent(baseContext, MainActivity::class.java))
    }
}