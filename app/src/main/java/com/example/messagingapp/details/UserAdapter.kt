package com.example.messagingapp.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.messagingapp.R

class UserAdapter(private var userList: List<User>, private val clickListener: (User) -> Unit) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    fun updateList(newList: List<User>) {
        userList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_item, parent, false)
        return UserViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.bind(user, clickListener)
    }

    override fun getItemCount() = userList.size

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userNameView: TextView = itemView.findViewById(R.id.userName)
        private val newMessageCountView: TextView = itemView.findViewById(R.id.newMessageCount)

        fun bind(user: User, clickListener: (User) -> Unit) {
            userNameView.text = user.name
            newMessageCountView.text = if (user.newMessageCount > 0) {
                user.newMessageCount.toString()
            } else {
                ""
            }
            itemView.setOnClickListener { clickListener(user) }
        }
    }
}
