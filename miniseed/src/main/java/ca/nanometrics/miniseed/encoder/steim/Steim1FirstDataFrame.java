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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;

public class Steim1FirstDataFrame extends AbstractSteim1DataFrame {
  private static final String ERROR_WHILE_GETTING_BYTE_ARRAY_FROM_FRAME =
      "Error while getting byte array from frame";
  private static final String CANNOT_GET_THE_BYTE_ARRAY_WITH_UNINITIALIZED_LAST_SAMPLE =
      "Cannot get the byte array for the first data frame without explicitly setting the last"
          + " sample of the block";
  public static final int NUM_DATA_WORDS = 13;
  static final int NUM_NON_DATA_WORDS = 3;

  private Sample m_steimBlockLastSample;

  public Steim1FirstDataFrame(
      String description, Steim1WordProvider wordProvider, Sample lastSample) {
    super(description, wordProvider, lastSample, NUM_DATA_WORDS, NUM_NON_DATA_WORDS);
  }

  public void setSteimBlockLastSample(Sample sampleAtTime) {
    m_steimBlockLastSample = sampleAtTime;
  }

  public Optional<Sample> getLastSampleOfSteimBlock() {
    return Optional.ofNullable(m_steimBlockLastSample);
  }

  public void setSteimBlockLastSampleAndFinish(Sample sampleAtTime) {
    setSteimBlockLastSample(sampleAtTime);
    super.finish();
  }

  @Override
  public byte[] getAsByteArray() {
    if (m_steimBlockLastSample == null) {
      throw new IllegalStateException(CANNOT_GET_THE_BYTE_ARRAY_WITH_UNINITIALIZED_LAST_SAMPLE);
    }

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(NUMBER_BYTES_PER_FRAME);
    try {
      outputStream.write(getBytesForControlCodes());
      outputStream.write(ByteBuffer.allocate(4).putInt(getFirstSample().sample()).array());
      outputStream.write(ByteBuffer.allocate(4).putInt(m_steimBlockLastSample.sample()).array());
      outputStream.write(getDataAsByteArray());
    } catch (IOException e) {
      throw new IllegalStateException(ERROR_WHILE_GETTING_BYTE_ARRAY_FROM_FRAME, e);
    }
    return outputStream.toByteArray();
  }

  @Override
  void blockFilled() {
    // Don't finish, because the first data frame needs the last value of the block to complete
  }
}
