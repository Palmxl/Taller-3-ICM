package com.example.taller_3_icm.menu

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taller_3_icm.databinding.ActivityListaDisponiblesBinding
import com.example.taller_3_icm.model.User
import com.google.firebase.database.*

class ListaDisponiblesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListaDisponiblesBinding
    private lateinit var ref: DatabaseReference
    private val lista = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListaDisponiblesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerUsuarios.layoutManager = LinearLayoutManager(this)
        ref = FirebaseDatabase.getInstance().getReference("users")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                lista.clear()
                for (userSnap in snapshot.children) {
                    val user = userSnap.getValue(User::class.java)
                    if (user != null && user.status == "available") {
                        lista.add(user)
                    }
                }
                binding.recyclerUsuarios.adapter = UsuarioAdapter(this@ListaDisponiblesActivity, lista)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
