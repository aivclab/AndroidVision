package dk.aivclab.demo.usecases.detection.utilities;

import android.graphics.RectF;


public final class CategoricalBoundingBox {

    private final Integer id;
    private final String title;
    private final int color;
    private final Float confidence;
    private final RectF location;

    public CategoricalBoundingBox(final Integer id,
                                  final String title,
                                  final int color,
                                  final Float confidence,
                                  final RectF location) {
        this.id = id;
        this.title = title;
        this.color = color;
        this.confidence = confidence;
        this.location = location;
    }

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getColor() {
        return color;
    }

    public Float getConfidence() {
        return confidence;
    }

    public RectF getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return String.format("Recognition{id=%d, title='%s', color='%d', confidence=%s, location=%s}",
                id,
                title,
                color,
                confidence,
                location);
    }
}
