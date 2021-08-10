package dk.aivclab.demo.usecases.detection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.display.DisplayManager;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.view.PreviewView;

import org.pytorch.IValue;
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
import dk.aivclab.demo.usecases.detection.utilities.CategoricalBoundingBox;
import dk.aivclab.demo.usecases.detection.utilities.drawing.BorderedText;
import dk.aivclab.demo.usecases.detection.views.OverlayView;
import dk.aivclab.demo.utilities.DisplayHelperFunctions;
import dk.aivclab.demo.utilities.ImageUtils;


public class DetectionActivity extends CameraXActivity {

    private final Paint boxPaint = new Paint();
    private final BorderedText borderedText = new BorderedText();
    PreviewView textureView;
    OverlayView bb_overlay;
    CategoricalBoundingBox[] categoricalBoundingBoxes;
    private boolean mAnalyzeImageErrorState;
    private FloatBuffer mInputTensorBuffer;
    private Tensor mInputTensor;
    private long mMovingAvgSum = 0;
    private final Queue<Long> mMovingAvgQueue = new LinkedList<>();
    private long mLastAnalysisResultTime;

    protected String getModuleAssetName() {
        return Constants.MODEL_NAME;
    }

    static class AnalysisResult {

        private final long analysisDuration;
        private final long moduleForwardDuration;
        private final CategoricalBoundingBox[] detections;

        AnalysisResult(CategoricalBoundingBox[] detections, long moduleForwardDuration, long analysisDuration) {
            this.detections = detections;
            this.moduleForwardDuration = moduleForwardDuration;
            this.analysisDuration = analysisDuration;
        }
    }

    @Override
    protected int getContentViewLayoutId() {
        return R.layout.activity_image_detection;
    }

    @Override
    protected UseCase[] getUseCases() {
        textureView = findViewById(R.id.activity_image_preview_view);
        bb_overlay = findViewById(R.id.detection_overlay_view);
        bb_overlay.addCallback(this::drawBoundingBoxes);

        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(10.0f);
        boxPaint.setStrokeCap(Paint.Cap.ROUND);
        boxPaint.setStrokeJoin(Paint.Join.ROUND);
        boxPaint.setStrokeMiter(100);

        float textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                Constants.TEXT_SIZE_DIP,
                this.getResources().getDisplayMetrics());
        borderedText.setTextSize(textSizePx);

        mDisplay = ((DisplayManager) getSystemService(Context.DISPLAY_SERVICE)).getDisplays()[0];

        rotation = mDisplay.getRotation();
        final Preview preview = new Preview.Builder().setTargetAspectRatio(DisplayHelperFunctions.getAspectRatio(
                mDisplay))     // We request aspect ratio but no resolution
                .setTargetRotation(rotation)        // Set initial target rotation
                .build();

        preview.setSurfaceProvider(textureView.getSurfaceProvider());

        final ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().
                setTargetRotation(rotation).
                setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).
                setTargetResolution(new Size(Constants.INPUT_TENSOR_WIDTH, Constants.INPUT_TENSOR_HEIGHT)).
                setBackgroundExecutor(mBackgroundExecutor).build();

        imageAnalysis.setAnalyzer(mBackgroundExecutor, this::analyse);

        return new UseCase[]{preview, imageAnalysis};
    }

    public synchronized void drawBoundingBoxes(final Canvas canvas) {
        if (categoricalBoundingBoxes != null && categoricalBoundingBoxes.length > 0) {
            rotation = mDisplay.getRotation();
            final boolean rotated = rotation % 180 == 90;

            int width = textureView.getWidth();
            int height = textureView.getHeight();

            final float multiplier = Math.min(canvas.getHeight() / (float) (rotated ? width : height),
                    canvas.getWidth() / (float) (rotated ? height : width));

            Matrix frameToCanvasMatrix = ImageUtils.getTransformationMatrix(width,
                    height,
                    (int) (multiplier * (rotated ? height : width)),
                    (int) (multiplier * (rotated ? width : height)),
                    rotation,
                    false);

            float margin = boxPaint.getStrokeWidth() / 2;
            for (CategoricalBoundingBox recognition : categoricalBoundingBoxes) {
                final RectF bb = new RectF(recognition.getLocation());
                frameToCanvasMatrix.mapRect(bb);
                boxPaint.setColor(recognition.getColor());
                canvas.drawRect(bb, boxPaint);

                borderedText.drawText(canvas,
                        bb.left + margin,
                        bb.top + margin,
                        String.format(Locale.ENGLISH,
                                "%s %.2f",
                                recognition.getTitle(),
                                (100 * recognition.getConfidence())) + "%",
                        boxPaint);
            }
        }
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
        /*final String moduleFileAbsoluteFilePath = new File(FileUtilities.assetFilePath(this,
            getModuleAssetName())).getAbsolutePath();
        mTorchModule = Module.load(moduleFileAbsoluteFilePath); //TODO: Load Model if not loaded

         */
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
            //IValue[] iValues = mTorchModule.forward(IValue.from(mInputTensor)).toTuple();
            IValue[] iValues = Constants.TEST_INPUT; //TODO: Use Model
            final long moduleForwardDuration = SystemClock.elapsedRealtime() - moduleForwardStartTime;

            int inputSize = textureView.getWidth();

            final CategoricalBoundingBox[] detections = new CategoricalBoundingBox[Constants.NUM_DETECTIONS];

            for (int bb_id = 0; bb_id < Constants.NUM_DETECTIONS; ++bb_id) {

                IValue[] t = iValues[bb_id].toTuple();
                float[] output_bb_xy_wh = t[0].toTensor().getDataAsFloatArray();
                float score = t[1].toTensor().getDataAsFloatArray()[0];
                float outputClass = t[2].toTensor().getDataAsFloatArray()[0];

                detections[bb_id] = new CategoricalBoundingBox(bb_id,
                        Constants.labels[((int) outputClass)],
                        Constants.colors[((int) outputClass)],
                        score,
                        new RectF(output_bb_xy_wh[1] * inputSize,
                                output_bb_xy_wh[0] * inputSize,
                                output_bb_xy_wh[3] * inputSize,
                                output_bb_xy_wh[2] * inputSize));
            }

            final long analysisDuration = SystemClock.elapsedRealtime() - startTime;
            return new AnalysisResult(detections, moduleForwardDuration, analysisDuration);
        } catch (Exception e) {
            Log.e(ProjectConstants.PROJECT_TAG, "Error during image analysis", e);
            mAnalyzeImageErrorState = true;
            runOnUiThread(() -> {
                if (!isFinishing()) {
                    DetectionActivity.this.finish();
                }
            });

        }
        return null;
    }

    protected void drawResult(AnalysisResult result) {
        categoricalBoundingBoxes = result.detections;
        bb_overlay.postInvalidate();

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
