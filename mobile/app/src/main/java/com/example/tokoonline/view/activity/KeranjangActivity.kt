package com.example.tokoonline.view.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.tokoonline.core.base.BaseActivity
import com.example.tokoonline.core.util.OnItemCheckBoxListener
import com.example.tokoonline.core.util.OnItemClick
import com.example.tokoonline.core.util.moneyFormatter
import com.example.tokoonline.data.model.ProdukKeranjang
import com.example.tokoonline.data.repository.KeranjangRepository
import com.example.tokoonline.databinding.ActivityKeranjangBinding
import com.example.tokoonline.view.adapter.AdapterKeranjang
import com.example.tokoonline.view.viewmodel.KeranjangViewModel
import kotlinx.coroutines.launch

class KeranjangActivity : BaseActivity() {

    private var uuid = ""
    private val viewModel: KeranjangViewModel by viewModels()

    private lateinit var binding: ActivityKeranjangBinding
    private lateinit var keranjangRepository: KeranjangRepository
    private val adapter: AdapterKeranjang by lazy {
        AdapterKeranjang(object : OnItemClick {
            override fun onClick(data: Any, position: Int) {
                showProgressDialog()

                val item = data as AdapterKeranjang.Companion.ItemData
                val produk = item.produk

                adapter.updateData(produk.qty, position, item.isIncrement, item.isChecked)

                // update keranjang in background
                if (produk.qty <= 0) { // delete
                    keranjangRepository.removeKeranjang(uuid, item.produk) {
                        if (!it) {
                            showToast("gagal update produk dalam keranjang")
                            finish()
                        } else {
                            viewModel.removeTotalBelanja(produk.harga)
                            dismissProgressDialog()
                        }
                    }
                } else {
                    keranjangRepository.updateKeranjang(uuid, item.produk) {
                        if (!it) {
                            showToast("gagal update produk dalam keranjang")
                            finish()
                        } else {
                            if (item.isChecked) {
                                if (item.isIncrement) viewModel.addTotalBelanja(produk.harga)
                                else viewModel.removeTotalBelanja(produk.harga)
                                dismissProgressDialog()
                            }
                        }
                    }
                }
            }
        }, object : OnItemCheckBoxListener {
            override fun onCheckBoxClick(isChecked: Boolean, data: Any, position: Int) {
                val produk = data as ProdukKeranjang
                if (isChecked) {
                    viewModel.addProdukToBePaid(data)
                    viewModel.addTotalBelanja(produk.harga * produk.qty)
                } else {
                    viewModel.removeProdukToBePaid(data)
                    viewModel.removeTotalBelanja(produk.harga * produk.qty)
                }

                adapter.updateData(produk.qty, position, false, isChecked)
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKeranjangBinding.inflate(layoutInflater)
        keranjangRepository = KeranjangRepository(this)
        setContentView(binding.root)

        binding.btnBayar.setOnClickListener{
            val produkList = viewModel.produkToBePaid
            val totalBelanja = viewModel.totalBelanja.value ?: 0
            goToBayar(totalBelanja, produkList)
        }

        initView()
        lifecycleScope.launch {
            userRepository.uid?.let {
                uuid = it
                getKeranjang(uuid)
            }
        }
    }

    private fun initView() {
        binding.rvProduk.adapter = adapter

        viewModel.totalBelanja.observe(this) { total ->
            binding.tvTotal.text = moneyFormatter(total)
        }

    }

    private suspend fun getKeranjang(it: String) {
        showProgressDialog()

        keranjangRepository.getKeranjang(userUid = it).collect {
            dismissProgressDialog()
            if (it.isEmpty() || it[0] == null) {
                showToast("Keranjang kosong")
            } else adapter.submitList(it)
        }
    }
}