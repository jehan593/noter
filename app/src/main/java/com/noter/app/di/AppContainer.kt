package com.noter.app.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.noter.app.data.db.AppDatabase
import com.noter.app.data.repository.NotesRepository
import com.noter.app.data.repository.TasksRepository

private val Context.dataStore by preferencesDataStore(name = "noter_settings")

interface AppContainer {
    val database: AppDatabase
    val notesRepository: NotesRepository
    val tasksRepository: TasksRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: AppDatabase by lazy {
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }

    override val notesRepository: NotesRepository by lazy {
        NotesRepository(context.dataStore, context)
    }

    override val tasksRepository: TasksRepository by lazy {
        TasksRepository(database.taskDao(), context)
    }
}
