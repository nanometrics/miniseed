package ca.nanometrics.miniseed.v2.blockettes;

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

import ca.nanometrics.miniseed.endian.Endian;
import java.io.IOException;
import java.io.InputStream;

public record SampleRateBlockette_100(int nextBlocketteOffset, float actualSampleRate, byte flags)
    implements Blockette {

  public static final int TYPE = 100;
  public static final int LENGTH = 12;
  public static final int UNUSED_BYTE = 0x20;

  @Override
  public int blocketteType() {
    return TYPE;
  }

  @Override
  public int length() {
    return LENGTH;
  }

  @Override
  public byte[] toByteArray(Endian endian) {
    byte[] result = new byte[LENGTH];
    endian.writeShort(result, 0, (short) TYPE);
    endian.writeShort(result, 2, (short) nextBlocketteOffset);
    endian.writeFloat(result, 4, actualSampleRate);
    result[8] = flags;
    result[9] = UNUSED_BYTE; // reserved
    result[10] = UNUSED_BYTE; // reserved
    result[11] = UNUSED_BYTE; // reserved
    return result;
  }

  static class Loader implements Blockette.Loader {

    @Override
    public Blockette load(
        final int blocketteType,
        final int nextBlocketteOffset,
        final InputStream input,
        Endian endian)
        throws IOException {
      byte[] buffer = input.readNBytes(LENGTH - 4);
      float actualSampleRate = endian.readFloat(buffer, 0);
      byte flags = buffer[4];
      // bytes 9-11 (5-7 of buffer) are reserved
      return new SampleRateBlockette_100(nextBlocketteOffset, actualSampleRate, flags);
    }
  }
}
