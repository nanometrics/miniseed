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
import ca.nanometrics.miniseed.encoding.Decode;
import ca.nanometrics.miniseed.endian.EndianReader;

public abstract class DecodeSteim extends Decode {
  protected static final int NUMBER_ELEMENTS_PER_FRAME = 16;
  protected static final int NUMBER_BYTES_PER_FRAME = NUMBER_ELEMENTS_PER_FRAME * 4;

  protected static final int BITS_PER_DECOMPRESS_FLAG = 2;
  protected static final int MASK_FOR_BOTTOM_2_BITS = 0x00000003;
  protected static final int NO_DIFFERENCES = 0;
  protected static final int FOUR_DIFFERENCES = 1;
  // For Steim1 this is two differences, for Steim2 1, 2 or 3 differences
  protected static final int TYPE_TWO_DIFFERENCES = 2;
  // For Steim1 this is one difference, for Steim2 5, 6 or 7 differences
  protected static final int TYPE_THREE_DIFFERENCES = 3;

  public DecodeSteim(EndianReader reader, int numOfSamples, int length) {
    super(reader, numOfSamples, length);
  }

  protected abstract String name();

  abstract void readTypeTwoDifferences(final SamplesBuilder builder);

  protected abstract void readTypeThreeDifferences(final SamplesBuilder builder);

  protected abstract int readFirstDifference(final SamplesBuilder builder);

  @Override
  public Samples decode() {
    SamplesBuilder builder = new SamplesBuilder(name(), expectedNumberOfSamples());
    readFirstAndFinal(builder);
    readFirstDifference(builder);
    int numberOfFrames = getNumberOfFrames();
    for (int i = 0; i < numberOfFrames; i++) {
      decodeFrame(builder);
    }
    return builder.build();
  }

  @Override
  public boolean isDecoderSteim() {
    return true;
  }

  /**
   * Check to see if the number is really negative
   *
   * @param number The number to check for overflow
   * @param maxValue The max intValue this int can be
   * @return The number after checking for overflow
   */
  protected int checkForOverflow(int number, int maxValue) {
    if (number > maxValue) {
      number -= 2 * (maxValue + 1);
    }
    return number;
  }

  private int getNumberOfFrames() {
    return getNumberOfBytesOfData() / NUMBER_BYTES_PER_FRAME;
  }

  private void decodeFrame(final SamplesBuilder builder) {
    int[] decompressionFlags = getDecompressionFlags();
    for (int i = 1; i < NUMBER_ELEMENTS_PER_FRAME; i++) {
      int decompressionFlag = decompressionFlags[i];
      switch (decompressionFlag) {
        case FOUR_DIFFERENCES -> readByteSizeDifferences(builder);
        case TYPE_TWO_DIFFERENCES -> readTypeTwoDifferences(builder);
        case TYPE_THREE_DIFFERENCES -> readTypeThreeDifferences(builder);
        case NO_DIFFERENCES -> getReader().readInt();
        default -> throw new IllegalStateException(
            "Unexpected decompression flag: " + decompressionFlag);
      }
    }
  }

  protected void readByteSizeDifferences(final SamplesBuilder builder) {
    for (int i = 0; i < 4; i++) {
      int diff = checkForOverflow(getReader().readByte(), MAX_8_BIT_POSITIVE_NUMBER);
      builder.addToSamples(diff);
    }
  }

  private int[] getDecompressionFlags() {
    int allDecompressionFlags = getReader().readInt();
    int[] decompressionFlags = new int[NUMBER_ELEMENTS_PER_FRAME];
    for (int i = NUMBER_ELEMENTS_PER_FRAME - 1; i >= 0; i--) {
      decompressionFlags[i] = allDecompressionFlags & MASK_FOR_BOTTOM_2_BITS;
      allDecompressionFlags >>= BITS_PER_DECOMPRESS_FLAG;
    }
    return decompressionFlags;
  }

  private void readFirstAndFinal(SamplesBuilder builder) {
    int offset = getReader().getOffset();
    getReader().setOffset(offset + 4);
    builder.setInitialSample(getReader().readInt());
    builder.setFinalSample(getReader().readInt());
    getReader().setOffset(offset);
  }

  static class SamplesBuilder {
    private final String m_name;
    private final int[] m_intSamples;
    private int m_numberOfProcessedSamples;
    private int m_initialSample;
    private int m_finalSample;
    private int m_sampleXminus1;
    private int m_lastSample;

    SamplesBuilder(final String name, int expectedNumberOfSamples) {
      m_name = name;
      m_intSamples = new int[expectedNumberOfSamples];
    }

    public Samples build() {
      verify();
      return Samples.build(m_intSamples);
    }

    public int getInitialSample() {
      return m_initialSample;
    }

    public void setInitialSample(int initialSample) {
      m_initialSample = initialSample;
    }

    public int getFinalSample() {
      return m_finalSample;
    }

    public void setFinalSample(final int finalSample) {
      m_finalSample = finalSample;
    }

    protected boolean isFirstSample() {
      return m_numberOfProcessedSamples == 0;
    }

    public int getSampleXminus1() {
      return m_sampleXminus1;
    }

    public void addToSamples(int diff) {
      if (isFirstSample()) {
        m_sampleXminus1 = getInitialSample() - diff;
        m_lastSample = getSampleXminus1();
      }

      if (m_numberOfProcessedSamples < m_intSamples.length) {
        m_lastSample = diff + m_lastSample;
        m_intSamples[m_numberOfProcessedSamples] = m_lastSample;
        m_numberOfProcessedSamples++;
      }
    }

    protected void verify() {
      if (m_numberOfProcessedSamples != m_intSamples.length) {
        throw new IllegalStateException(
            String.format(
                "While decoding %s, the number of samples decoded was %s, but the number of samples"
                    + " expected was %s",
                m_name, m_numberOfProcessedSamples, m_intSamples.length));
      }
      if (m_finalSample != m_intSamples[m_numberOfProcessedSamples - 1]) {
        throw new IllegalStateException(
            String.format(
                "While decoding %s, the last sample decoded was %s, but the last sample expected"
                    + " was: ",
                m_name, m_intSamples[m_numberOfProcessedSamples - 1], getFinalSample()));
      }
    }
  }
}
