package dk.aivclab.demo.usecases.classification;

import dk.aivclab.demo.usecases.classification.categories.Dmr;

public class Constants {
  static final int INPUT_TENSOR_WIDTH = 224;
  static final int INPUT_TENSOR_HEIGHT = 224;
  static final int TOP_K = 3;
  static final String SCORES_FORMAT = "%.2f";

  static final String MODEL_NAME = Dmr.DMR_MODEL_NAME;
  static String[] CATEGORIES = Dmr.DMR_CATEGORIES;


}
