package mobile.mates.farmmates

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import mobile.mates.farmmates.crypt.BiometricPromptUtils
import mobile.mates.farmmates.crypt.CIPHERTEXT_WRAPPER
import mobile.mates.farmmates.crypt.CryptographyManager
import mobile.mates.farmmates.crypt.SHARED_PREFS_FILENAME
import mobile.mates.farmmates.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    // Firebase connection
    private lateinit var auth: FirebaseAuth

    // Biometrics
    private var token: String? = null
    private var awaitForBio: Boolean = false
    private lateinit var biometricPrompt: BiometricPrompt
    private var cryptographyManager = CryptographyManager()
    private val ciphertextWrapper
        get() = cryptographyManager.getCiphertextWrapperFromSharedPrefs(
            applicationContext,
            SHARED_PREFS_FILENAME,
            Context.MODE_PRIVATE,
            CIPHERTEXT_WRAPPER
        )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        // Biometrics setup
        val canAuthenticate = BiometricManager.from(applicationContext).canAuthenticate()
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            binding.fingerprintLogin.visibility = View.VISIBLE
            binding.fingerprintLogin.setOnClickListener {
                if (ciphertextWrapper != null) {
                    showBiometricPromptForDecryption()
                } else {
                    enableBiometricLogin()
                }
            }
        } else {
            binding.fingerprintLogin.visibility = View.INVISIBLE
        }

        binding.loginButton.setOnClickListener {
            handleLogin(
                binding.emailAddress.text.toString(), binding.password.text.toString()
            )
        }
        binding.goSignUp.setOnClickListener { goToSignUp() }
    }

    private fun enableBiometricLogin() {
        awaitForBio = true
        handleLogin(binding.emailAddress.text.toString(), binding.password.text.toString())
    }

//    override fun onStart() {
//        super.onStart()
//        updateUI(auth.currentUser)
//    }

    override fun onResume() {
        super.onResume()
        getToken()
        if (ciphertextWrapper != null)
            if (token == null)
                showBiometricPromptForDecryption()
            else
            // User is already signed in
                Log.i("Auth", "signed in")
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
                    if (awaitForBio) {
                        getToken()
                        showBiometricPromptForEnabling()
                    } else
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

    private fun getToken() {
        auth.currentUser?.getIdToken(true)?.addOnSuccessListener {
            token = it.toString()
        }
        Log.i("Cipher", "Token from server: $token")
    }

    private fun showBiometricPromptForEnabling() {
        val canAuthenticate = BiometricManager.from(applicationContext).canAuthenticate()
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            val secretKeyName = "biometric_sample_encryption_key"
            cryptographyManager = CryptographyManager()
            val cipher = cryptographyManager.getInitializedCipherForEncryption(secretKeyName)
            val biometricPrompt =
                BiometricPromptUtils.createBiometricPrompt(this, ::encryptAndStoreServerToken)
            val promptInfo = BiometricPromptUtils.createPromptInfo(this)
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun showBiometricPromptForDecryption() {
        ciphertextWrapper?.let { textWrapper ->
            val secretKeyName = "biometric_sample_encryption_key"
            val cipher = cryptographyManager.getInitializedCipherForDecryption(
                secretKeyName, textWrapper.initializationVector
            )
            biometricPrompt =
                BiometricPromptUtils.createBiometricPrompt(
                    this,
                    ::decryptServerTokenFromStorage
                )
            val promptInfo = BiometricPromptUtils.createPromptInfo(this)
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun decryptServerTokenFromStorage(authResult: BiometricPrompt.AuthenticationResult) {
        ciphertextWrapper?.let { textWrapper ->
            authResult.cryptoObject?.cipher?.let {
                val plaintext =
                    cryptographyManager.decryptData(textWrapper.ciphertext, it)
                token = plaintext
                // Now that you have the token, you can query server for everything else
                // the only reason we call this fakeToken is because we didn't really get it from
                // the server. In your case, you will have gotten it from the server the first time
                // and therefore, it's a real token.
                updateUI(auth.currentUser)
            }
        }
    }


    private fun encryptAndStoreServerToken(authResult: BiometricPrompt.AuthenticationResult) {
        authResult.cryptoObject?.cipher?.apply {
            Log.d("Cipher", "The token from server is $token")
            val encryptedServerTokenWrapper =
                token?.let { cryptographyManager.encryptData(it, this) }
            if (encryptedServerTokenWrapper != null) {
                cryptographyManager.persistCiphertextWrapperToSharedPrefs(
                    encryptedServerTokenWrapper,
                    applicationContext,
                    SHARED_PREFS_FILENAME,
                    Context.MODE_PRIVATE,
                    CIPHERTEXT_WRAPPER
                )
            }
            updateUI(auth.currentUser)
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