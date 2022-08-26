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

import ca.nanometrics.miniseed.encoding.DataEncoding;
import ca.nanometrics.miniseed.endian.Endian;
import ca.nanometrics.miniseed.endian.WordOrder;
import java.io.IOException;
import java.io.InputStream;

public record DataOnlyBlockette_1000(
    int nextBlocketteOffset, DataEncoding encodingFormat, WordOrder wordOrder, int dataRecordLength)
    implements Blockette {

  public static final int TYPE = 1000;
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
    result[4] = encodingFormat.code();
    result[5] = (byte) wordOrder.code();
    result[6] = recordLengthAsPowerOfTwo();
    result[7] = 0; // reserved
    return result;
  }

  private byte recordLengthAsPowerOfTwo() {
    int length = dataRecordLength;
    byte powerOfTwo = 0;
    while (length != 1) {
      length = length / 2;
      powerOfTwo++;
    }
    return powerOfTwo;
  }

  static class Loader implements Blockette.Loader {
    @Override
    public Blockette load(
        int blocketteType, int nextBlocketteOffset, InputStream input, Endian endian)
        throws IOException {
      byte[] buffer = input.readNBytes(4);
      return new DataOnlyBlockette_1000(
          nextBlocketteOffset,
          DataEncoding.fromCode(buffer[0]),
          WordOrder.fromCode(buffer[1]),
          (int) Math.pow(2.0, buffer[2]));
    }
  }
}
