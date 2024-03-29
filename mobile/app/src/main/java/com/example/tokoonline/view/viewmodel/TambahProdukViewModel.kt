package com.example.tokoonline.view.viewmodel

import com.example.tokoonline.core.base.BaseViewModel
import com.example.tokoonline.data.model.firebase.Produk

class TambahProdukViewModel: BaseViewModel() {
    fun addData(produk: Produk, onComplete: (isSuccess: Boolean) -> Unit) {
        produkRepository.addProduk(produk) {
            onComplete(it)
        }
    }

    fun updateProduk(produk: Produk, onComplete: (isSuccess: Boolean) -> Unit) {
        produkRepository.updateProduk(produk, onComplete)
    }

}