package com.professional.webview

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity(), Bindable {

    private val address = "https://dropmefiles.com/"

    private lateinit var merlin: Merlin
    private lateinit var merlinsBeard: MerlinsBeard
    lateinit var wvMain: WebView
    lateinit var pbLoading: ProgressBar
    private lateinit var tvNoInet: TextView

    private var results: Array<Uri>? = null

    private val REQUEST_SELECT_FILE = 1

    private var STORAGE_PERMISSION = 1
    private var CAMERA_PERMISSION = 2
    private val PERMISION_CODE = 3
    private var filePath: ValueCallback<Array<Uri>>? = null
    private var fileCameraMessage: String? = null

    private var PHOTO_PATH = "PhotoPath"
    private var FILE_PREFFIX = "file:"

    private var existsError = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wvMain = findViewById(R.id.wvMain)
        pbLoading = findViewById(R.id.pbLoading)
        tvNoInet = findViewById(R.id.tvNoInet)

        wvMain.setInitialScale(0)

        initWebView()

        merlin = Merlin.Builder()
            .withBindableCallbacks()
            .build(this)

        tvNoInet.setOnClickListener {
            existsError = false
            pbLoading.visibility = View.VISIBLE
            tvNoInet.visibility = View.GONE
            pbLoading.visibility = View.VISIBLE
            val handler = Handler()
            handler.postDelayed({
                if (merlinsBeard.isConnected) {
                    onConnect()
                } else {
                    tvNoInet.visibility = View.VISIBLE
                    wvMain.visibility = View.GONE
                    pbLoading.visibility = View.GONE
                }
            }, 2000)
        }

        merlinsBeard = MerlinsBeard.Builder().build(this)

        loadUrl()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        val webSettings = wvMain.settings
        webSettings.javaScriptEnabled = true
        webSettings.builtInZoomControls = true

        wvMain.webViewClient = object : WebViewClient() {

            @TargetApi(Build.VERSION_CODES.M)
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                view!!.clearHistory()

                if (error != null) {
                    if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            error.description.contains("net::ERR_")
                        } else {
                            TODO("VERSION.SDK_INT < M")
                        }
                    ) {
                        wvMain.visibility = View.GONE
                        tvNoInet.visibility = View.VISIBLE
                        existsError = true
                    }
                }
            }

            @SuppressWarnings("deprecation")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return Utils.urlActions(url, this@MainActivity)
            }


            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return Utils.urlActions(request.url.toString(), this@MainActivity)
            }
        }

        // Set web view chrome client
        wvMain.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (newProgress == 100 && !existsError) {
                    pbLoading.visibility = View.GONE
                    tvNoInet.visibility = View.GONE
                    wvMain.visibility = View.VISIBLE
                }
            }

            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                if (grantedPermission(STORAGE_PERMISSION) && grantedPermission(CAMERA_PERMISSION)) {
                    if (filePath != null) {
                        filePath!!.onReceiveValue(null)
                    }
                    filePath = filePathCallback
                    var takePictureIntent: Intent?
                    takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    if (takePictureIntent.resolveActivity(this@MainActivity.packageManager) != null) {
                        var photoFile: File? = null
                        try {
                            photoFile = Utils.createImage()
                            takePictureIntent.putExtra(PHOTO_PATH, fileCameraMessage)
                        } catch (ex: IOException) {
                            Log.d(this@MainActivity.localClassName, ex.toString())
                        }

                        if (photoFile != null) {
                            fileCameraMessage = FILE_PREFFIX + photoFile.absolutePath
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
                        } else {
                            takePictureIntent = null
                        }
                    }

                    val contentSelectionIntent = Utils.getContentSelectionIntent()

                    val chooserIntent =
                        Utils.getChooserIntent(this@MainActivity, contentSelectionIntent, takePictureIntent)
                    startActivityForResult(chooserIntent, REQUEST_SELECT_FILE)

                    return true
                } else {
                    requestPermissions()
                    return false
                }
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request?.resources!!.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                    request.grant(request.resources)
                }
            }
        }
    }

    //Checking permission for storage and camera for writing and uploading images
    fun requestPermissions() {
        val perms = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )

        //Checking for storage permission to write images for upload
        if (!grantedPermission(STORAGE_PERMISSION) && !grantedPermission(CAMERA_PERMISSION)) {
            ActivityCompat.requestPermissions(this@MainActivity, perms, PERMISION_CODE)

            //Checking for WRITE_EXTERNAL_STORAGE permission
        } else if (!grantedPermission(STORAGE_PERMISSION)) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISION_CODE
            )

            //Checking for CAMERA permissions
        } else if (!grantedPermission(CAMERA_PERMISSION)) {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.CAMERA), PERMISION_CODE)
        }
    }

    fun grantedPermission(permission: Int): Boolean {
        when (permission) {
            STORAGE_PERMISSION -> return ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            CAMERA_PERMISSION -> return ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == REQUEST_SELECT_FILE) {
            if (filePath == null) {
                Log.d(this@MainActivity.localClassName, "File path is empty")
                return
            }

            if (intent != null) {
                val dataString = intent.dataString
                if (dataString != null)
                    results = arrayOf(Uri.parse(dataString))
                else
                    if (fileCameraMessage != null)
                        results = arrayOf(Uri.parse(fileCameraMessage))
            }
            filePath?.onReceiveValue(results)
            filePath = null
        }
    }

    private fun loadUrl() {
        tvNoInet.visibility = View.GONE

        wvMain.loadUrl(address)
    }

    override fun onResume() {
        super.onResume()
        merlin.bind()
        merlin.registerBindable(this)
    }

    override fun onPause() {
        merlin.unbind()
        super.onPause()
    }

    override fun onBind(networkStatus: NetworkStatus) {
        if (!networkStatus.isAvailable) {
            onDisconnect()
            return
        }
        onConnect()
    }

    private fun onConnect() {
        tvNoInet.visibility = View.GONE
        loadUrl()
    }

    private fun onDisconnect() {
        tvNoInet.visibility = View.VISIBLE
        wvMain.visibility = View.GONE
        pbLoading.visibility = View.GONE
    }

    override fun onBackPressed() {
        when {
            wvMain.canGoBack() -> wvMain.goBack()
            else -> {
                super.onBackPressed()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        wvMain.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        wvMain.restoreState(savedInstanceState)
    }
}
