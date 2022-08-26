package ca.nanometrics.miniseed.encoding.integers;

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
import ca.nanometrics.miniseed.encoding.Decode;
import ca.nanometrics.miniseed.endian.EndianReader;

public abstract class DecodeIntegers extends Decode {

  public DecodeIntegers(EndianReader reader, int numOfSamples, int length) {
    super(reader, numOfSamples, length);
  }

  @Override
  public Samples decode() {
    verifyHaveEnoughDataForNumberOfSamples();
    int[] samples = new int[expectedNumberOfSamples()];
    for (int i = 0; i < expectedNumberOfSamples(); i++) {
      samples[i] = readNextSample();
    }
    return Samples.build(samples);
  }

  protected abstract int readNextSample();

  protected abstract int bytesPerSample();

  protected void verifyHaveEnoughDataForNumberOfSamples() {
    int expectedNumberOfBytes = expectedNumberOfSamples() * bytesPerSample();
    if (getNumberOfBytesOfData() < expectedNumberOfBytes) {
      throw new IllegalStateException(
          String.format(
              "Not enough bytes of data for the number of samples. Expecting at least: %s for"
                  + " reading %s, but was %s",
              expectedNumberOfBytes, expectedNumberOfSamples(), getNumberOfBytesOfData()));
    }
  }
}
