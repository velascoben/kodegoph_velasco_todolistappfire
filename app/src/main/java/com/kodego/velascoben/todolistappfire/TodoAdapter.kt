package com.kodego.velascoben.todolistappfire

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kodego.velascoben.todolistappfire.databinding.RowItemBinding
import com.kodego.velascoben.todolistappfire.db.Todo

class TodoAdapter (var todoModel : MutableList<Todo>) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>(){

    inner class TodoViewHolder(var binding : RowItemBinding) : RecyclerView.ViewHolder(binding.root)

    var onClicking : ((Todo, Int) -> Unit) ? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = RowItemBinding.inflate(layoutInflater, parent, false)
        return TodoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.binding.apply {
            tvTask.text = todoModel[position].task
            if (todoModel[position].status) {
                tvTask.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                imgCheckBox.setImageResource(R.drawable.ic_box_check)
            } else {
                tvTask.paintFlags = tvTask.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                imgCheckBox.setImageResource(R.drawable.ic_box_blank)
            }

            imgCheckBox.setOnClickListener() {
                onClicking?.invoke(todoModel[position],position)
            }
        }
    }

    override fun getItemCount(): Int {
        return todoModel.size
    }

}