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
import ca.nanometrics.miniseed.Samples.Type;
import ca.nanometrics.miniseed.encoding.floats.Decode32BitFloats;
import ca.nanometrics.miniseed.encoding.floats.Decode64BitDoubles;
import ca.nanometrics.miniseed.encoding.integers.Decode16BitIntegers;
import ca.nanometrics.miniseed.encoding.integers.Decode32BitIntegers;
import ca.nanometrics.miniseed.encoding.steim.DecodeSteim1;
import ca.nanometrics.miniseed.encoding.steim.DecodeSteim2;
import ca.nanometrics.miniseed.endian.EndianReader;
import ca.nanometrics.miniseed.endian.LittleEndianReader;
import javax.annotation.Nullable;

/**
 * As defined in the MiniSEED format specification.
 * https://miniseed3.readthedocs.io/en/latest/data-encodings.html
 */
public enum DataEncoding {
  TEXT(0, true, Samples.Type.TEXT),
  INTEGER_16BIT(1, true, Samples.Type.INTEGER),
  INTEGER_24BIT(2, false, Samples.Type.INTEGER),
  INTEGER_32BIT(3, true, Samples.Type.INTEGER),
  FLOAT_32BIT(4, true, Samples.Type.DOUBLE),
  FLOAT_64BIT(5, true, Samples.Type.DOUBLE),
  STEIM1(10, true, Samples.Type.INTEGER),
  STEIM2(11, true, Samples.Type.INTEGER),
  GEOSCOPE_MULTIPLEXED_24BIT_INTEGER(12, false, Samples.Type.INTEGER),
  GEOSCOPE_MULTIPLEXED_16BIT_GAIN_RANGED_3BITEXPONENT(13, false, Samples.Type.INTEGER),
  GEOSCOPE_MULTIPLEXED_16BIT_GAIN_RANGED_4BITEXPONENT(14, false, Samples.Type.INTEGER),
  US_NATIONAL_NETWORK(15, false, Samples.Type.INTEGER),
  CDSN_16BIT_GAIN_RANGED(16, false, Samples.Type.INTEGER),
  STEIM3(19, true, Samples.Type.INTEGER),
  SRO(30, false, Samples.Type.INTEGER),
  HGLP(31, false, Samples.Type.INTEGER),
  DWWSSN_GAIN_RANGED(32, false, Samples.Type.INTEGER),
  RTSN_16BIT_GAIN_RANGED(33, false, Samples.Type.INTEGER),
  OPAQUE(100, true, Samples.Type.NONE);

  private static DecoderFactory unsupported(DataEncoding encoding) {
    return (endianFactory, bytes, numSamples, length) -> {
      throw new UnsupportedOperationException("Unsupported data encoding: " + encoding);
    };
  }

  private final byte m_code;
  private final boolean m_miniseed3Support;
  private final Type m_sampleType;

  DataEncoding(int code, boolean miniseed3Support, Samples.Type sampleType) {
    m_code = (byte) code;
    m_miniseed3Support = miniseed3Support;
    m_sampleType = sampleType;
  }

  public byte code() {
    return m_code;
  }

  public boolean miniseed3Support() {
    return m_miniseed3Support;
  }

  public Type sampleType() {
    return m_sampleType;
  }

  @Nullable
  public Decode decoder(
      EndianReaderFactory readerFactory, byte[] bytes, int numSamples, int length) {
    DecoderFactory decoderFactory = factory(this);
    return decoderFactory == null
        ? null
        : decoderFactory.create(readerFactory, bytes, numSamples, length);
  }

  public static DataEncoding fromCode(int code) {
    for (DataEncoding encoding : DataEncoding.values()) {
      if (encoding.m_code == code) {
        return encoding;
      }
    }
    throw new IllegalArgumentException("Unknown data encoding code: " + code);
  }

  static DecoderFactory factory(DataEncoding encoding) {
    return switch (encoding) {
      case TEXT -> (endianFactory, bytes, numSamples, length) ->
          new DecodeText(endianFactory.apply(encoding, bytes), numSamples, length);
      case INTEGER_16BIT -> (endianFactory, bytes, numSamples, length) ->
          new Decode16BitIntegers(new LittleEndianReader(bytes), numSamples, length);
      case INTEGER_24BIT -> unsupported(encoding);
      case INTEGER_32BIT -> (endianFactory, bytes, numSamples, length) ->
          new Decode32BitIntegers(new LittleEndianReader(bytes), numSamples, length);
      case FLOAT_32BIT -> (endianFactory, bytes, numSamples, length) ->
          new Decode32BitFloats(new LittleEndianReader(bytes), numSamples, length);
      case FLOAT_64BIT -> (endianFactory, bytes, numSamples, length) ->
          new Decode64BitDoubles(new LittleEndianReader(bytes), numSamples, length);
      case STEIM1 -> (endianFactory, bytes, numSamples, length) ->
          new DecodeSteim1(endianFactory.apply(encoding, bytes), numSamples, length);
      case STEIM2 -> (endianFactory, bytes, numSamples, length) ->
          new DecodeSteim2(endianFactory.apply(encoding, bytes), numSamples, length);
      case GEOSCOPE_MULTIPLEXED_24BIT_INTEGER -> unsupported(encoding);
      case GEOSCOPE_MULTIPLEXED_16BIT_GAIN_RANGED_3BITEXPONENT -> unsupported(encoding);
      case GEOSCOPE_MULTIPLEXED_16BIT_GAIN_RANGED_4BITEXPONENT -> unsupported(encoding);
      case US_NATIONAL_NETWORK -> unsupported(encoding);
      case CDSN_16BIT_GAIN_RANGED -> unsupported(encoding);
      case STEIM3 -> unsupported(encoding);
      case SRO -> unsupported(encoding);
      case HGLP -> unsupported(encoding);
      case DWWSSN_GAIN_RANGED -> unsupported(encoding);
      case RTSN_16BIT_GAIN_RANGED -> unsupported(encoding);
      case OPAQUE -> unsupported(encoding);
    };
  }

  @FunctionalInterface
  interface DecoderFactory {
    Decode create(EndianReaderFactory endianFactory, byte[] bytes, int numSamples, int length);
  }

  @FunctionalInterface
  public interface EndianReaderFactory {
    EndianReader apply(DataEncoding encoding, byte[] bytes);
  }
}
