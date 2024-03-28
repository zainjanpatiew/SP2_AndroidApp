package adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ermes.R
import com.example.ermes.databinding.UserListItemBinding
import models.UserModel


class UsersAdapter(private val usersList: List<UserModel>) :
    RecyclerView.Adapter<UsersAdapter.UserHolder>() {


    inner class UserHolder(binding: UserListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        var binding: UserListItemBinding
        init {
            this.binding = binding
            binding.root.setOnClickListener { clickListener?.onUserClick(adapterPosition) }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserHolder {
        return UserHolder(
            UserListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            )
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: UserHolder, position: Int) {
        val user=usersList[position]
        holder.binding.name.text = user.first_name
        /*Glide.with(holder.itemView).load(user.)
            .placeholder(R.drawable.ic_account)
            .into(holder.binding.profileImg)*/

    }

    override fun getItemCount(): Int {
        return usersList.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun setOnClickListener(userClickListener: UserClickListener){
        clickListener=userClickListener
    }

    companion object {
        private var clickListener: UserClickListener? =null
    }

    interface UserClickListener{
        fun onUserClick(position: Int)
    }

}