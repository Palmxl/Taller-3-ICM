package com.example.taller_3_icm

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

data class SimpleUser(val name: String, val uid: String)

class MainActivity : AppCompatActivity() {

    private val users = listOf(
        SimpleUser("Persona 1", "uid_persona_1"),
        SimpleUser("Persona 2", "uid_persona_2"),
        SimpleUser("Persona 3", "uid_persona_3")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val lv = findViewById<ListView>(R.id.listUsers)
        lv.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            users.map { it.name }
        )
        // Comportamiento original: tocar un usuario abre FollowMapActivity (seguimiento)
        lv.setOnItemClickListener { _, _, position, _ ->
            val selected = users[position]
            val i = Intent(this, FollowMapActivity::class.java).apply {
                putExtra("TRACKED_UID", selected.uid)
                putExtra("TRACKED_NAME", selected.name)
            }
            startActivity(i)
        }

        // Nuevo: botón "Simular inicio de sesión" → abre HomeMapActivity (mapa + 5 POIs)
        findViewById<Button>(R.id.btnSimularLogin).setOnClickListener {
            startActivity(Intent(this, HomeMapActivity::class.java))
        }
    }
}
