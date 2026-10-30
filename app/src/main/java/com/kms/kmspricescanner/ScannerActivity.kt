package com.kms.kmspricescanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var cameraExecutor: ExecutorService

    @Volatile
    private var isProcessing = false

    private var startTime: Long = 0
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var cameraProvider: ProcessCameraProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        startTime = System.currentTimeMillis()

        viewFinder = findViewById(R.id.viewFinder)
        val btnClose = findViewById<ImageButton>(R.id.btn_close_scanner)
        val btnSwitch = findViewById<ImageButton>(R.id.btn_switch_camera)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionsLauncher.launch(Manifest.permission.CAMERA)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        btnClose.setOnClickListener { finish() }

        btnSwitch.setOnClickListener {
            isProcessing = false
            startTime = System.currentTimeMillis()

            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcodeValue ->
                        // شرط الأمان: تجاهل القراءة في أول 1500 ميلي ثانية
                        if (System.currentTimeMillis() - startTime > 1500) {
                            handleScannedBarcode(barcodeValue)
                        }
                    })
                }

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e("ScannerActivity", "فشل تشغيل الكاميرا", exc)
                if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    startCamera()
                }
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun handleScannedBarcode(barcodeValue: String) {
        if (isProcessing) return

        synchronized(this) {
            if (isProcessing) return
            isProcessing = true

            // ✅ الماسح الذكي: يبحث في قاعدة البيانات المحلية (Room) فور التقاط الباركود
            lifecycleScope.launch {
                try {
                    cameraProvider?.unbindAll()

                    val db = AppDatabase.getDatabase(this@ScannerActivity)
                    val repo = ProductRepository(db.productDao())
                    val foundProducts = repo.searchProducts(barcodeValue)
                    val exactProduct = foundProducts.find { it.localBarcode == barcodeValue || it.globalBarcode == barcodeValue }

                    if (exactProduct != null) {
                        // المنتج مسجل: احفظه في السجل وافتح شاشة التفاصيل مباشرة
                        ScanHistoryHelper.saveProduct(this@ScannerActivity, exactProduct.localBarcode)

                        val intent = Intent(this@ScannerActivity, ProductDetailsActivity::class.java).apply {
                            putExtra("name", exactProduct.name)
                            putExtra("pricePiece", exactProduct.pricePiece)
                            putExtra("priceDozen", exactProduct.priceDozen)
                            putExtra("priceCarton", exactProduct.priceCarton)
                            putExtra("localBarcode", exactProduct.localBarcode)
                            putExtra("globalBarcode", exactProduct.globalBarcode)
                            putExtra("department", exactProduct.department)
                            putExtra("branch", exactProduct.branch)
                            putExtra("imageUrl", exactProduct.imageUrl)
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        // المنتج غير مسجل: أعد الباركود للصفحة الرئيسية لطلب الإضافة
                        val resultIntent = Intent()
                        resultIntent.putExtra("scanned_code", barcodeValue)
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                } catch (e: Exception) {
                    finish()
                }
            }
        }
    }

    private class BarcodeAnalyzer(private val onBarcodeFound: (String) -> Unit) : ImageAnalysis.Analyzer {

        // ✅ الفلتر الذكي: يتجاهل QR ويركز على الأكواد التجارية (1D)
        private val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_ITF
            )
            .build()

        private val scanner = BarcodeScanning.getClient(options)

        @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            val rawValue = barcode.rawValue
                            if (!rawValue.isNullOrEmpty()) {
                                onBarcodeFound(rawValue)
                                break
                            }
                        }
                    }
                    .addOnCompleteListener {
                        try { imageProxy.close() } catch (e: Exception) {}
                    }
            } else {
                imageProxy.close()
            }
        }
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "يجب السماح بالكاميرا", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        baseContext, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}