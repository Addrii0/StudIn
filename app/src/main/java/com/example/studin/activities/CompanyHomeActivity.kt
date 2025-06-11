package com.example.studin.activities
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CompanyHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompanyHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var companyReference: DatabaseReference
    private lateinit var database: FirebaseDatabase
    private val TAG = "CompanyHomeActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompanyHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        val companyUid = auth.currentUser?.uid

        if(companyUid != null){
            companyReference = database.getReference("companies").child(companyUid)
            loadUserProfileImage()
        }

        binding.layoutButtonLogout.setOnClickListener {
            logoutUser()
        }
        binding.buttonCreateNewOffer.setOnClickListener {
            val intent = Intent(this, CompanyOffersActivity::class.java)
            startActivity(intent)
        }

        binding.layoutButtonChat.setOnClickListener {
            val intent = Intent(this, MainChatsActivity::class.java)
            startActivity(intent)
        }
        binding.companyProfileIcon.setOnClickListener {
            val intent = Intent(this, CompanyProfileActivity::class.java)
            startActivity(intent)
        }
    loadCompanyStats()
    setupQuickTip()
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
                        .into(binding.companyProfileIcon)
                } else {
                    Log.w(TAG, "No se encontró URL de imagen de perfil o está vacía.")
                    binding.companyProfileIcon.setImageResource(R.drawable.icono_persona)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Error al cargar datos del usuario para imagen: ", error.toException())
                binding.companyProfileIcon.setImageResource(R.drawable.ic_profile_person)
            }
        })
    }
    private fun loadCompanyStats() {
        val companyId = auth.currentUser?.uid
        if (companyId == null) {
            binding.textViewActiveOffersCount.text = "N/A"
            binding.textViewNewApplicantsCount.text = "N/A"
            return
        }

        val offersRef = database.getReference("offers")
        offersRef.orderByChild("companyId").equalTo(companyId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                         var activeOfferCount = 0
                         for (offerSnapshot in snapshot.children) {
                             val status = offerSnapshot.child("active").getValue(Boolean::class.java)
                             if (status == true) {
                                 activeOfferCount++
                             }
                         }
                         binding.textViewActiveOffersCount.text = activeOfferCount.toString()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.textViewActiveOffersCount.text = "Error"
                    Log.w("CompanyHome", "Error al cargar contador de ofertas: ", error.toException())
                }
            })
        loadNewApplicantsCount(companyId)
    }
    private fun loadNewApplicantsCount(companyId: String) {
        val offersRef = database.getReference("offers")
        val applicationsRef = database.getReference("offerApplications")
        var totalNewApplicants = 0
        val offerIdsForCompany = mutableListOf<String>()

        offersRef.orderByChild("companyId").equalTo(companyId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(offersSnapshot: DataSnapshot) {
                    if (!offersSnapshot.exists()) {
                        binding.textViewNewApplicantsCount.text = "0"
                        return
                    }

                    offersSnapshot.children.forEach { offerData ->
                        offerData.key?.let { offerIdsForCompany.add(it) }
                    }

                    if (offerIdsForCompany.isEmpty()) {
                        binding.textViewNewApplicantsCount.text = "0"
                        return
                    }

                    var queriesCompleted = 0

                    offerIdsForCompany.forEach { offerId ->
                        applicationsRef.child(offerId).orderByChild("status").equalTo("pending")
                            .addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(appsSnapshot: DataSnapshot) {

                                    updateApplicantCountForOffer(offerId, appsSnapshot.childrenCount.toInt())
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.w("CompanyHome", "Error al cargar aplicaciones para oferta $offerId: ", error.toException())
                                    updateApplicantCountForOffer(offerId, 0)
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.textViewNewApplicantsCount.text = "Error"
                    Log.w("CompanyHome", "Error al cargar ofertas de la empresa: ", error.toException())
                }
            })
    }
    private val newApplicantsPerOffer = mutableMapOf<String, Int>()

    private fun updateApplicantCountForOffer(offerId: String, count: Int) {
        newApplicantsPerOffer[offerId] = count
        recalculateTotalNewApplicants()
    }

    private fun recalculateTotalNewApplicants() {
        var total = 0
        newApplicantsPerOffer.values.forEach { count ->
            total += count
        }
        binding.textViewNewApplicantsCount.text = total.toString()
    }
    private fun setupQuickTip() {
        val tipsArray = resources.getStringArray(R.array.quick_company_tips)
        if (tipsArray.isNotEmpty()) {
            val randomTip = tipsArray.random()
            binding.textViewQuickTip.text = randomTip
            binding.cardViewQuickTip.visibility = View.VISIBLE
        } else {
            binding.cardViewQuickTip.visibility = View.GONE
        }
    }

}