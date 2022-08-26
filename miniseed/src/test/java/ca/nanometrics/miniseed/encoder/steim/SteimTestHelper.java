package ca.nanometrics.miniseed.encoder.steim;

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

import ca.nanometrics.miniseed.Sample;

public class SteimTestHelper {
  public static final long ARBITRARY_TIME = 123456789;
  public static final Sample INTEGER_MAX_VALUE = new Sample(Integer.MAX_VALUE);
  public static final Sample SHORT_MAX_VALUE = new Sample(Short.MAX_VALUE);
  public static final Sample BYTE_MAX_VALUE = new Sample(Byte.MAX_VALUE);
  public static final Sample ZERO_VALUE = new Sample(0);

  private SteimTestHelper() {
    // static method only
  }

  public static Sample[] getSamplesWithConstantDifferenceBytes(
      int numDifferences, int differenceBytes) {
    switch (differenceBytes) {
      case 1:
        return getSamplesWithOneByteDifferences(numDifferences);
      case 2:
        return getSamplesWithTwoByteDifferences(numDifferences);
      case 4:
        return getSamplesWithFourByteDifferences(numDifferences);
      default:
        throw new IllegalArgumentException(
            "Can't generate differences with " + differenceBytes + " bytes difference");
    }
  }

  public static Sample[] getSamplesWithFourByteDifferences(int numDifferences) {
    return getSamplesWithFourByteDifferences(numDifferences, ARBITRARY_TIME, 0);
  }

  public static Sample[] getSamplesWithFourByteDifferences(
      int numDifferences, long startTimeNanos, long periodNanos) {
    Sample[] samples = new Sample[numDifferences];
    for (int i = 0; i < numDifferences; i++) {
      samples[i] = (i & 1) == 0 ? new Sample(Integer.MAX_VALUE) : new Sample(0);
    }
    return samples;
  }

  public static Sample[] getSamplesWithTwoByteDifferences(int numDifferences) {
    Sample[] samples = new Sample[numDifferences];
    for (int i = 0; i < numDifferences; i++) {
      samples[i] = (i & 1) == 0 ? SHORT_MAX_VALUE : ZERO_VALUE;
    }
    return samples;
  }

  public static Sample[] getSamplesWithOneByteDifferences(int numDifferences) {
    Sample[] samples = new Sample[numDifferences];
    for (int i = 0; i < numDifferences; i++) {
      samples[i] = (i & 1) == 0 ? BYTE_MAX_VALUE : ZERO_VALUE;
    }
    return samples;
  }

  public static Sample getSampleWithOneByteDifferenceFrom(Sample sampleAtTime) {
    return getSampleWithDifferenceFrom(sampleAtTime, 1);
  }

  public static Sample getSampleWithFourByteDifferenceFrom(Sample sampleAtTime) {
    return getSampleWithDifferenceFrom(sampleAtTime, 4);
  }

  private static Sample getSampleWithDifferenceFrom(Sample sampleAtTime, int numBytesDifference) {
    int difference;
    if (numBytesDifference == 1) {
      difference = 1;
    } else if (numBytesDifference == 2) {
      difference = Byte.MAX_VALUE + 2;
    } else if (numBytesDifference == 4) {
      difference = Short.MAX_VALUE + 2;
    } else {
      throw new IllegalArgumentException();
    }
    int baseSample = sampleAtTime.sample();
    if (sampleWouldOverflowInteger(baseSample, difference)) {
      return new Sample(baseSample - difference);
    }

    if (sampleWouldUnderflowInteger(baseSample, difference)) {
      return new Sample(baseSample + difference);
    }

    return new Sample(baseSample + difference);
  }

  private static boolean sampleWouldOverflowInteger(int sample, int difference) {
    return Integer.MAX_VALUE - sample < difference;
  }

  private static boolean sampleWouldUnderflowInteger(int sample, int difference) {
    return Integer.MIN_VALUE + sample < difference;
  }
}
