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
import java.nio.charset.StandardCharsets;

public class DecodeText extends Decode {

  public DecodeText(EndianReader reader, int numSamples, int length) {
    super(reader, numSamples, length);
  }

  @Override
  public Samples decode() {
    byte[] bytes = new byte[recordLength()];
    getReader().read(bytes);
    String text = new String(bytes, StandardCharsets.US_ASCII);
    return Samples.build(text);
  }
}
