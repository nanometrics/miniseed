package ca.nanometrics.miniseed.encoding;

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

public abstract class Decode extends DecoderEncoder {
  private final int m_initialSample = 0;
  private final int m_firstDiff = 0;
  private final EndianReader m_reader;

  public Decode(EndianReader reader, int numOfSamples, int length) {
    super(numOfSamples, length);
    m_reader = reader;
  }

  public abstract Samples decode();

  public boolean isDecoderSteim() {
    return false;
  }

  public EndianReader getReader() {
    return m_reader;
  }

  protected int getNumberOfBytesOfData() {
    return recordLength() - getReader().getOffset();
  }

  public int initialSample() {
    return m_initialSample;
  }

  int firstDiff() {
    return m_firstDiff;
  }
}
