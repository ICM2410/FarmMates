package mobile.mates.farmmates

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import mobile.mates.farmmates.databinding.ActivityReportBinding
import mobile.mates.farmmates.dialogs.ChooseDialog
import mobile.mates.farmmates.models.Report
import java.io.File
import java.net.URL
import java.util.UUID

class ReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var reportImageUri: Uri

    private val imageRefSelector = "images/"
    private val reportsRefSelector = "reports/"

    private lateinit var cameraUri: Uri

    private val getGalleryContent = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { updateImageView(it) }

    private val getCameraContent = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) {
        if (it)
            updateImageView(cameraUri)
    }

    private lateinit var report: Report

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        database = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        val file = File(filesDir, "picFromCamera")
        cameraUri =
            FileProvider.getUriForFile(baseContext, baseContext.packageName + ".fileprovider", file)

        binding.reportImage.setOnClickListener { loadImage() }
        binding.sendReport.setOnClickListener { createReport() }
    }

    private fun loadImage() {
        ChooseDialog(
            ::loadFromGallery,
            ::loadFromCamera
        ).show(supportFragmentManager, "SOURCE_SELECTOR")
    }

    private fun loadFromCamera() {
        getCameraContent.launch(cameraUri)
    }

    private fun loadFromGallery() {
        getGalleryContent.launch("image/*")
    }

    private fun updateImageView(uri: Uri?) {
        if (uri == null)
            Toast.makeText(baseContext, "Please, select a photo", Toast.LENGTH_SHORT).show()
        else {
            val imageStream = contentResolver.openInputStream(uri)
            reportImageUri = uri
            binding.reportImage.setImageBitmap(BitmapFactory.decodeStream(imageStream))
        }
    }

    private fun createReport() {
        if (!validateFields())
            return

        report = Report()
        report.description = binding.descriptionInput.text.toString()
        report.priority = binding.triageSelect.selectedItem.toString()
        report.createdBy = getAuthId()
        saveImage()
    }

    private fun validateFields(): Boolean {
        return true
    }

    private fun getAuthId(): String {
        return if (auth.currentUser != null)
            auth.currentUser!!.uid
        else
            ""
    }

    private fun saveImage() {
        if (this::reportImageUri.isInitialized) {
            val newImageRef = storage.getReference(imageRefSelector + UUID.randomUUID().toString())
            newImageRef.putFile(reportImageUri)
                .addOnSuccessListener {
                    it.metadata!!.reference!!.downloadUrl.addOnSuccessListener { url ->
                        report.imageUrl = URL(url.toString())
                        saveReport()
                    }
                }
                .addOnFailureListener {
                    Log.e("Storage", "Error saving report's image")
                }
        }
    }

    private fun saveReport() {
        database.collection(reportsRefSelector)
            .add(report)
            .addOnSuccessListener {
                Toast.makeText(baseContext, "Se ha guardado el reporte", Toast.LENGTH_LONG).show()
                goToMap()
            }
            .addOnFailureListener {
                Toast.makeText(
                    baseContext,
                    "Se ha producido un error al guardar el reporte. Intente de nuevo",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun goToMap() {
        startActivity(Intent(baseContext, Map::class.java))
    }
}