package dk.aivclab.demo.usecases.detection;


import org.pytorch.IValue;
import org.pytorch.Tensor;

import dk.aivclab.demo.usecases.detection.categories.PascalVoc;

class Constants {

    static final int INPUT_TENSOR_WIDTH = 224;
    static final int INPUT_TENSOR_HEIGHT = 224;


    static final int NUM_DETECTIONS = 2;
    static final float TEXT_SIZE_DIP = 18;

    static final String[] labels = PascalVoc.labels;

    static final int[] colors = PascalVoc.colors;

    static final String MODEL_NAME = "detection_model.pt";

    static IValue[] TEST_INPUT = IValue.listFrom(IValue.tupleFrom(IValue.from(Tensor.fromBlob(new float[]{0.6f,
                    0.3f,
                    0.5f,
                    0.5f
            }, new long[]{4})),
            IValue.from(Tensor.fromBlob(new float[]{0.7f}, new long[]{1})),
            IValue.from(Tensor.fromBlob(new float[]{0.2f}, new long[]{1}))),
            IValue.tupleFrom(IValue.from(Tensor.fromBlob(new float[]{0.3f, 0.1f, 0.9f, 0.9f
                    }, new long[]{4})),
                    IValue.from(Tensor.fromBlob(new float[]{3f}, new long[]{1})),
                    IValue.from(Tensor.fromBlob(new float[]{2f}, new long[]{1})))).toList();
}
