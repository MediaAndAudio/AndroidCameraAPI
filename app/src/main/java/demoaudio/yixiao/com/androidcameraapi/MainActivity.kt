package demoaudio.yixiao.com.androidcameraapi

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
    fun btnClick(v: View){
        when(v.id){
            R.id.texture_view_btn-> startTextureview()
            R.id.surface_view_btn-> startSurfaceview()
        }
    }
    fun startTextureview(){
        showToast("startTextureview")
        val mIntent = Intent(this@MainActivity,CameraActivity::class.java)
        mIntent.putExtra("From",0)
        startActivity(mIntent)
    }
    fun startSurfaceview(){
        showToast("startSurfaceview")

        val mIntent = Intent(this@MainActivity,CameraActivity::class.java)
        mIntent.putExtra("From",1)
        startActivity(mIntent)
    }

    private fun showToast(msg:String){
        Toast.makeText(this,msg, Toast.LENGTH_LONG).show()
    }
}
