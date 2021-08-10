package dk.aivclab.demo.usecases.segmentation.utilities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import org.pytorch.IValue;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.util.Map;

public class SegmentationDrawing {

    public static void processRes(Map<String, IValue> outTensors){
        //TODO: FINISH
        // see http://host.robots.ox.ac.uk:8080/pascal/VOC/voc2007/segexamples/index.html for the list of classes with indexes
         final int CLASS_NUM = 21;
         final int DOG = 12;
         final int PERSON = 15;
         final int SHEEP = 17;
        Bitmap mBitmap = BitmapFactory.decodeByteArray(new byte[]{},0,0);
        //mBitmap = BitmapFactory.decodeStream(getAssets().open(mImagename));
        //final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(mBitmap,                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
        //final float[] inputs = inputTensor.getDataAsFloatArray();
        //Map<String, IValue> outTensors = mModule.forward(IValue.from(inputTensor)).toDictStringKey();
        final Tensor outputTensor = outTensors.get("out").toTensor();
        final float[] scores = outputTensor.getDataAsFloatArray();
        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight();
        int[] intValues = new int[width * height];
        for (int j = 0; j < width; j++) {
            for (int k = 0; k < height; k++) {
                int maxi = 0, maxj = 0, maxk = 0;
                double max_num = -Double.MAX_VALUE;
                for (int i = 0; i < CLASS_NUM; i++) {
                    int i1 = i * (width * height) + j * width + k;
                    if (scores[i1] > max_num) {
                        max_num = scores[i1];
                        maxi = i;
                        maxj = j;
                        maxk = k;
                    }
                }
                if (maxi == PERSON)
                    intValues[maxj * width + maxk] = 0xFFFF0000;
                else if (maxi == DOG)
                    intValues[maxj * width + maxk] = 0xFF00FF00;
                else if (maxi == SHEEP)
                    intValues[maxj * width + maxk] = 0xFF0000FF;
                else
                    intValues[maxj * width + maxk] = 0xFF000000;
            }
        }

        Bitmap bmpSegmentation = Bitmap.createScaledBitmap(mBitmap, width, height, true);
        Bitmap outputBitmap = bmpSegmentation.copy(bmpSegmentation.getConfig(), true);
        outputBitmap.setPixels(intValues, 0, outputBitmap.getWidth(), 0, 0, outputBitmap.getWidth(), outputBitmap.getHeight());
        final Bitmap transferredBitmap = Bitmap.createScaledBitmap(outputBitmap, mBitmap.getWidth(), mBitmap.getHeight(), true);
        //mImageView.setImageBitmap(transferredBitmap);
    }

    public static void segmentResultToBitmap(float[] segmentedImage, int[] classColors, Bitmap targetBitmap) {
        int width = targetBitmap.getWidth();
        int height = targetBitmap.getHeight();
        int p_len = width * height;
        int asda_len = segmentedImage.length;

        //if(p_len != asda_len) throw new IllegalArgumentException(p_len + ":" + asda_len);

        int[] pixels = new int[p_len];

        for (int i = 0; i < asda_len / 3; i++) {
            int a = Math.abs((int) segmentedImage[i]);
            int c = classColors[a];
            pixels[i] = c;
        }

        targetBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    }

    public static Bitmap resizeBitmap(Bitmap bitmap, int width, int height) {
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        if (bitmap.getWidth() != bitmap.getHeight()) {
            throw new Error("Mask expected to be square but got something else");
        }
        // Expects image to be square shape
        Bitmap scaledBitmap;
        if (height > width) {
            scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, width, true);
        } else {
            scaledBitmap = Bitmap.createScaledBitmap(bitmap, height, height, true);
        }

        float pX = (width - scaledBitmap.getWidth()) / 2.0f;
        float pY = (height - scaledBitmap.getHeight()) / 2.0f;
        Canvas can = new Canvas(result);
        // can.drawARGB(0x00, 0xff, 0xff, 0xff)
        can.drawBitmap(scaledBitmap, pX, pY, null);
        // scaledBitmap.recycle()

        return result;
    }
}
