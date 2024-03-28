package adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ermes.databinding.RecordListItemBinding
import models.EmpRecord

// final submission
class RecordsAdapter(private val recordsList: List<EmpRecord>) :
    RecyclerView.Adapter<RecordsAdapter.RecordHolder>() {


    inner class RecordHolder(binding: RecordListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        var binding: RecordListItemBinding
        init {
            this.binding = binding
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordHolder {
        return RecordHolder(
            RecordListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            )
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecordHolder, position: Int) {
        val record=recordsList[position]
        holder.binding.name.text = record.name
        holder.binding.date.text = record.date.toString()
        holder.binding.checkedInTime.text = record.checkedIn
        holder.binding.checkedOutTime.text = record.checkedOut
        /*Glide.with(holder.itemView).load(record.profileImg)
            .placeholder(R.drawable.ic_account)
            .into(holder.binding.profileImg)*/

    }

    override fun getItemCount(): Int {
        return recordsList.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }


}