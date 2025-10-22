package com.example.taller_3_icm.menu

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.taller_3_icm.databinding.ItemUsuarioBinding
import com.example.taller_3_icm.model.User

class UsuarioAdapter(
    private val context: Context,
    private val lista: List<User>
) : RecyclerView.Adapter<UsuarioAdapter.UsuarioViewHolder>() {

    inner class UsuarioViewHolder(val binding: ItemUsuarioBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val binding = ItemUsuarioBinding.inflate(LayoutInflater.from(context), parent, false)
        return UsuarioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val user = lista[position]
        holder.binding.tvNombreUsuario.text = "${user.nombre} ${user.apellido}"
        Glide.with(context).load(user.imagenUrl).into(holder.binding.imgUsuario)

        holder.binding.btnVerPosicion.setOnClickListener {
            val intent = Intent(context, VerPosicionActivity::class.java)
            intent.putExtra("lat", user.latitud)
            intent.putExtra("lon", user.longitud)
            intent.putExtra("nombre", "${user.nombre} ${user.apellido}")
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = lista.size
}