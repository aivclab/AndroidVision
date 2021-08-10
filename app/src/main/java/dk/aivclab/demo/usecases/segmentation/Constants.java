package dk.aivclab.demo.usecases.segmentation;

import dk.aivclab.demo.usecases.segmentation.categories.PascalVoc;

class Constants {

    static final int INPUT_TENSOR_WIDTH = 224;
    static final int INPUT_TENSOR_HEIGHT = 224;

    static final String MODEL_NAME = "segmentation_model.pt";

    static final int[] colors = PascalVoc.colors;
}
