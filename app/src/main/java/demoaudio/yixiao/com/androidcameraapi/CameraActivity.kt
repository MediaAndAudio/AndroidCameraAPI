package demoaudio.yixiao.com.androidcameraapi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import demoaudio.yixiao.com.audiorecordandaudiotrack.PermissionUtils
import kotlinx.android.synthetic.main.activity_camera.*
import java.util.*


/**
 * 主要思路：
    获得摄像头管理器CameraManager mCameraManager，mCameraManager.openCamera()来打开摄像头
    指定要打开的摄像头，并创建openCamera()所需要的CameraDevice.StateCallback stateCallback
    在CameraDevice.StateCallback stateCallback中调用takePreview()，这个方法中，使用CaptureRequest.Builder创建预览需要的CameraRequest，并初始化了CameraCaptureSession，最后调用了setRepeatingRequest(previewRequest, null, childHandler)进行了预览
    点击屏幕，调用takePicture()，这个方法内，最终调用了capture(mCaptureRequest, null, childHandler)
    在new ImageReader.OnImageAvailableListener(){}回调方法中，将拍照拿到的图片进行展示
 */
class CameraActivity : AppCompatActivity(),View.OnClickListener {
    override fun onClick(p0: View?) {

    }
    private val REQUEST_CODE:Int = 1000
    var permissions = arrayOf(Manifest.permission.CAMERA)
    var from:Int = 0
    var isStart:Boolean = false
    var mCameraDevice: CameraDevice?=null
    private var  mSurfaceView: SurfaceView?=null
    private var  mSurfaceHolder:SurfaceHolder?=null
    private var  mCameraManager:CameraManager?=null//摄像头管理器
    private var  childHandler:Handler?=null
    private var  mainHandler: Handler?=null
    private var  mCameraID:String?=""//摄像头Id 0 为后  1 为前
    private var  mImageReader:ImageReader? = null
    private var  mCameraCaptureSession:CameraCaptureSession?=null
    //    var mSurfaceHolder: SurfaceHolder? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        if(null != intent){
            from = intent.getIntExtra("From",0)
            if (VersionUtils.checkSDKVersion(23)) {
                LogUtil.i("TAG", "checkSDKVersion(23)")
                val flag = PermissionUtils.checkPermissionAllGranted(this,permissions)
                LogUtil.i("TAG", "checkPermissionAllGranted)" + flag)
                if (!flag) {
                    PermissionUtils.RequestPermissionsRequestCodeValidator(this, permissions, REQUEST_CODE)
                    return
                }else{
                    init()
                }
            }

            get_btn.text = "启动"
        }

    }
    fun init(){
        when(from){
            0 -> initSufaceView()
            1 -> suface_view_camera.visibility = View.GONE
        }
    }
    fun initSufaceView(){
        texture_view_camera.visibility = View.GONE
        var  mSurfaceHolder = suface_view_camera.holder
        mSurfaceHolder.setKeepScreenOn(true)
        mSurfaceHolder.addCallback(object : SurfaceHolder.Callback{
            override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {

            }

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun surfaceDestroyed(p0: SurfaceHolder?) {
                // 释放Camera资源
                if (null != mCameraDevice) {
                    mCameraDevice!!.close()
                   mCameraDevice = null
                }
            }

            override fun surfaceCreated(p0: SurfaceHolder?) {
                // 初始化Camera
                initCamera2()
            }

        })
    }

    /**
     * 初始化Camera2
     */
    private fun initCamera2() {
        val handlerThread = HandlerThread("Camera2")
        handlerThread.start()
        childHandler = Handler(handlerThread.looper)
        mainHandler = Handler(mainLooper)
        mCameraID = "" + CameraCharacteristics.LENS_FACING_FRONT//后摄像头
        mImageReader = ImageReader.newInstance(1080, 1920, ImageFormat.JPEG, 1)
        mImageReader!!.setOnImageAvailableListener(ImageReader.OnImageAvailableListener { reader ->
            //可以在这里处理拍照得到的临时照片 例如，写入本地
            mCameraDevice!!.close()
            mSurfaceView!!.setVisibility(View.GONE)
            show_image.setVisibility(View.VISIBLE)
            // 拿到拍照照片数据
            val image = reader.acquireNextImage()
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)//由缓冲区存入字节数组
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                show_image.setImageBitmap(bitmap)
            }
        }, mainHandler)
        //获取摄像头管理
        mCameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            //打开摄像头
            mCameraManager!!.openCamera(mCameraID, stateCallback, mainHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }


    /**
     * 摄像头创建监听
     */
    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {//打开摄像头
            mCameraDevice = camera
            //开启预览
            takePreview()
        }

        override fun onDisconnected(camera: CameraDevice) {//关闭摄像头
            if (null != mCameraDevice) {
                mCameraDevice!!.close()
                mCameraDevice = null
            }
        }

        override fun onError(camera: CameraDevice, error: Int) {//发生错误
            showToast("摄像头开启失败")
        }
    }

    /**
     * 开始预览
     */
    private fun takePreview() {
        try {
            // 创建预览需要的CaptureRequest.Builder
            val previewRequestBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            // 将SurfaceView的surface作为CaptureRequest.Builder的目标
            previewRequestBuilder.addTarget(mSurfaceHolder!!.getSurface())
            // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求
            mCameraDevice!!.createCaptureSession(Arrays.asList(mSurfaceHolder!!.getSurface(), mImageReader!!.getSurface()), object : CameraCaptureSession.StateCallback() // ③
            {
                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    if (null == mCameraDevice) return
                    // 当摄像头已经准备好时，开始显示预览
                    mCameraCaptureSession = cameraCaptureSession
                    try {
                        // 自动对焦
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                        // 打开闪光灯
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                        // 显示预览
                        val previewRequest = previewRequestBuilder.build()
                        mCameraCaptureSession!!.setRepeatingRequest(previewRequest, null, childHandler)
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }

                }

                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                    showToast("配置失败")
                }
            }, childHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }
    private fun showToast(msg:String){
        Toast.makeText(this,msg, Toast.LENGTH_LONG).show()
    }

    fun btnClick(v: View){
        when(v.id){
            R.id.get_btn -> getCarema()
        }
    }

    fun getCarema(){
        if(isStart){
            //捕获
            takePreview()
        }else{
            //启动
            get_btn.text = "捕获"
            isStart = true
//            when(from){
//                0 ->
//                1 -> suface_view_camera.visibility = View.GONE
//            }
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            var isAllGranted = true
            // 判断是否所有的权限都已经授予了
            for (grant in grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false
                    break
                }
            }

            if (!isAllGranted) {
                // 弹出对话框告诉用户需要权限的原因, 并引导用户去应用权限管理中手动打开权限按钮
                showToast("您拒绝权限申请会导致部分功能无法正常使用，请在设置中将权限设置为允许！")
            }else{
                init()
            }
        }
    }
}
