package dk.aivclab.demo.usecases.classification.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import dk.aivclab.demo.R;

public class ResultHeaderView extends RelativeLayout {

  public final TextView nameTextView;
  public final TextView scoreTextView;

  public ResultHeaderView(@NonNull Context context) {
    this(context, null);
  }

  public ResultHeaderView(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ResultHeaderView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  public ResultHeaderView(@NonNull Context context,
                          @Nullable AttributeSet attrs,
                          int defStyleAttr,
                          int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    inflate(context, R.layout.image_classification_result_header, this);
    nameTextView = findViewById(R.id.result_header_name_text);
    scoreTextView = findViewById(R.id.result_header_score_text);
    TypedArray a = context.getTheme()
        .obtainStyledAttributes(attrs, R.styleable.ResultRowView, defStyleAttr, defStyleRes);
    try {
      final @StyleRes int textAppearanceResId = a.getResourceId(R.styleable.ResultRowView_textAppearance,
          R.style.TextAppearanceImageClassificationResult);

      nameTextView.setTextAppearance(context, textAppearanceResId);
      scoreTextView.setTextAppearance(context, textAppearanceResId);
    } finally {
      a.recycle();
    }
  }
}
