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

public record DataExtensionBlockette_1001(
    int nextBlocketteOffset, byte timingQuality, byte microsecond, byte frameCount)
    implements Blockette {

  public static final int TYPE = 1001;
  public static final int LENGTH = 8;

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
    result[4] = timingQuality;
    result[5] = microsecond;
    result[6] = 0; // reserved
    result[7] = frameCount;
    return result;
  }

  static class Loader implements Blockette.Loader {
    @Override
    public Blockette load(
        int blocketteType, int nextBlocketteOffset, InputStream input, Endian endian)
        throws IOException {
      byte[] buffer = input.readNBytes(4);
      byte timingQuality = buffer[0];
      if (timingQuality < 0 || timingQuality > 100)
        throw new IllegalStateException(
            "Timing quality must be between 0 and 100, but got " + timingQuality);
      byte microsecond = buffer[1];
      // buffer[2] is reserved (unused)
      byte frameCount = buffer[3];
      if (frameCount > 63)
        throw new IllegalStateException(
            "Frame count has a maximum value of 63, but got " + frameCount);
      return new DataExtensionBlockette_1001(
          nextBlocketteOffset, timingQuality, microsecond, frameCount);
    }
  }
}
