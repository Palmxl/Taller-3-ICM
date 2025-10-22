package com.example.taller_3_icm.menu

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taller_3_icm.databinding.ActivityListaDisponiblesBinding
import com.example.taller_3_icm.model.User
import com.google.firebase.database.*

class ListaDisponiblesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListaDisponiblesBinding
    private lateinit var ref: DatabaseReference
    private val lista = mutableListOf<User>()
    private val estadoAnterior = mutableMapOf<String, String>() // uid -> status

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListaDisponiblesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerUsuarios.layoutManager = LinearLayoutManager(this)
        ref = FirebaseDatabase.getInstance().getReference("users")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listaTemporal = mutableListOf<User>()

                for (userSnap in snapshot.children) {
                    val user = userSnap.getValue(User::class.java)
                    val uid = userSnap.key ?: continue
                    val statusNuevo = user?.status ?: "offline"
                    val statusViejo = estadoAnterior[uid]

                    // Detectar cambios de estado
                    if (statusViejo != null && statusViejo != statusNuevo) {
                        if (statusNuevo == "available") {
                            val nombre = "${user?.nombre} ${user?.apellido}"
                            Toast.makeText(
                                this@ListaDisponiblesActivity,
                                "$nombre se ha conectado",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (statusNuevo == "offline") {
                            val nombre = "${user?.nombre} ${user?.apellido}"
                            Toast.makeText(
                                this@ListaDisponiblesActivity,
                                "$nombre se ha desconectado",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    // Guardar el nuevo estado para futuras comparaciones
                    estadoAnterior[uid] = statusNuevo

                    // Agregar a lista solo si está disponible
                    if (user != null && statusNuevo == "available") {
                        listaTemporal.add(user)
                    }
                }

                // Actualizar RecyclerView
                lista.clear()
                lista.addAll(listaTemporal)
                binding.recyclerUsuarios.adapter =
                    UsuarioAdapter(this@ListaDisponiblesActivity, lista)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
