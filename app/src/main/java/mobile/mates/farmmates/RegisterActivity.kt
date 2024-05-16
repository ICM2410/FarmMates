package mobile.mates.farmmates

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import mobile.mates.farmmates.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    // Firebase auth
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        binding.goLogin.setOnClickListener { goLoginPage() }
        binding.goNextButton.setOnClickListener { goNextRegisterPage() }
    }

    private fun goLoginPage() {
        startActivity(Intent(baseContext, LoginActivity::class.java))
    }

    private fun validForm(email: String, password: String, confirmPassword: String): Boolean {
        var valid = false

        if (email.isEmpty())
            binding.emailAddress.error = "Required"
        else if (!validEmailAddress(email))
            binding.emailAddress.error = "Invalid email address"
        else if (password.isEmpty())
            binding.password.error = "Required"
        else if (password.length < 6)
            binding.password.error = "Password should be at least 6 characters long"
        else if (password != confirmPassword)
            binding.confirmPassword.error = "Passwords do not match"
        else
            valid = true

        return valid
    }

    private fun validEmailAddress(email: String): Boolean {
        val regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$"
        return email.matches(regex.toRegex())
    }

    private fun goNextRegisterPage() {
        val intent = Intent(baseContext, PersonalRegisterActivity::class.java)
        val bundle = Bundle()
        bundle.putString("email", binding.emailAddress.text.toString())
        bundle.putString("password", binding.confirmPassword.text.toString())
        intent.putExtra("info", bundle)
        startActivity(intent)
    }
}