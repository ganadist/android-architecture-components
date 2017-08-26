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

package com.example.android.persistence.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.databinding.ObservableField

import com.example.android.persistence.db.DatabaseCreator
import com.example.android.persistence.db.entity.CommentEntity
import com.example.android.persistence.db.entity.ProductEntity

class ProductViewModel(application: Application,
                       private val mProductId: Int) :
        AndroidViewModel(application) {
    companion object {
        private val ABSENT = MutableLiveData<Any>()
    }
    init {
        ABSENT.setValue(null)
    }

    val observableProduct: LiveData<ProductEntity>
    var product = ObservableField<ProductEntity>()

    /**
     * Expose the LiveData Comments query so the UI can observe it.
     */
    val comments: LiveData<List<CommentEntity>>

    init {
        val databaseCreator = DatabaseCreator.getInstance(this.getApplication())

        comments = Transformations.switchMap(databaseCreator.isDatabaseCreated) { isDbCreated ->
            if (isDbCreated) {
                databaseCreator.database!!.commentDao().loadComments(mProductId)
            } else {
                ABSENT as MutableLiveData<List<CommentEntity>>
            }
        }
        observableProduct = Transformations.switchMap(databaseCreator.isDatabaseCreated) { isDbCreated ->
            if (isDbCreated) {
                databaseCreator.database!!.productDao().loadProduct(mProductId)
            } else {
                ABSENT as MutableLiveData<ProductEntity>
            }
        }
        databaseCreator.createDb(this.getApplication())
    }

    fun setProduct(product: ProductEntity) {
        this.product.set(product)
    }

    /**
     * A creator is used to inject the product ID into the ViewModel
     *
     *
     * This creator is to showcase how to inject dependencies into ViewModels. It's not
     * actually necessary in this case, as the product ID can be passed in a public method.
     */
    class Factory(private val mApplication: Application, private val mProductId: Int) :
            ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProductViewModel(mApplication, mProductId) as T
        }
    }
}
