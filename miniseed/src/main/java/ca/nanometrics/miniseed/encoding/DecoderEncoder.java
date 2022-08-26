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

public abstract class DecoderEncoder {

  protected static final int MAX_8_BIT_POSITIVE_NUMBER = 0x7f;
  protected static final int MAX_16_BIT_POSITIVE_NUMBER = 0x7fff;
  protected static final int MAX_32_BIT_POSITIVE_NUMBER = 0x7fffffff;
  protected static final int MAX_8_BIT_NEGATIVE_NUMBER = (MAX_8_BIT_POSITIVE_NUMBER + 1) * -1;
  protected static final int MAX_16_BIT_NEGATIVE_NUMBER = (MAX_16_BIT_POSITIVE_NUMBER + 1) * -1;
  protected static final int MAX_32_BIT_NEGATIVE_NUMBER = (MAX_32_BIT_POSITIVE_NUMBER + 1) * -1;

  private final int m_expectedNumberOfSamples;
  private final int m_recordLength;

  public DecoderEncoder(int expectedNumberOfSamples, int length) {
    m_expectedNumberOfSamples = expectedNumberOfSamples;
    m_recordLength = length;
  }

  public int expectedNumberOfSamples() {
    return m_expectedNumberOfSamples;
  }

  public int recordLength() {
    return m_recordLength;
  }
}
