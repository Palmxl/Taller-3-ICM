package com.example.taller_3_icm.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.taller_3_icm.databinding.ActivityWelcomeBinding
import com.example.taller_3_icm.menu.MenuActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return

        val ref = FirebaseDatabase.getInstance().getReference("users/$uid")
        ref.get().addOnSuccessListener { snapshot ->
            val nombre = snapshot.child("nombre").getValue(String::class.java) ?: ""
            val apellido = snapshot.child("apellido").getValue(String::class.java) ?: ""
            val imagenUrl = snapshot.child("imagenUrl").getValue(String::class.java)

            binding.tvNombre.text = "$nombre $apellido"

            // Cargar imagen con Glide si existe URL
            if (!imagenUrl.isNullOrEmpty()) {
                Glide.with(this).load(imagenUrl).into(binding.imgPerfilWelcome)
            }
        }

        // Nuevo botón para ir al Menú Principal
        binding.btnIrMenu.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        // Botón cerrar sesión
        binding.btnCerrarSesion.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
