package ca.nanometrics.miniseed.endian;

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

import java.nio.charset.StandardCharsets;

public final class BigEndian implements Endian {
  public static final int MASK_UBYTE = 0x00FF;
  public static final int MASK_USHORT = 0xFFFF;
  public static final int MASK_UINT24 = 0x00FFFFFF;
  public static final long MASK_UINT = 0xFFFFFFFFL;

  public static final int MASK_BYTE = 0xFF;
  public static final int MASK_HIGH_NIBBLE = 0x00F0;
  public static final int MASK_LOW_NIBBLE = 0x000F;

  private static final BigEndian INSTANCE = new BigEndian();

  public static BigEndian get() {
    return INSTANCE;
  }

  private BigEndian() {
    // singleton only
  }

  @Override
  public Endian.Type type() {
    return Endian.Type.BIG;
  }

  @Override
  public EndianReader createReader(byte[] bytes) {
    return new BigEndianReader(bytes);
  }

  @Override
  public int readInt(byte[] buffer, int index) {
    return (buffer[index] & MASK_BYTE) << 24
        | (buffer[index + 1] & MASK_BYTE) << 16
        | (buffer[index + 2] & MASK_BYTE) << 8
        | buffer[index + 3] & MASK_BYTE;
  }

  @Override
  public long readUInt(byte[] buffer, int index) {
    return readInt(buffer, index) & MASK_UINT;
  }

  @Override
  public void writeInt(byte[] buffer, int index, int value) {
    buffer[index] = (byte) (value >> 24 & MASK_BYTE);
    buffer[index + 1] = (byte) (value >> 16 & MASK_BYTE);
    buffer[index + 2] = (byte) (value >> 8 & MASK_BYTE);
    buffer[index + 3] = (byte) (value & MASK_BYTE);
  }

  @Override
  public short readUByte(byte[] array, int index) {
    return (short) (array[index] & MASK_UBYTE);
  }

  public byte readULowNibble(byte[] array, int index) {
    return (byte) (array[index] & MASK_LOW_NIBBLE);
  }

  public byte readUHighNibble(byte[] array, int index) {
    return (byte) ((array[index] >> 4) & MASK_LOW_NIBBLE);
  }

  /**
   * Read a 12-bit integer from the lower half the byte at index and the full byte at index + 1. (eg
   * 0x0FFF)
   */
  public int readLowUInt12(byte[] buffer, int index) {
    return (buffer[index] & MASK_LOW_NIBBLE) << 8 | buffer[index + 1] & MASK_BYTE;
  }

  /**
   * Read a 12-bit integer from the byte at index and the upper half of the byte at index + 1. (eg
   * 0xFFF0)
   */
  public int readHighUInt12(byte[] buffer, int index) {
    return (buffer[index] & MASK_BYTE) << 4 | (buffer[index + 1] & MASK_HIGH_NIBBLE) >> 4;
  }

  @Override
  public short readShort(byte[] buffer, int index) {
    int value = (buffer[index] & MASK_BYTE) << 8 | buffer[index + 1] & MASK_BYTE;
    return (short) value;
  }

  @Override
  public int readUShort(byte[] buffer, int index) {
    return readShort(buffer, index) & MASK_USHORT;
  }

  @Override
  public void writeShort(byte[] buffer, int index, short value) {
    buffer[index] = (byte) (value >> 8 & MASK_BYTE);
    buffer[index + 1] = (byte) (value & MASK_BYTE);
  }

  public long readLong(byte[] buffer, int index) {
    return (long) (buffer[index] & MASK_BYTE) << 56
        | (long) (buffer[index + 1] & MASK_BYTE) << 48
        | (long) (buffer[index + 2] & MASK_BYTE) << 40
        | (long) (buffer[index + 3] & MASK_BYTE) << 32
        | (long) (buffer[index + 4] & MASK_BYTE) << 24
        | (long) (buffer[index + 5] & MASK_BYTE) << 16
        | (long) (buffer[index + 6] & MASK_BYTE) << 8
        | buffer[index + 7] & MASK_BYTE;
  }

  public void writeLong(byte[] buffer, int index, long value) {
    buffer[index] = (byte) (value >> 56 & MASK_BYTE);
    buffer[index + 1] = (byte) (value >> 48 & MASK_BYTE);
    buffer[index + 2] = (byte) (value >> 40 & MASK_BYTE);
    buffer[index + 3] = (byte) (value >> 32 & MASK_BYTE);
    buffer[index + 4] = (byte) (value >> 24 & MASK_BYTE);
    buffer[index + 5] = (byte) (value >> 16 & MASK_BYTE);
    buffer[index + 6] = (byte) (value >> 8 & MASK_BYTE);
    buffer[index + 7] = (byte) (value & MASK_BYTE);
  }

  public int readInt24(byte[] buffer, int index) {
    // ensure proper sign extension
    return buffer[index] << 16
        | (buffer[index + 1] & MASK_BYTE) << 8
        | buffer[index + 2] & MASK_BYTE;
  }

  public int readUInt24(byte[] buffer, int index) {
    return readInt24(buffer, index) & MASK_UINT24;
  }

  public void writeInt24(byte[] buffer, int index, int value) {
    buffer[index] = (byte) (value >> 16 & MASK_BYTE);
    buffer[index + 1] = (byte) (value >> 8 & MASK_BYTE);
    buffer[index + 2] = (byte) (value & MASK_BYTE);
  }

  public long readPartOfLong(byte[] buffer, int index, int numBytes) {
    if (numBytes < 1 || numBytes > 8) {
      throw new IllegalArgumentException();
    }

    // allow sign extension:
    long value = (long) buffer[index] << (numBytes - 1) * 8;

    for (int i = 1; i < numBytes; ++i) {
      value |= ((long) buffer[index + i] & MASK_BYTE) << (numBytes - i - 1) * 8;
    }

    return value;
  }

  public long readUPartOfLong(byte[] buffer, int index, int numBytes) {
    if (numBytes < 1 || numBytes > 8) {
      throw new IllegalArgumentException();
    }

    long value = 0;
    for (int i = 0; i < numBytes; ++i) {
      value |= ((long) buffer[index + i] & MASK_BYTE) << (numBytes - i - 1) * 8;
    }

    return value;
  }

  public void writePartOfLong(byte[] buffer, int index, long value, int numBytes) {
    if (numBytes < 1 || numBytes > 8) {
      throw new IllegalArgumentException();
    }

    for (int i = 0; i < numBytes; ++i) {
      buffer[index + i] = (byte) (value >> (numBytes - i - 1) * 8 & MASK_BYTE);
    }
  }

  @Override
  public float readFloat(byte[] buffer, int index) {
    int bits = readInt(buffer, index);
    return Float.intBitsToFloat(bits);
  }

  @Override
  public void writeFloat(byte[] buffer, int index, float num) {
    int bits = Float.floatToIntBits(num);
    writeInt(buffer, index, bits);
  }

  @Override
  public double readDouble(byte[] buffer, int index) {
    long bits = readLong(buffer, index);
    return Double.longBitsToDouble(bits);
  }

  @Override
  public void writeDouble(byte[] buffer, int index, double num) {
    long bits = Double.doubleToLongBits(num);
    writeLong(buffer, index, bits);
  }

  public String readString(byte[] buffer, int index, int fieldLength) {
    int length;
    for (length = 0; length < fieldLength; length++) {
      if (buffer[index + length] == 0) {
        break;
      }
    }

    if (length > 0) {
      return new String(buffer, index, length, StandardCharsets.UTF_8);
    }
    return "";
  }

  public void writeString(byte[] buffer, int index, String string, int len) {
    // write the name as bytes, zero terminated
    byte[] data = string.getBytes(StandardCharsets.UTF_8);
    int bytesToCopy = Math.min(data.length, len - 1);
    if (bytesToCopy > 0) {
      System.arraycopy(data, 0, buffer, index, bytesToCopy);
    }
    buffer[index + bytesToCopy] = 0;
  }
}
