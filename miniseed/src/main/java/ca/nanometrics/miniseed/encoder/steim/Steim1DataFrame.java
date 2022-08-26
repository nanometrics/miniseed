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

public class Steim1DataFrame extends AbstractSteim1DataFrame {

  public static final int NUM_DATA_WORDS = 15;
  private static final int NUM_NON_DATA_WORDS = 1;

  public Steim1DataFrame(String description, Steim1WordProvider wordProvider, Sample lastSample) {
    super(description, wordProvider, lastSample, NUM_DATA_WORDS, NUM_NON_DATA_WORDS);
  }

  @Override
  public byte[] getAsByteArray() {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(NUMBER_BYTES_PER_FRAME);
    try {
      outputStream.write(getBytesForControlCodes());
      outputStream.write(getDataAsByteArray());
    } catch (IOException e) {
      throw new RuntimeException("Error while getting byte array from Steim Frame", e);
    }
    return outputStream.toByteArray();
  }
}
