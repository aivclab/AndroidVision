package dk.aivclab.demo.utilities;

import android.util.DisplayMetrics;
import android.view.Display;

import androidx.camera.core.AspectRatio;

import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;

public class DisplayHelperFunctions {

  /**
   [androidx.camera.core.ImageAnalysisConfig] requires enum value of
   [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
   <p>
   Detecting the most suitable ratio for dimensions provided in @params by counting absolute
   of preview ratio to one of the provided values.

   @param display - Display object

   @return suitable aspect ratio
   */
  public static int getAspectRatio(Display display) {
    DisplayMetrics metrics = new DisplayMetrics();
    display.getRealMetrics(metrics);
    int width = metrics.widthPixels;
    int height = metrics.heightPixels;

    double previewRatio = ((double) max(width, height)) / min(width, height);
    double RATIO_4_3_VALUE = 4.0 / 3.0;
    double RATIO_16_9_VALUE = 16.0 / 9.0;
    if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
      return AspectRatio.RATIO_4_3;
    }
    return AspectRatio.RATIO_16_9;
  }
}
