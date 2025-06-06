package com.example.studin.activities
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.studin.R
import com.example.studin.classes.Company
import com.example.studin.databinding.ActivityCompanyHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

class CompanyHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompanyHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var companyReference: DatabaseReference

    private val TAG = "CompanyHomeActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompanyHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val companyUid = auth.currentUser?.uid

        if(companyUid != null){
            loadUserProfileImage()
        }

        binding.textViewLogout.setOnClickListener {
            logoutUser()
        }
        binding.buttonLogout.setOnClickListener {
            val intent = Intent(this, CompanyOffersActivity::class.java)
            startActivity(intent)
        }

        binding.chat.setOnClickListener {
            val intent = Intent(this, MainChatsActivity::class.java)
            startActivity(intent)
        }
        binding.companyProfile.setOnClickListener {
            val intent = Intent(this, CompanyProfileActivity::class.java)
            startActivity(intent)
        }

    }

    private fun logoutUser() {
        auth.signOut()
        Toast.makeText(this, "Has cerrado sesión.", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun loadUserProfileImage() {
        companyReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val company = snapshot.getValue(Company::class.java)
                if (company != null && !company.profileImageUrl.isNullOrEmpty()) {
                    Log.d(TAG, "URL de imagen de perfil obtenida: ${company.profileImageUrl}")
                    Glide.with(this@CompanyHomeActivity)
                        .load(company.profileImageUrl)
                        .placeholder(R.drawable.icono_empresa)
                        .error(R.drawable.icono_empresa)
                        .circleCrop()
                        .into(binding.companyProfile)
                } else {
                    Log.w(TAG, "No se encontró URL de imagen de perfil o está vacía.")
                    binding.companyProfile.setImageResource(R.drawable.icono_persona)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Error al cargar datos del usuario para imagen: ", error.toException())
                binding.companyProfile.setImageResource(R.drawable.ic_profile_person)
            }
        })
    }
}