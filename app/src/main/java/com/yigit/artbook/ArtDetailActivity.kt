package com.yigit.artbook

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.yigit.artbook.databinding.ActivityArtDetailBinding
import java.io.ByteArrayOutputStream

class ArtDetailActivity : AppCompatActivity() {

    private lateinit var binding : ActivityArtDetailBinding
    private lateinit var activityResultLauncher : ActivityResultLauncher<Intent>
    private lateinit var permissionResultLauncher : ActivityResultLauncher<String>
    var selectedBitmap : Bitmap? = null
    private lateinit var database : SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtDetailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)
        registerLaunchers()
        checkSourcePageAndReadData()
    }
    private fun checkSourcePageAndReadData(){
        val intent = intent
        val info = intent.getStringExtra("info")
        if(info.equals("new")){
            resetFields()
        }else{
            readData()
        }
    }

    private fun readData(){
        binding.btnSave.visibility = View.INVISIBLE
        val selectedId = intent.getIntExtra("id",0)
        val cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", arrayOf(selectedId.toString()))
        val artNameIndex = cursor.getColumnIndex("art_name")
        val artistNameIndex = cursor.getColumnIndex("artist_name")
        val yearIndex = cursor.getColumnIndex("year")
        val imageIndex = cursor.getColumnIndex("image")

        while(cursor.moveToNext()){
            binding.artNameText.setText(cursor.getString(artNameIndex))
            binding.artistNameText.setText(cursor.getString(artistNameIndex))
            binding.yearText.setText(cursor.getString(yearIndex))
            val byteArray = cursor.getBlob(imageIndex)
            val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
            binding.imageView.setImageBitmap(bitmap)
        }
        cursor.close()
    }

    private fun resetFields(){
        binding.artNameText.setText("")
        binding.artistNameText.setText("")
        binding.yearText.setText("")
        binding.btnSave.visibility = View.VISIBLE
        binding.imageView.setImageResource(R.drawable.imageplaceholder)
    }

    fun btnSaveOnClick(view: View) {
        val artName = binding.artNameText.text.toString()
        val artistName = binding.artistNameText.text.toString()
        val year = binding.yearText.text.toString()
        if(selectedBitmap != null){
            val smallBitMap = makeSmallerBitMap(selectedBitmap!!,300)
            val outputStream = ByteArrayOutputStream()
            smallBitMap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteArray = outputStream.toByteArray()

            try {
                database.execSQL("CREATE TABLE IF NOT EXISTS arts(id INTEGER PRIMARY KEY, art_name VARCHAR,artist_name VARCHAR, year VARCHAR,image BLOB)")
                val sqlString = "INSERT INTO arts(art_name,artist_name,year,image) VALUES(?,?,?,?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1,artName)
                statement.bindString(2,artistName)
                statement.bindString(3,year)
                statement.bindBlob(4,byteArray)
                statement.execute()
            }catch (e:java.lang.Exception){
                e.printStackTrace()
            }
            val intent = Intent(this@ArtDetailActivity,MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }

    private fun makeSmallerBitMap(image: Bitmap,maxSize: Int) : Bitmap{
        var width = image.width
        var height = image.height
        val bitmapRatio : Double = width.toDouble() / height.toDouble()
        if(bitmapRatio > 1){
            //landscape
            width = maxSize
            val scaledHeight = width/bitmapRatio
            height = scaledHeight.toInt()
        }else{
            //portrait
            height = maxSize
            val scaledWidth = height*bitmapRatio
            width = scaledWidth.toInt()
        }
        return Bitmap.createScaledBitmap(image,width,height,true)
    }

    fun selectImage(view: View) {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                Snackbar.make(view,"Permission Needed For Gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission",View.OnClickListener {
                    //Request Permission
                    permissionResultLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }).show()
            }else{
                //Request Permission
                permissionResultLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }else{
            val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)
        }
    }

    private fun registerLaunchers(){
        registerActivityResultLauncher()
        registerPermissionResultLauncher()
    }

    private fun registerActivityResultLauncher(){
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if(result.resultCode == RESULT_OK){
                val intentFromResult = result.data
                if(intentFromResult != null){
                    val imageData = intentFromResult.data
                    if(imageData != null){
                        try {
                            if(Build.VERSION.SDK_INT >= 28){
                                val source = ImageDecoder.createSource(this@ArtDetailActivity.contentResolver,imageData)
                                selectedBitmap = ImageDecoder.decodeBitmap(source)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }else{
                                selectedBitmap = MediaStore.Images.Media.getBitmap(contentResolver,imageData)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }
                        } catch(e:Exception){
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun registerPermissionResultLauncher(){
        permissionResultLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){result ->
            if(result){
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }else{
                Toast.makeText(this@ArtDetailActivity,"Permission Needed",Toast.LENGTH_LONG).show()
            }
        }
    }
}