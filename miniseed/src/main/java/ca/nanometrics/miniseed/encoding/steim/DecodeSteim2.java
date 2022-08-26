/** Copyright (c) 2001-2008 Nanometrics Inc. All rights reserved. */
package ca.nanometrics.miniseed.encoding.steim;

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

public class DecodeSteim2 extends DecodeSteim {
  private static final int NUMBER_OF_BITS = 32;
  private static final int NUMBER_OF_BITS_DATA = 30;
  private static final int NUMBER_OF_BITS_DNIB = 2;
  private static final int THIRTY_BITS = 30;
  private static final int FIFTEEN_BITS = 15;
  private static final int TEN_BITS = 10;
  private static final int SIX_BITS = 6;
  private static final int FIVE_BITS = 5;
  private static final int FOUR_BITS = 4;
  private static final int DNIB_MASK = 3;

  // for type two
  private static final int ONE_DIFFERENCE = 1;
  private static final int TWO_DIFFERENCES = 2;
  private static final int THREE_DIFFERENCES = 3;

  // for type three
  private static final int FIVE_DIFFERENCES = 0;
  private static final int SIX_DIFFERENCES = 1;
  private static final int SEVEN_DIFFERENCES = 2;

  /**
   * Constructor
   *
   * @param aReader A reader to decode the samples with
   * @param numOfSamples The number of samples there should be
   * @param length The record length
   */
  public DecodeSteim2(EndianReader aReader, int numOfSamples, int length) {
    super(aReader, numOfSamples, length);
  }

  @Override
  protected String name() {
    return "STEIM2";
  }

  /** The first two bits determine how many differences */
  @Override
  void readTypeTwoDifferences(final SamplesBuilder builder) {
    int value = getReader().readInt();
    int dnib = value >> NUMBER_OF_BITS_DATA & DNIB_MASK;
    int diff = value << NUMBER_OF_BITS_DNIB;
    switch (dnib) {
      case ONE_DIFFERENCE -> readBitDifferences(builder, diff, 1, THIRTY_BITS);
      case TWO_DIFFERENCES -> readBitDifferences(builder, diff, 2, FIFTEEN_BITS);
      case THREE_DIFFERENCES -> readBitDifferences(builder, diff, 3, TEN_BITS);
      default -> {
        /* do nothing */
      }
    }
  }

  /** The first two bits determine how many differences */
  @Override
  protected void readTypeThreeDifferences(final SamplesBuilder builder) {
    int value = getReader().readInt();
    int dnib = value >> NUMBER_OF_BITS_DATA & DNIB_MASK;
    int diff = value << NUMBER_OF_BITS_DNIB;
    switch (dnib) {
      case FIVE_DIFFERENCES -> readBitDifferences(builder, diff, 5, SIX_BITS);
      case SIX_DIFFERENCES -> readBitDifferences(builder, diff, 6, FIVE_BITS);
      case SEVEN_DIFFERENCES -> {
        diff = diff << 2; // First two bits after dnib aren't used
        readBitDifferences(builder, diff, 7, FOUR_BITS);
      }
      default -> {
        /* do nothing */
      }
    }
  }

  private void readBitDifferences(
      final SamplesBuilder builder, int value, int numOfDiffs, int sizeOfDiffs) {
    for (int i = 0; i < numOfDiffs; i++) {
      int diff = value >> NUMBER_OF_BITS - sizeOfDiffs;
      builder.addToSamples(diff);
      value = value << sizeOfDiffs;
    }
  }

  /** Not implemented for Steim2. */
  @Override
  protected int readFirstDifference(final SamplesBuilder builder) {
    return 0;
  }

  public static byte[] toBigEndian(Samples samples) {
    throw new UnsupportedOperationException(
        "Converting from little endian to big endian is not supported yet");
  }
}
