package mobile.mates.farmmates

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import mobile.mates.farmmates.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    // Firebase connection
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        binding.loginButton.setOnClickListener {
            handleLogin(
                binding.emailAddress.text.toString(), binding.password.text.toString()
            )
        }
        binding.goSignUp.setOnClickListener { goToSignUp() }
    }

    override fun onStart() {
        super.onStart()
        updateUI(auth.currentUser)
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            Toast.makeText(baseContext, "Welcome", Toast.LENGTH_LONG).show()
            goToMainPage()
        }
    }

    private fun goToMainPage() {
        startActivity(Intent(baseContext, MainActivity::class.java))
    }

    private fun goToSignUp() {
        startActivity(Intent(baseContext, RegisterActivity::class.java))
    }

    private fun handleLogin(email: String, password: String) {
        if (validForm(email, password)) {
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                if (it.isSuccessful) {
                    updateUI(auth.currentUser)
                } else {
                    val message = it.exception!!.message
                    Toast.makeText(baseContext, message, Toast.LENGTH_LONG).show()
                    binding.emailAddress.text.clear()
                    binding.password.text.clear()
                }
            }
        }
    }

    private fun validForm(email: String, password: String): Boolean {
        var valid = false
        if (email.isEmpty())
            binding.emailAddress.error = "Required"
        else if (!validEmailAddress(email))
            binding.emailAddress.error = "Invalid email address"
        else if (password.isEmpty())
            binding.password.error = "Required"
        else if (password.length < 6)
            binding.password.error = "Password should be at least 6 characters long"
        else
            valid = true

        return valid
    }

    private fun validEmailAddress(email: String): Boolean {
        val regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$"
        return email.matches(regex.toRegex())
    }
}