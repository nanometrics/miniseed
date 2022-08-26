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

public class BigEndianReader implements EndianReader {
  /** The array from which to read. */
  private final byte[] m_buffer;
  /** The current read offset in the byte array. */
  private int m_offset;

  /** Construct a reader for the given byte array. */
  public BigEndianReader(byte[] aBuffer, int anOffset) {
    m_buffer = aBuffer;
    m_offset = anOffset;
  }

  public BigEndianReader(byte[] aBuffer) {
    this(aBuffer, 0);
  }

  /** Set the read offset to a new intValue. */
  @Override
  public void setOffset(int newOffset) {
    m_offset = newOffset;
  }

  /**
   * Get the read offset
   *
   * @return the read offset
   * @see EndianReader#getOffset
   */
  @Override
  public int getOffset() {
    return m_offset;
  }

  /**
   * Read a byte array from the buffer.
   *
   * @see EndianReader#read(byte[], int, int)
   */
  @Override
  public int read(byte[] barray, int start, int bytesToRead) {
    System.arraycopy(m_buffer, start, barray, 0, bytesToRead);
    m_offset += bytesToRead;
    return bytesToRead;
  }

  /**
   * Read a byte array from the buffer.
   *
   * @see EndianReader#read(byte[])
   */
  @Override
  public int read(byte[] buffer) {
    return read(buffer, 0, buffer.length);
  }

  /**
   * Reads an int from a byte array at the given offset. *
   *
   * @see EndianReader#readInt()
   */
  @Override
  public int readInt() {
    int val = BigEndian.get().readInt(m_buffer, m_offset);
    m_offset += INT_SIZE;
    return val;
  }

  /**
   * Reads an unsigned int from a byte array at the given offset.
   *
   * @see EndianReader#readUInt()
   */
  @Override
  public long readUInt() {
    long val = BigEndian.get().readUInt(m_buffer, m_offset);
    m_offset += INT_SIZE;
    return val;
  }

  /**
   * Reads a byte from a byte array at the given offset.
   *
   * @see EndianReader#readByte()
   */
  @Override
  public byte readByte() {
    byte val = m_buffer[m_offset];
    m_offset += BYTE_SIZE;
    return val;
  }

  /**
   * Reads an unsigned byte from a byte array at the given offset.
   *
   * @see EndianReader#readUByte()
   */
  @Override
  public short readUByte() {
    return (short) (readByte() & 0xff);
  }

  /**
   * Reads a short int from a byte array at the given offset.
   *
   * @see EndianReader#readShort()
   */
  @Override
  public short readShort() {
    short val = BigEndian.get().readShort(m_buffer, m_offset);
    m_offset += SHORT_SIZE;
    return val;
  }

  /**
   * Reads an Unsigned short int from a byte array at the given offset.
   *
   * @see EndianReader#readUShort()
   */
  @Override
  public int readUShort() {
    int val = BigEndian.get().readUShort(m_buffer, m_offset);
    m_offset += SHORT_SIZE;
    return val;
  }

  /**
   * Reads a long from a byte array at the given offset.
   *
   * @see EndianReader#readLong()
   */
  @Override
  public long readLong() {
    long val = BigEndian.get().readLong(m_buffer, m_offset);
    m_offset += LONG_SIZE;
    return val;
  }

  /**
   * Reads a 24-bit int from a byte array at the given offset.
   *
   * @see EndianReader#readInt24()
   */
  @Override
  public int readInt24() {
    int val = BigEndian.get().readInt24(m_buffer, m_offset);
    m_offset += INT24_SIZE;
    return val;
  }

  /**
   * Reads a float from a byte array at the given offset.
   *
   * @see EndianReader#readFloat()
   */
  @Override
  public float readFloat() {
    float val = BigEndian.get().readFloat(m_buffer, m_offset);
    m_offset += FLOAT_SIZE;
    return val;
  }

  /**
   * Reads a double from a byte array at the given offset.
   *
   * @see EndianReader#readDouble()
   */
  @Override
  public double readDouble() {
    double val = BigEndian.get().readDouble(m_buffer, m_offset);
    m_offset += DOUBLE_SIZE;
    return val;
  }
}
