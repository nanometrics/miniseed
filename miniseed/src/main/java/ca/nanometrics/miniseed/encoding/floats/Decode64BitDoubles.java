package ca.nanometrics.miniseed.encoding.floats;

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

import ca.nanometrics.miniseed.Samples;
import ca.nanometrics.miniseed.endian.EndianReader;

public class Decode64BitDoubles extends DecodeFloatingPoint {
  private static final int NUMBER_OF_BYTES_PER_SAMPLE = 8;

  public Decode64BitDoubles(EndianReader reader, int numOfSamples, int length) {
    super(reader, numOfSamples, length);
  }

  @Override
  public Samples decode() {
    verifyHaveEnoughDataForNumberOfSamples();
    double[] samples = new double[expectedNumberOfSamples()];
    for (int i = 0; i < expectedNumberOfSamples(); i++) {
      samples[i] = getReader().readDouble();
    }
    return Samples.build(samples);
  }

  @Override
  protected int bytesPerSample() {
    return NUMBER_OF_BYTES_PER_SAMPLE;
  }
}
