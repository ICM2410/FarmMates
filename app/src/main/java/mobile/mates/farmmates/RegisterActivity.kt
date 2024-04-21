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
        binding.goNextButton.setOnClickListener {
            handleSignUp(
                binding.emailAddress.text.toString(),
                binding.password.text.toString(),
                binding.confirmPassword.text.toString()
            )
        }
    }

    private fun goLoginPage() {
        startActivity(Intent(baseContext, LoginActivity::class.java))
    }

    private fun handleSignUp(email: String, password: String, confirmPassword: String) {
        if (!validForm(email, password, confirmPassword)) return

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
            if (it.isSuccessful) {
                goNextRegisterPage()
            } else
                Toast.makeText(baseContext, "Sign up failed", Toast.LENGTH_SHORT).show()
        }
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
        Toast.makeText(baseContext, "Welcome to FarmMates", Toast.LENGTH_SHORT).show()

        // TODO : Handle user personal info
        // startActivity(Intent(baseContext, PersonalRegisterActivity::class.java))

        startActivity(Intent(baseContext, MainActivity::class.java))
    }
}