package com.kodego.velascoben.todolistappfire.db

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class TodoDao {

    var dbReference : DatabaseReference = Firebase.database.reference

    fun add(todo: Todo) {
        dbReference.push().setValue(todo)
    }

    fun get(): Query {
        return dbReference.orderByKey()
    }

    fun remove(key: String) {
        dbReference.child(key).removeValue()
    }

    fun updateStatus(key : String, map : Map <String,Boolean>) {
        dbReference.child(key).updateChildren(map)
    }

    fun update(key : String, map : Map <String,String>) {
        dbReference.child(key).updateChildren(map)
    }

}