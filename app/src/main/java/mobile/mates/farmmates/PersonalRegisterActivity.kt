package mobile.mates.farmmates

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import mobile.mates.farmmates.databinding.ActivityPersonalRegisterBinding
import mobile.mates.farmmates.dialogs.ChooseDialog
import mobile.mates.farmmates.dialogs.LoadingDialog
import mobile.mates.farmmates.models.User
import java.io.File

class PersonalRegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPersonalRegisterBinding

    // Image loading
    private lateinit var cameraUri: Uri

    private val getGalleryContent = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { loadImage(it) }

    private val getCameraContent = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) {
        if (it)
            loadImage(cameraUri)
    }

    // Firebase realtime database
    private lateinit var database: FirebaseDatabase
    private lateinit var db: DatabaseReference
    private val usersSelector = "users/"

    // Firebase cloud storage
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private lateinit var profilePicUri: Uri

    // Auth
    private lateinit var auth: FirebaseAuth

    private lateinit var credentialsBundle: Bundle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonalRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        credentialsBundle = intent.getBundleExtra("info")!!

        // Camera file provider
        val file = File(filesDir, "picFromCamera")
        cameraUri =
            FileProvider.getUriForFile(baseContext, baseContext.packageName + ".fileprovider", file)

        // Firebase db and storage init
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        storageRef = storage.getReference("ProfilePics")

        binding.signUpButton.setOnClickListener { handleRegister() }
        binding.goBackButton.setOnClickListener { goBack() }
        binding.loadProfilePic.setOnClickListener { loadProfilePic() }
    }

    private fun handleRegister() {
        if (!checkValidPersonalInfo())
            return
        binding.signUpButton.isEnabled = false
        binding.signUpButton.isClickable = false
        signUpToFirebase()
    }

    private fun signUpToFirebase() {
        val email = credentialsBundle.getString("email")!!
        val password = credentialsBundle.getString("password")!!

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
            if (it.isSuccessful)
                saveProfilePic()
            else {
                val message = it.exception!!.message
                Toast.makeText(baseContext, message, Toast.LENGTH_LONG).show()
                goBack()
            }
        }
    }

    private fun checkValidPersonalInfo(): Boolean {
        val nameValue = binding.name.text.toString()
        val lastNameValue = binding.lastName.text.toString()
        val phoneNumber = binding.phoneNumber.text.toString()

        var valid = false

        if (nameValue.isEmpty())
            binding.name.error = "Required"
        else if (lastNameValue.isEmpty())
            binding.lastName.error = "Required"
        else if (phoneNumber.isEmpty())
            binding.phoneNumber.error = "Required"
        else
            valid = true

        return valid
    }

    private fun saveProfilePic() {
        if (this::profilePicUri.isInitialized) {
            // Save profile pic to cloud storage
            if (this::profilePicUri.isInitialized) {
                val profilePicRef = storageRef.child(auth.currentUser!!.uid)
                profilePicRef.putFile(profilePicUri)
                    .addOnSuccessListener {
                        it.metadata!!.reference!!.downloadUrl.addOnSuccessListener { url ->
                            savePersonalInfo(url.toString())
                        }
                    }
                    .addOnFailureListener {
                        Log.e("Storage", "Error saving profile pic")
                        savePersonalInfo("")
                    }
                    .addOnProgressListener {
                        LoadingDialog().show(
                            supportFragmentManager,
                            "LOADING_DIALOG"
                        )
                    }
            }
        } else
            savePersonalInfo("")
    }

    private fun savePersonalInfo(profilePicUrl: String) {
        // Save user info to realtime database
        if (auth.currentUser != null) {

            val user = User(
                binding.name.text.toString(),
                binding.lastName.text.toString(),
                binding.phoneNumber.text.toString(),
                auth.currentUser!!.uid,
                profilePicUrl,
                0.0,
                0.0
            )
            db = database.getReference(usersSelector + auth.currentUser!!.uid)
            db.setValue(user)
            goToMainPage()
        }

    }

    private fun goToMainPage() {
        startActivity(Intent(baseContext, MainActivity::class.java))
    }

    private fun goBack() {
        startActivity(Intent(baseContext, RegisterActivity::class.java))
    }

    private fun loadFromGallery() {
        getGalleryContent.launch("image/*")
    }

    private fun loadFromCamera() {
        getCameraContent.launch(cameraUri)
    }

    private fun loadProfilePic() {
        ChooseDialog(
            ::loadFromGallery,
            ::loadFromCamera
        ).show(supportFragmentManager, "SOURCE_SELECTOR")
    }

    private fun loadImage(uri: Uri?) {
        if (uri == null)
            Toast.makeText(baseContext, "Please, select a photo", Toast.LENGTH_SHORT).show()
        else {
            val imageStream = contentResolver.openInputStream(uri)
            profilePicUri = uri
            binding.loadProfilePic.setImageBitmap(BitmapFactory.decodeStream(imageStream))
        }
    }
}