package com.yigit.artbook

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.yigit.artbook.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private lateinit var artList : ArrayList<Art>
    private lateinit var artAdapter: ArtAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        artList = ArrayList<Art>()

        bindArtListAdapter()

        getArtsFromDb()
    }

    private fun bindArtListAdapter(){
        artAdapter = ArtAdapter(artList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = artAdapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.art_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.add_art_item){
            val intent = Intent(this@MainActivity,ArtDetailActivity::class.java)
            intent.putExtra("info","new")
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getArtsFromDb(){
        try {
            val database = this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)
            val cursor = database.rawQuery("SELECT * FROM arts ORDER BY art_name",null)
            val artNameIndex = cursor.getColumnIndex("art_name")
            val idIndex = cursor.getColumnIndex("id")
            while(cursor.moveToNext()){
                val name = cursor.getString(artNameIndex)
                val id = cursor.getInt(idIndex)
                val art = Art(id,name)
                artList.add(art)
            }
            artAdapter.notifyDataSetChanged()
            cursor.close()

        } catch (e:Exception){
            e.printStackTrace()
        }
    }
}