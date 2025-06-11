package com.example.studin.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.example.studin.R
import com.example.studin.classes.News
import com.example.studin.databinding.ActivityCreateEditNewsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import java.util.UUID

class CreateEditNewsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateEditNewsBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var newsRootRef: DatabaseReference
    private lateinit var storage: com.google.firebase.storage.FirebaseStorage

    private var currentNewsId: String? = null
    private var currentCompanyId: String? = null
    private var currentCompanyName: String? = null

    private var selectedImageUri: Uri? = null
    private var imageUrlFromDatabase: String? = null

    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    private val TAG = "CreateEditNewsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateEditNewsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        newsRootRef = database.getReference("news")
        storage = Firebase.storage

        currentCompanyId = FirebaseAuth.getInstance().currentUser?.uid
        currentCompanyName =
            FirebaseAuth.getInstance().currentUser?.displayName ?: "Empresa Anónima"

        setupImagePicker()

        if (intent.hasExtra("NEWS_ID")) {
            currentNewsId = intent.getStringExtra("NEWS_ID")
            if (currentNewsId != null) {
                Log.d(TAG, "Modo Edición para NEWS_ID: $currentNewsId")
                supportActionBar?.title = getString(R.string.title_edit_news)
                loadNewsData(currentNewsId!!)
            } else {
                Log.w(TAG, "NEWS_ID era esperado pero es nulo en modo edición.")
                supportActionBar?.title = getString(R.string.title_create_news)
            }
        } else {
            Log.d(TAG, "Modo Creación de Noticia")
            supportActionBar?.title = getString(R.string.title_create_news)
        }

        if (currentCompanyId == null) {
            Toast.makeText(this, "Error: Usuario no identificado.", Toast.LENGTH_LONG).show()
            binding.buttonSaveNews.isEnabled = false
        }

        binding.buttonSelectNewsImage.setOnClickListener {
            openGalleryForImage()
        }

        binding.buttonSaveNews.setOnClickListener {
            validateAndSaveNews()
        }
    }

    private fun setupImagePicker() {
        imagePickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    data?.data?.let { uri ->
                        selectedImageUri = uri
                        binding.imageViewNewsPreview.setImageURI(uri)
                        binding.imageViewNewsPreview.isVisible = true
                        Log.d(TAG, "Imagen seleccionada: $selectedImageUri")
                    }
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.error_selecting_image),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun loadNewsData(newsId: String) {
        newsRootRef.child(newsId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val newsItem = snapshot.getValue(News::class.java)
                newsItem?.let {
                    binding.editTextNewsTitle.setText(it.title)
                    binding.editTextNewsContent.setText(it.content)
                    // Guardar la URL existente y cargarla en la preview si existe
                    imageUrlFromDatabase = it.imageUrl
                    if (!it.imageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(it.imageUrl)
                            .placeholder(R.drawable.default_header_placeholder)
                            .error(R.drawable.default_header_placeholder)
                            .into(binding.imageViewNewsPreview)
                        binding.imageViewNewsPreview.isVisible = true
                    } else {
                        binding.imageViewNewsPreview.isVisible = false
                    }
                }
            } else {
                Toast.makeText(this, "Error: No se encontró la noticia.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al cargar noticia: ${it.message}", Toast.LENGTH_SHORT)
                .show()
            finish()
        }
    }

    private fun validateAndSaveNews() {
        val title = binding.editTextNewsTitle.text.toString().trim()
        val content = binding.editTextNewsContent.text.toString().trim()

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Título y contenido son obligatorios.", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentCompanyId == null) {
            Toast.makeText(this, "Error crítico: ID de empresa no disponible.", Toast.LENGTH_LONG)
                .show()
            return
        }

        // Deshabilitar botón de guardar para evitar múltiples clicks
        binding.buttonSaveNews.isEnabled = false
        binding.progressBarNewsUpload.isVisible = true // Mostrar ProgressBar general

        if (selectedImageUri != null) {
            //Si hay una nueva imagen seleccionada para subir
            uploadImageAndSaveNews(title, content)
        } else {
            proceedToSaveNewsData(title, content, imageUrlFromDatabase)
        }
    }

    private fun getFileExtension(uri: Uri): String? {
        val cr = contentResolver
        val mime = MimeTypeMap.getSingleton()
        return mime.getExtensionFromMimeType(cr.getType(uri))
    }

    private fun uploadImageAndSaveNews(title: String, content: String) {
        val newsIdForStorage = currentNewsId ?: newsRootRef.push().key ?: UUID.randomUUID()
            .toString()
        val fileExtension = selectedImageUri?.let { getFileExtension(it) } ?: "jpg"
        val imageFileName = "${UUID.randomUUID()}.$fileExtension"
        val imagePath = "news_images/$newsIdForStorage/$imageFileName"
        val imageRef: StorageReference = storage.reference.child(imagePath)

        Log.d(TAG, "Subiendo imagen a Firebase Storage en ruta: $imagePath")
        binding.progressBarNewsUpload.isIndeterminate = false

        selectedImageUri?.let { uri ->
            imageRef.putFile(uri)
                .addOnSuccessListener { taskSnapshot ->
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        val uploadedImageUrl = downloadUri.toString()
                        Log.d(TAG, "Imagen subida exitosamente. URL: $uploadedImageUrl")
                        Toast.makeText(
                            this,
                            getString(R.string.image_uploaded_successfully),
                            Toast.LENGTH_SHORT
                        ).show()
                        proceedToSaveNewsData(title, content, uploadedImageUrl)
                    }.addOnFailureListener { exception ->
                        Log.e(TAG, "Error al obtener URL de descarga de imagen", exception)
                        Toast.makeText(
                            this,
                            getString(R.string.error_getting_download_url, exception.message),
                            Toast.LENGTH_LONG
                        ).show()
                         proceedToSaveNewsData(
                            title,
                            content,
                            if (currentNewsId != null) imageUrlFromDatabase else null
                        )
                        binding.progressBarNewsUpload.isVisible = false
                        binding.buttonSaveNews.isEnabled = true
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error al subir imagen a Firebase Storage", exception)
                    Toast.makeText(
                        this,
                        getString(R.string.error_uploading_image, exception.message),
                        Toast.LENGTH_LONG
                    ).show()
                    binding.progressBarNewsUpload.isVisible = false
                    binding.buttonSaveNews.isEnabled =
                        true
                }
                .addOnProgressListener { taskSnapshot ->
                    val progress =
                        (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                    binding.progressBarNewsUpload.progress = progress.toInt()
                    Log.d(TAG, "Progreso de subida de imagen: ${progress.toInt()}%")
                }
        } ?: run {
            Log.w(
                TAG,
                "uploadImageAndSaveNews fue llamado pero selectedImageUri es nulo. Guardando con URL existente o sin imagen."
            )
            proceedToSaveNewsData(title, content, imageUrlFromDatabase)
        }
    }

    private fun proceedToSaveNewsData(title: String, content: String, finalImageUrl: String?) {
        val newsIdToUse = currentNewsId ?: newsRootRef.push().key ?: run {
            Log.e(TAG, "Fallo al obtener push key, generando UUID para la noticia.")
            UUID.randomUUID().toString()
        }

        if (currentCompanyId == null) {
            Toast.makeText(
                this,
                "Error crítico: ID de empresa es nulo antes de guardar en DB.",
                Toast.LENGTH_LONG
            ).show()
            binding.progressBarNewsUpload.isVisible = false
            binding.buttonSaveNews.isEnabled = true
            return
        }

        val newsItem = News(
            uid = newsIdToUse,
            title = title,
            content = content,
            imageUrl = finalImageUrl,
            timestamp = System.currentTimeMillis(),
            authorId = currentCompanyId!!,
            authorName = currentCompanyName
        )

        newsRootRef.child(newsIdToUse).setValue(newsItem)
            .addOnSuccessListener {
                Log.d(TAG, "Noticia guardada/actualizada en DB con ID: $newsIdToUse")
                Toast.makeText(this, "Noticia guardada exitosamente", Toast.LENGTH_SHORT).show()
                binding.progressBarNewsUpload.isVisible = false
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar noticia en Realtime Database para ID: $newsIdToUse", e)
                Toast.makeText(
                    this,
                    "Error al guardar la noticia: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                binding.progressBarNewsUpload.isVisible = false
                binding.buttonSaveNews.isEnabled =
                    true // Reactivar botón en caso de fallo para reintentar
            }
    }
}