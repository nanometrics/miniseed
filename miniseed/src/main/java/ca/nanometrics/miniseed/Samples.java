package ca.nanometrics.miniseed;

/*-
 * #%L
 * miniseed
 * %%
 * Copyright (C) 2022 - 2023 Nanometrics Inc
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.stream.IntStream;

public interface Samples {
  enum Type {
    INTEGER,
    FLOAT,
    DOUBLE,
    TEXT,
    NONE
  }

  Type type();

  default boolean isInt() {
    return type() == Type.INTEGER;
  }

  int[] intSamples();

  float[] floatSamples();

  double[] doubleSamples();

  String text();

  static Samples build(int[] samples) {
    return new IntSamples(samples);
  }

  static Samples build(float[] samples) {
    return new FloatSamples(samples);
  }

  static Samples build(double[] samples) {
    return new DoubleSamples(samples);
  }

  static Samples build(String text) {
    return new Text(text);
  }

  record IntSamples(int[] intSamples) implements Samples {
    @Override
    public Type type() {
      return Type.INTEGER;
    }

    @Override
    public float[] floatSamples() {
      throw new IllegalStateException("Samples are integers");
    }

    @Override
    public double[] doubleSamples() {
      return IntStream.of(intSamples).mapToDouble(sample -> sample).toArray();
    }

    @Override
    public String text() {
      throw new IllegalStateException("Samples are not TEXT");
    }
  }

  record FloatSamples(float[] floatSamples) implements Samples {
    @Override
    public Type type() {
      return Type.FLOAT;
    }

    @Override
    public int[] intSamples() {
      throw new IllegalStateException("Samples are 32-bit floats (IEEE float)");
    }

    @Override
    public double[] doubleSamples() {
      throw new IllegalStateException("Samples are 32-bit floats (IEEE float)");
    }

    @Override
    public String text() {
      throw new IllegalStateException("Samples are not TEXT");
    }
  }

  record DoubleSamples(double[] doubleSamples) implements Samples {
    @Override
    public Type type() {
      return Type.DOUBLE;
    }

    @Override
    public int[] intSamples() {
      throw new IllegalStateException("Samples are 64-bit floats (IEEE double)");
    }

    @Override
    public float[] floatSamples() {
      throw new IllegalStateException("Samples are 64-bit floats (IEEE double)");
    }

    @Override
    public String text() {
      throw new IllegalStateException("Samples are not TEXT");
    }
  }

  record Text(String text) implements Samples {
    @Override
    public Type type() {
      return Type.TEXT;
    }

    @Override
    public int[] intSamples() {
      throw new IllegalStateException("Samples are TEXT");
    }

    @Override
    public float[] floatSamples() {
      throw new IllegalStateException("Samples are TEXT");
    }

    @Override
    public double[] doubleSamples() {
      throw new IllegalStateException("Samples are TEXT");
    }
  }
}
