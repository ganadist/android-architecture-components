/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.persistence.db

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.persistence.room.Room
import android.content.Context
import android.os.AsyncTask
import android.util.Log

import java.util.concurrent.atomic.AtomicBoolean

import com.example.android.persistence.db.AppDatabase.DATABASE_NAME

/**
 * Creates the [AppDatabase] asynchronously, exposing a LiveData object to notify of creation.
 */
class DatabaseCreator {

    private val mIsDatabaseCreated = MutableLiveData<Boolean>()

    var database: AppDatabase? = null
        private set

    private val mInitializing = AtomicBoolean(true)

    /** Used to observe when the database initialization is done  */
    val isDatabaseCreated: LiveData<Boolean>
        get() = mIsDatabaseCreated

    /**
     * Creates or returns a previously-created database.
     *
     *
     * Although this uses an AsyncTask which currently uses a serial executor, it's thread-safe.
     */
    fun createDb(context: Context) {

        Log.d("DatabaseCreator", "Creating DB from " + Thread.currentThread().name)

        if (!mInitializing.compareAndSet(true, false)) {
            return  // Already initializing
        }

        mIsDatabaseCreated.value = false// Trigger an update to show a loading screen.
        object : AsyncTask<Context, Void, Void>() {

            override fun doInBackground(vararg params: Context): Void? {
                Log.d("DatabaseCreator",
                        "Starting bg job " + Thread.currentThread().name)

                val context = params[0].applicationContext.apply {

                    // Reset the database to have new data on every run.
                    deleteDatabase(DATABASE_NAME)
                }

                // Build the database!
                val db = Room.databaseBuilder(context.applicationContext,
                        AppDatabase::class.java, DATABASE_NAME).build().run {
                    // Add a delay to simulate a long-running operation
                    addDelay()

                    // Add some data to the database
                    DatabaseInitUtil.initializeDb(this)
                    Log.d("DatabaseCreator",
                            "DB was populated in thread " + Thread.currentThread().name)

                    database = this
                }
                return null
            }

            override fun onPostExecute(ignored: Void) {
                // Now on the main thread, notify observers that the db is created and ready.
                mIsDatabaseCreated.value = true
            }
        }.execute(context.applicationContext)
    }

    private fun addDelay() {
        try {
            Thread.sleep(4000)
        } catch (ignored: InterruptedException) {
        }

    }

    companion object {
        private val sInstance: DatabaseCreator by lazy {
            DatabaseCreator()
        }

        @Synchronized
        @JvmStatic fun getInstance(context: Context): DatabaseCreator {
            return sInstance
        }
    }
}
