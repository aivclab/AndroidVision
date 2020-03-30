package dk.aivclab.demo.usecases.segmentation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.view.PreviewView;

import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Objects;
import java.util.Queue;

import dk.aivclab.demo.CameraXActivity;
import dk.aivclab.demo.ProjectConstants;
import dk.aivclab.demo.R;
import dk.aivclab.demo.usecases.segmentation.utilities.SegmentationDrawing;
import dk.aivclab.demo.utilities.DisplayHelperFunctions;


public class SegmentationActivity extends CameraXActivity {

  PreviewView textureView;
  Bitmap unscaled_result;
  private boolean mAnalyzeImageErrorState;
  private FloatBuffer mInputTensorBuffer;
  private Tensor mInputTensor;
  private long mMovingAvgSum = 0;
  private Queue<Long> mMovingAvgQueue = new LinkedList<>();
  private long mLastAnalysisResultTime;
  private ImageView segmentationOverlayView;

  protected String getModuleAssetName() {
    return Constants.MODEL_NAME;
  }

  static class AnalysisResult {

    private final long analysisDuration;
    private final long moduleForwardDuration;
    private final float[] output;

    AnalysisResult(long moduleForwardDuration, long analysisDuration, float[] output) {
      this.output = output;
      this.moduleForwardDuration = moduleForwardDuration;
      this.analysisDuration = analysisDuration;
    }
  }

  @Override
  protected int getContentViewLayoutId() {
    return R.layout.activity_image_segmentation;
  }

  final protected UseCase[] getUseCases() {

    textureView = findViewById(R.id.activity_image_preview_view);
    segmentationOverlayView = findViewById(R.id.segmentation_overlay_image_view);

    unscaled_result = Bitmap.createBitmap(Constants.INPUT_TENSOR_WIDTH, // width
        Constants.INPUT_TENSOR_HEIGHT, // height
        Bitmap.Config.ARGB_8888);

    mDisplay = ((DisplayManager) getSystemService(Context.DISPLAY_SERVICE)).getDisplays()[0];
    PreviewView textureView = findViewById(R.id.activity_image_preview_view);

    rotation = mDisplay.getRotation();
    final Preview preview = new Preview.Builder().setTargetAspectRatio(DisplayHelperFunctions.getAspectRatio(
        mDisplay))     // We request aspect ratio but no resolution
        .setTargetRotation(rotation)        // Set initial target rotation
        .build();

    preview.setSurfaceProvider(textureView.getPreviewSurfaceProvider());

    final ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().
        setTargetRotation(rotation).
        setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).
        setTargetResolution(new Size(Constants.INPUT_TENSOR_WIDTH, Constants.INPUT_TENSOR_HEIGHT)).
        setBackgroundExecutor(mBackgroundExecutor).build();

    imageAnalysis.setAnalyzer(mBackgroundExecutor, this::analyse);

    return new UseCase[]{preview, imageAnalysis};
  }

  private void analyse(ImageProxy image) {
    if (!(SystemClock.elapsedRealtime() - mLastAnalysisResultTime < ProjectConstants.ANALYSIS_INTERVAL_MS)) {
      final AnalysisResult result = analyseImage(image, rotation);
      if (result != null) {
        mLastAnalysisResultTime = SystemClock.elapsedRealtime();
        runOnUiThread(() -> drawResult(result));
      }
    }

    image.close();
  }

  @SuppressLint("UnsafeExperimentalUsageError")
  @WorkerThread
  @Nullable
  protected AnalysisResult analyseImage(ImageProxy image, int rotationDegrees) {
    if (mAnalyzeImageErrorState) {
      return null;
    }

    try {
      if (mTorchModule == null) {
        //final String moduleFileAbsoluteFilePath = new File(FileUtilities.assetFilePath(this,            getModuleAssetName())).getAbsolutePath();
        //mTorchModule = Module.load(moduleFileAbsoluteFilePath); //TODO: Load Model

        mInputTensorBuffer = Tensor.allocateFloatBuffer(3
                                                        * Constants.INPUT_TENSOR_WIDTH
                                                        * Constants.INPUT_TENSOR_HEIGHT);
        mInputTensor = Tensor.fromBlob(mInputTensorBuffer,
            new long[]{1, 3, Constants.INPUT_TENSOR_HEIGHT, Constants.INPUT_TENSOR_WIDTH
            });
      }

      final long startTime = SystemClock.elapsedRealtime();
      TensorImageUtils.imageYUV420CenterCropToFloatBuffer(Objects.requireNonNull(image.getImage()),
          rotationDegrees,
          Constants.INPUT_TENSOR_WIDTH,
          Constants.INPUT_TENSOR_HEIGHT,
          TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
          TensorImageUtils.TORCHVISION_NORM_STD_RGB,
          mInputTensorBuffer,
          0);

      final long moduleForwardStartTime = SystemClock.elapsedRealtime();
      //final Tensor outputTensor = mTorchModule.forward(IValue.from(mInputTensor)).toTensor();
      final Tensor outputTensor = mInputTensor; //TODO: Use Model
      final long moduleForwardDuration = SystemClock.elapsedRealtime() - moduleForwardStartTime;

      final float[] scores = outputTensor.getDataAsFloatArray();

      final long analysisDuration = SystemClock.elapsedRealtime() - startTime;
      return new AnalysisResult(moduleForwardDuration, analysisDuration, scores);
    } catch (Exception e) {
      Log.e(ProjectConstants.PROJECT_TAG, "Error during image analysis", e);
      mAnalyzeImageErrorState = true;
      runOnUiThread(() -> {
        if (!isFinishing()) {
          SegmentationActivity.this.finish();
        }
      });

    }
    return null;
  }

  protected void drawResult(AnalysisResult result) {
    SegmentationDrawing.segmentResultToBitmap(result.output, Constants.colors, unscaled_result);
    segmentationOverlayView.setImageBitmap(SegmentationDrawing.resizeBitmap(unscaled_result,
        textureView.getWidth(),
        textureView.getHeight()));

    mMovingAvgSum += result.moduleForwardDuration;
    mMovingAvgQueue.add(result.moduleForwardDuration);

    if (mMovingAvgQueue.size() > ProjectConstants.MOVING_AVG_PERIOD) {
      mMovingAvgSum -= mMovingAvgQueue.remove();
    }

    mMsText.setText(String.format(Locale.US, ProjectConstants.FORMAT_MS, result.moduleForwardDuration));
    if (mMsText.getVisibility() != View.VISIBLE) {
      mMsText.setVisibility(View.VISIBLE);
    }
    mFpsText.setText(String.format(Locale.US,
        ProjectConstants.FORMAT_FPS,
        (1000.f / result.analysisDuration)));
    if (mFpsText.getVisibility() != View.VISIBLE) {
      mFpsText.setVisibility(View.VISIBLE);
    }

    if (mMovingAvgQueue.size() == ProjectConstants.MOVING_AVG_PERIOD) {
      float avgMs = (float) mMovingAvgSum / ProjectConstants.MOVING_AVG_PERIOD;
      mMsAvgText.setText(String.format(Locale.US, ProjectConstants.FORMAT_AVG_MS, avgMs));
      if (mMsAvgText.getVisibility() != View.VISIBLE) {
        mMsAvgText.setVisibility(View.VISIBLE);
      }
    }
  }

}
