package com.cameratextreader

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.cameratextreader.databinding.ActivityMainBinding
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var btnScan: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        //To force app to only light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(binding.root)

        getViews()
        setInputImageDialog()
    }

    private fun getViews() {
        btnScan = binding.btnScan
    }

    private fun setInputImageDialog(){
        val popUpMenu = PopupMenu(this, btnScan)

        popUpMenu.inflate(R.menu.menu_input_image)

        popUpMenu.setOnMenuItemClickListener {menuItem ->
            goToScannerActivity(menuItem.itemId)
            true
        }

        setBtnScanListener(popUpMenu)
    }

    @SuppressLint("DiscouragedPrivateApi")
    private fun setBtnScanListener(popUpMenu: PopupMenu) {
        btnScan.setOnClickListener {
            try{
                val popup = PopupMenu::class.java.getDeclaredField("mPopup")
                popup.isAccessible = true
                val menu = popup.get(popUpMenu)
                menu.javaClass
                    .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                    .invoke(menu, true)

            }catch (e:Exception){
                Log.e("MainError", e.toString())
            }finally {
                popUpMenu.show()
            }
        }
    }

    private fun goToScannerActivity(itemId: Int) {
        val intent = Intent(this, ScannerActivity::class.java)

        when(itemId) {
            R.id.camera -> intent.putExtra("InputImage", 1)
            R.id.gallery -> intent.putExtra("InputImage", 2)
        }

        startActivity(intent)
    }
}