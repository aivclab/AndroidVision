package dk.aivclab.demo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.UseCase;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import dk.aivclab.demo.usecases.classification.utilities.StatusBarUtils;

public abstract class CameraXActivity extends TorchModuleActivity {
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 200;
    private static final String[] PERMISSIONS = {Manifest.permission.CAMERA};
    protected static Display mDisplay;
    protected static int rotation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StatusBarUtils.setStatusBarOverlay(getWindow(), true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE_CAMERA_PERMISSION);
        } else {
            setupUseCases();
        }
    }

    private void setupUseCases() {

        CameraSelector cameraSelector = new CameraSelector.Builder().
                requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        Context as = this;
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(as);
        cameraProviderFuture.addListener(() -> {
                    try {
                        ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                        cameraProvider.unbindAll();
                        cameraProvider.bindToLifecycle((LifecycleOwner) as, cameraSelector, getUseCases());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, mBackgroundExecutor

        );
    }

    protected abstract UseCase[] getUseCases();

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this,
                        "You can't use image classification example without granting CAMERA permission",
                        Toast.LENGTH_LONG).show();
                finish();
            } else {
                setupUseCases();
            }
        }
    }
}
