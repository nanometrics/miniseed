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

public final class LittleEndian implements Endian {
  private static final LittleEndian INSTANCE = new LittleEndian();

  public static LittleEndian get() {
    return INSTANCE;
  }

  private LittleEndian() {
    // singleton only
  }

  @Override
  public Endian.Type type() {
    return Endian.Type.LITTLE;
  }

  @Override
  public EndianReader createReader(byte[] bytes) {
    return new LittleEndianReader(bytes);
  }

  @Override
  public int readInt(byte[] buffer, int index) {
    return (buffer[index + 3] & 0xff) << 24
        | (buffer[index + 2] & 0xff) << 16
        | (buffer[index + 1] & 0xff) << 8
        | buffer[index] & 0xff;
  }

  @Override
  public long readUInt(byte[] buffer, int index) {
    return readInt(buffer, index) & 0xffffffffL;
  }

  @Override
  public void writeInt(byte[] buffer, int index, int value) {
    buffer[index + 3] = (byte) (value >> 24 & 0xff);
    buffer[index + 2] = (byte) (value >> 16 & 0xff);
    buffer[index + 1] = (byte) (value >> 8 & 0xff);
    buffer[index] = (byte) (value & 0xff);
  }

  @Override
  public short readShort(byte[] buffer, int index) {
    int value = (buffer[index + 1] & 0xff) << 8 | buffer[index] & 0xff;
    return (short) value;
  }

  @Override
  public short readUByte(byte[] buffer, int index) {
    return (short) (buffer[index] & 0xff);
  }

  @Override
  public int readUShort(byte[] buffer, int index) {
    return readShort(buffer, index) & 0xffff;
  }

  @Override
  public void writeShort(byte[] buffer, int index, short value) {
    buffer[index + 1] = (byte) (value >> 8 & 0xff);
    buffer[index] = (byte) (value & 0xff);
  }

  public long readLong(byte[] buffer, int index) {
    return (long) (buffer[index + 7] & 0xff) << 56
        | (long) (buffer[index + 6] & 0xff) << 48
        | (long) (buffer[index + 5] & 0xff) << 40
        | (long) (buffer[index + 4] & 0xff) << 32
        | (long) (buffer[index + 3] & 0xff) << 24
        | (long) (buffer[index + 2] & 0xff) << 16
        | (long) (buffer[index + 1] & 0xff) << 8
        | buffer[index] & 0xff;
  }

  public void writeLong(byte[] buffer, int index, long value) {
    buffer[index + 7] = (byte) (value >> 56 & 0xff);
    buffer[index + 6] = (byte) (value >> 48 & 0xff);
    buffer[index + 5] = (byte) (value >> 40 & 0xff);
    buffer[index + 4] = (byte) (value >> 32 & 0xff);
    buffer[index + 3] = (byte) (value >> 24 & 0xff);
    buffer[index + 2] = (byte) (value >> 16 & 0xff);
    buffer[index + 1] = (byte) (value >> 8 & 0xff);
    buffer[index] = (byte) (value & 0xff);
  }

  public int readInt24(byte[] buffer, int index) {
    // ensure proper sign extension
    return buffer[index + 2] << 16 | (buffer[index + 1] & 0xff) << 8 | buffer[index] & 0xff;
  }

  public int readUInt24(byte[] buffer, int index) {
    return readInt24(buffer, index) & 0x00ffffff;
  }

  public void writeInt24(byte[] buffer, int index, int value) {
    buffer[index + 2] = (byte) (value >> 16 & 0xff);
    buffer[index + 1] = (byte) (value >> 8 & 0xff);
    buffer[index] = (byte) (value & 0xff);
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
