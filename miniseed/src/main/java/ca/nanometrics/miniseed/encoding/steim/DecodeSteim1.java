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

import ca.nanometrics.miniseed.endian.EndianReader;

public class DecodeSteim1 extends DecodeSteim {
  public DecodeSteim1(EndianReader reader, int numOfSamples, int length) {
    super(reader, numOfSamples, length);
  }

  @Override
  protected String name() {
    return "STEIM1";
  }

  /** Read one short as the difference */
  @Override
  void readTypeTwoDifferences(final SamplesBuilder builder) {
    for (int i = 0; i < 2; i++) {
      int diff = checkForOverflow(getReader().readShort(), MAX_16_BIT_POSITIVE_NUMBER);
      builder.addToSamples(diff);
    }
  }

  /** Read one int as the difference */
  @Override
  protected void readTypeThreeDifferences(final SamplesBuilder builder) {
    int diff = checkForOverflow(getReader().readInt(), MAX_32_BIT_POSITIVE_NUMBER);
    builder.addToSamples(diff);
  }

  @Override
  protected int readFirstDifference(final SamplesBuilder builder) {
    int offset = getReader().getOffset();
    int wo = getReader().readInt();
    int c3 = wo >> 24 & 3;
    getReader().setOffset(offset + 12);
    int firstDiff = 0;
    switch (c3) {
      case FOUR_DIFFERENCES:
        int val = getReader().readByte();
        firstDiff = checkForOverflow(val, MAX_8_BIT_POSITIVE_NUMBER);
        break;
      case TYPE_TWO_DIFFERENCES:
        firstDiff = checkForOverflow(getReader().readShort(), MAX_16_BIT_POSITIVE_NUMBER);
        break;
      case TYPE_THREE_DIFFERENCES:
        firstDiff = checkForOverflow(getReader().readInt(), MAX_32_BIT_POSITIVE_NUMBER);
        break;
      default:
        throw new IllegalStateException("Invaild size c3 is: " + c3);
    }
    getReader().setOffset(offset);
    return firstDiff;
  }
}
