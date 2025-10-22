package com.example.taller_3_icm.menu

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.taller_3_icm.auth.LoginActivity
import com.example.taller_3_icm.databinding.ActivityMenuBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding
    private lateinit var auth: FirebaseAuth
    private var disponible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return

        binding.btnDisponibilidad.setOnClickListener {
            disponible = !disponible
            val estado = if (disponible) "available" else "offline"

            FirebaseDatabase.getInstance().getReference("users/$uid/status")
                .setValue(estado)

            binding.btnDisponibilidad.text =
                if (disponible) "Desconectarme" else "Conectarme (Disponible)"

            Toast.makeText(this, "Estado cambiado a $estado", Toast.LENGTH_SHORT).show()
        }

        binding.btnLista.setOnClickListener {
            startActivity(Intent(this, ListaDisponiblesActivity::class.java))
        }

        binding.btnCerrarSesion.setOnClickListener {
            FirebaseDatabase.getInstance().getReference("users/$uid/status").setValue("offline")
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}