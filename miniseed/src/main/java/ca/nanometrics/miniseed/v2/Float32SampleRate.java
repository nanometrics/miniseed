package ca.nanometrics.miniseed.v2;

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

import ca.nanometrics.miniseed.endian.BigEndian;

public record Float32SampleRate(float value) implements V2SampleRate {
  @Override
  public double sampleRateDouble() {
    return value;
  }

  @Override
  public int sampleRateInt() {
    throw new IllegalStateException("Sample rate cannot be represented as an integer: " + value);
  }

  @Override
  public byte[] encoding() {
    byte[] bytes = new byte[4];
    BigEndian.get().writeFloat(bytes, 0, value);
    return bytes;
  }

  @Override
  public boolean isFloatingPoint() {
    return true;
  }

  @Override
  public double samplePeriod() {
    return 1.0 / value;
  }

  @Override
  public int factor() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int multiplier() {
    throw new UnsupportedOperationException();
  }

  public static Float32SampleRate get(float sampleRate) {
    return new Float32SampleRate(sampleRate);
  }

  public static Float32SampleRate get(byte[] encoding) {
    return get(BigEndian.get().readFloat(encoding, 0));
  }
}
