package dk.aivclab.demo.usecases.detection.utilities.drawing;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Typeface;

import java.util.Vector;

/**
 * A class that encapsulates the tedious bits of rendering legible, bordered text onto a canvas.
 */
public class BorderedText {
    private final Paint interiorPaint;
    private final Paint exteriorPaint;

    private float textSize;

    /**
     * Creates a left-aligned bordered text object with a white interior, a black exterior and 12 px textsize.
     */
    public BorderedText() {
        this(Color.WHITE, Color.BLACK, 12);
    }

    /**
     * Create a bordered text object with the specified interior and exterior colors, text size and
     * alignment.
     *
     * @param interiorColor the interior text color
     * @param exteriorColor the exterior text color
     * @param textSize      text size in pixels
     */
    public BorderedText(final int interiorColor, final int exteriorColor, final float textSize) {
        interiorPaint = new Paint();
        interiorPaint.setTextSize(textSize);
        interiorPaint.setColor(interiorColor);
        interiorPaint.setStyle(Style.FILL);
        interiorPaint.setAntiAlias(false);
        interiorPaint.setAlpha(255);

        exteriorPaint = new Paint();
        exteriorPaint.setTextSize(textSize);
        exteriorPaint.setColor(exteriorColor);
        exteriorPaint.setStyle(Style.FILL_AND_STROKE);
        exteriorPaint.setStrokeWidth(textSize / 8);
        exteriorPaint.setAntiAlias(false);
        exteriorPaint.setAlpha(255);

        this.textSize = textSize;
    }

    public void setTypeface(Typeface typeface) {
        interiorPaint.setTypeface(typeface);
        exteriorPaint.setTypeface(typeface);
    }

    public void drawLines(Canvas canvas, final float posX, final float posY, Vector<String> lines) {
        int lineNum = 0;
        for (final String line : lines) {
            drawText(canvas, posX, posY + getTextSize() * lineNum, line);
            ++lineNum;
        }
    }

    public void drawText(final Canvas canvas, final float posX, final float posY, final String text) {
        canvas.drawText(text, posX, posY, exteriorPaint);
        canvas.drawText(text, posX, posY, interiorPaint);
    }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float textSizePx) {
        textSize = textSizePx;
    }

    public void drawText(final Canvas canvas,
                         final float posX,
                         final float posY,
                         final String text,
                         Paint paint) {
        canvas.drawText(text, posX, posY, paint);
        canvas.drawText(text, posX, posY, interiorPaint);
    }

    public void setInteriorColor(final int color) {
        interiorPaint.setColor(color);
    }

    public void setExteriorColor(final int color) {
        exteriorPaint.setColor(color);
    }

    public void setAlpha(final int alpha) {
        interiorPaint.setAlpha(alpha);
        exteriorPaint.setAlpha(alpha);
    }

    public void getTextBounds(final String line, final int index, final int count, final Rect lineBounds) {
        interiorPaint.getTextBounds(line, index, count, lineBounds);
    }

    public void setTextAlign(final Align align) {
        interiorPaint.setTextAlign(align);
        exteriorPaint.setTextAlign(align);
    }
}
