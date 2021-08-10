package dk.aivclab.demo.usecases.classification.utilities;

import java.util.Arrays;

public class Selection {

    public static int[] topK(float[] a, final int num) {
        float[] values = new float[num];
        Arrays.fill(values, -Float.MAX_VALUE);
        int[] indices = new int[num];
        Arrays.fill(indices, -1);

        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < num; j++) {
                if (a[i] > values[j]) {
                    for (int k = num - 1; k >= j + 1; k--) {
                        values[k] = values[k - 1];
                        indices[k] = indices[k - 1];
                    }
                    values[j] = a[i];
                    indices[j] = i;
                    break;
                }
            }
        }
        return indices;
    }

    public static float[] SoftMax(float[] params) {
        double sum = 0;

        for (int i = 0; i < params.length; i++) {
            params[i] = (float) Math.exp(params[i]);
            sum += params[i];
        }

        if (Double.isNaN(sum) || sum < 0) {
            Arrays.fill(params, (float) (1.0 / params.length));
        } else {
            for (int i = 0; i < params.length; i++) {
                params[i] = (float) (params[i] / sum);
            }
        }

        return params;
    }

    public ArgMaxTupleResult ArgMax(double[] params) {
        // ArgMax function to select the first higher value and its index from the array.
        int maxIndex = 0;
        for (int i = 0; i < params.length; i++) {
            if (params[maxIndex] < params[i]) {
                maxIndex = i;
            }
        }

        return new ArgMaxTupleResult(maxIndex, params[maxIndex]);
    }

    public class ArgMaxTupleResult {
        private final int index;
        private final double maxValue;

        ArgMaxTupleResult(int index, double maxValue) {
            this.index = index;
            this.maxValue = maxValue;
        }

        public int getIndex() {
            return index;
        }

        public double getMaxValue() {
            return maxValue;
        }
    }
}
