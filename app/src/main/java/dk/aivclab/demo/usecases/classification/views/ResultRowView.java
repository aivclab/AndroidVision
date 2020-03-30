package dk.aivclab.demo.usecases.classification.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import dk.aivclab.demo.R;

public class ResultRowView extends RelativeLayout {

  public final TextView nameTextView;
  public final TextView scoreTextView;
  public final ProgressBar scoreProgressBar;
  private boolean mIsInProgress = true;

  public ResultRowView(@NonNull Context context) {
    this(context, null);
  }

  public ResultRowView(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ResultRowView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  public ResultRowView(@NonNull Context context,
                       @Nullable AttributeSet attrs,
                       int defStyleAttr,
                       int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    inflate(context, R.layout.image_classification_result_row, this);
    nameTextView = findViewById(R.id.result_row_name_text);
    scoreTextView = findViewById(R.id.result_row_score_text);
    scoreProgressBar = findViewById(R.id.result_row_progess_bar);

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

  public void setProgressState(boolean isInProgress) {
    final boolean changed = isInProgress != mIsInProgress;
    mIsInProgress = isInProgress;
    if (isInProgress) {
      nameTextView.setVisibility(View.INVISIBLE);
      scoreTextView.setVisibility(View.INVISIBLE);
    } else {
      nameTextView.setVisibility(View.VISIBLE);
      scoreTextView.setVisibility(View.VISIBLE);
    }

    if (changed) {
      invalidate();
    }
  }
}
