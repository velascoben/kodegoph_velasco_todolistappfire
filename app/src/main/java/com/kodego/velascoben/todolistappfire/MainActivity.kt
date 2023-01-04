package com.kodego.velascoben.todolistappfire

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.kodego.velascoben.todolistappfire.databinding.ActivityMainBinding
import com.kodego.velascoben.todolistappfire.db.Todo
import com.kodego.velascoben.todolistappfire.db.TodoDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    lateinit var binding : ActivityMainBinding
    lateinit var adapter: TodoAdapter
    var dao = TodoDao()

    private lateinit var swipeHelper: ItemTouchHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val displayMetrics: DisplayMetrics = resources.displayMetrics
        val height = (displayMetrics.heightPixels / displayMetrics.density).toInt().dp
        val width = (displayMetrics.widthPixels / displayMetrics.density).toInt().dp

        val deleteIcon = resources.getDrawable(R.drawable.ic_outline_delete_24, null)
        val editIcon = resources.getDrawable(R.drawable.ic_outline_edit_24, null)

        val rvList = binding.recyclerView

        val deleteColor = resources.getColor(android.R.color.holo_red_light)
        val archiveColor = resources.getColor(android.R.color.holo_green_light)

        swipeHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                val task = adapter.todoModel[pos]

                if (direction == ItemTouchHelper.RIGHT) {
                    adapter.todoModel.removeAt(pos)
                    adapter.notifyDataSetChanged()
                    delete(task)
                    displayCount()
                    Snackbar.make(binding.root, "Item Deleted", Snackbar.LENGTH_LONG)
                        .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                        .setBackgroundTint(Color.parseColor("#000000"))
                        .setActionTextColor(Color.parseColor("#FFFFFF"))
                        .setAction("UNDO") {
                            adapter.todoModel.add(task)
                            save(task)
                            displayCount()
                            adapter.notifyDataSetChanged()
                        }.show()
                } else if (direction == ItemTouchHelper.LEFT) {
                    AddTodo(task).show(supportFragmentManager, "updateTodoTag")
                    adapter.notifyDataSetChanged()
                    view()



                }

            }

            override fun onChildDraw(
                canvas: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                //1. Background color based upon direction swiped
                when {
                    abs(dX) < width / 5 -> canvas.drawColor(Color.GRAY)
                    dX > width / 5 -> canvas.drawColor(deleteColor)
                    else -> canvas.drawColor(archiveColor)
                }

                //2. Printing the icons
                val textMargin = resources.getDimension(R.dimen.text_margin)
                    .roundToInt()
                deleteIcon.bounds = Rect(
                    textMargin,
                    viewHolder.itemView.top + textMargin + 18.dp, // Default 8.dp
                    textMargin + deleteIcon.intrinsicWidth,
                    viewHolder.itemView.top + deleteIcon.intrinsicHeight
                            + textMargin + 18.dp
                )
                editIcon.bounds = Rect(
                    width - textMargin - editIcon.intrinsicWidth,
                    viewHolder.itemView.top + textMargin + 18.dp,
                    width - textMargin,
                    viewHolder.itemView.top + editIcon.intrinsicHeight
                            + textMargin + 18.dp
                )

                //3. Drawing icon based upon direction swiped
                if (dX > 0) deleteIcon.draw(canvas) else editIcon.draw(canvas)

                super.onChildDraw(
                    canvas,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }
        }
        )

        view()

        swipeHelper.attachToRecyclerView(rvList)

        binding.btnAdd.setOnClickListener() {
            AddTodo(null).show(supportFragmentManager,"newTodoTag")
            adapter.notifyDataSetChanged()
            view()
        }

    }

    fun view() {

        dao.get().addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var employees : ArrayList<Todo> = ArrayList<Todo>()

                var dataFromDB = snapshot.children

                for (data in dataFromDB) {

                    // Get ID of Object from DB
                    var id = data.key.toString()

                    var task = data.child("task").value.toString()
                    var description = data.child("description").value.toString()
                    var status = data.child("status").value

                    var employee = Todo(id,task,description, status as Boolean)
                    employees.add(employee)
                }

                adapter = TodoAdapter(employees)
                binding.recyclerView.adapter = adapter
                binding.recyclerView.layoutManager = LinearLayoutManager(applicationContext)

                adapter.onClicking = {
                        item : Todo, position : Int ->

                    if (item.status) {
                        GlobalScope.launch(Dispatchers.IO) {
                            var mapData = mutableMapOf<String,Boolean>()
                            mapData["status"] = false
                            dao.updateStatus(item.id,mapData)
                            displayMessage("Task Reopened")
                            view()
                        }
                    } else {
                        GlobalScope.launch(Dispatchers.IO) {
                            var mapData = mutableMapOf<String,Boolean>()
                            mapData["status"] = true
                            dao.updateStatus(item.id,mapData)
                            displayMessage("Task Done")
                            view()
                        }
                    }

                }

                displayCount()

            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    fun addTodo(addsTodo : Todo) {
        adapter.todoModel.add(addsTodo)
    }

    private fun save(todo: Todo) {
        GlobalScope.launch(Dispatchers.IO) {
            dao.add(todo)
        }
    }

    private fun delete(item: Todo) {
        dao.remove(item.id)
        view()
    }

    fun displayMessage(message : String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
            .setBackgroundTint(Color.parseColor("#499C54"))
            .setActionTextColor(Color.parseColor("#FFFFFF"))
            .setAction("DISMISS") {
//                Toast.makeText(applicationContext,"Snackbar clicked",Toast.LENGTH_LONG).show()
            }.show()
    }

    fun displayCount() {
        if(adapter.todoModel.size < 1) {
            binding.tvTaskNumber.text = "No Tasks Today"
        } else if(adapter.todoModel.size == 1) {
            binding.tvTaskNumber.text = "1 Task Today"
        } else if(adapter.todoModel.size > 1) {
            binding.tvTaskNumber.text = "${adapter.itemCount} Tasks Today"
        }
    }

    fun dataChanged() {
        adapter.notifyDataSetChanged()
    }

    private val Int.dp
        get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            toFloat(), resources.displayMetrics
        ).roundToInt()
}