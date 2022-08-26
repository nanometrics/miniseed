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

/** This provides a generic interface for reading from big-endian and little-endian byte arrays. */
public interface EndianReader {
  int BYTE_SIZE = 1;
  int SHORT_SIZE = 2;
  int INT_SIZE = 4;
  int INT24_SIZE = 3;
  int LONG_SIZE = 8;
  int FLOAT_SIZE = 4;
  int DOUBLE_SIZE = 8;

  /**
   * @return the read offset
   */
  int getOffset();

  /**
   * Set the current offset to read from.
   *
   * @param offset
   */
  void setOffset(int offset);

  /**
   * Reads <code>buffer.length</code> bytes into the byte array.
   *
   * @param buffer
   * @return the number of bytes read
   */
  int read(byte[] buffer);

  /**
   * Reads length bytes into the byte array at the specified offset.
   *
   * @param buffer
   * @param offset
   * @param length
   * @return the number of bytes read
   */
  int read(byte[] buffer, int offset, int length);

  /**
   * Reads an int from a byte array at the given offset.
   *
   * @return the read number
   */
  int readInt();

  /**
   * Reads an unsigned int from a byte array at the given offset.
   *
   * @return the read number
   */
  long readUInt();

  /**
   * Reads a byte from a byte array at the given offset.
   *
   * @return the read number
   */
  byte readByte();

  /**
   * Reads an unsigned byte from a byte array at the given offset.
   *
   * @return the read number
   */
  short readUByte();

  /**
   * Reads a short int from a byte array at the given offset.
   *
   * @return the read number
   */
  short readShort();

  /**
   * Reads an unsigned short int from a byte array at the given offset.
   *
   * @return the read number
   */
  int readUShort();

  /**
   * Reads a long from a byte array at the given offset.
   *
   * @return the read number
   */
  long readLong();

  /**
   * Reads a 24-bit int from a byte array at the given offset.
   *
   * @return the read number
   */
  int readInt24();

  /**
   * Reads a float from a byte array at the given offset.
   *
   * @return the read number
   */
  float readFloat();

  /**
   * Reads a double from a byte array at the given offset.
   *
   * @return the read number
   */
  double readDouble();
}
