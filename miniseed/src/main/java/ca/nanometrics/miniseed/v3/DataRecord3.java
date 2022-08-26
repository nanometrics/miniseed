/** Copyright (c) 2001-2008 Nanometrics Inc. All rights reserved. */
package ca.nanometrics.miniseed.v3;

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

import ca.nanometrics.miniseed.DataRecord;
import ca.nanometrics.miniseed.Samples;
import ca.nanometrics.miniseed.encoding.DataEncoding;
import ca.nanometrics.miniseed.encoding.Decode;
import ca.nanometrics.miniseed.endian.BigEndianReader;
import ca.nanometrics.miniseed.endian.LittleEndianReader;
import com.google.auto.value.AutoBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32C;
import org.json.JSONArray;
import org.json.JSONObject;

public class DataRecord3 implements DataRecord {
  private final DataRecord3Header m_header;
  private final byte[] m_payload;

  DataRecord3(DataRecord3Header header, byte[] payload) {
    m_header = header;
    m_payload = payload;
  }

  public static boolean isMiniSeed3(InputStream input) throws IOException {
    input.mark(3);
    byte[] buffer = new byte[3];
    try {
      return input.read(buffer) == 3 && isMiniSeed3(buffer);
    } finally {
      input.reset();
    }
  }

  public static boolean isMiniSeed3(byte[] buffer) {
    return buffer.length >= 3 && buffer[0] == 'M' && buffer[1] == 'S' && buffer[2] == 3;
  }

  @Override
  public DataRecord3Header header() {
    return m_header;
  }

  @Override
  public byte[] payload() {
    return m_payload;
  }

  public static DataRecord3 read(InputStream input) throws IOException {
    return builder().read(input);
  }

  @Override
  public JSONObject toJson() {
    JSONObject json = header().toJson();
    switch (header().dataPayloadEncoding()) {
      case TEXT:
        if (m_payload.length > 0) {
          json.put("Data", new String(m_payload, StandardCharsets.UTF_8));
        }
        break;
      case OPAQUE:
        break;
      default:
        Decode decoder = getDecoder();
        if (decoder != null) {
          json.put("Data", toJson(decoder));
        }
        break;
        // do nothing
    }
    return json;
  }

  private Object toJson(Decode decoder) {
    Samples samples = decoder.decode();
    return switch (samples.type()) {
      case INTEGER -> new JSONArray(samples.intSamples());
      case FLOAT -> new JSONArray(samples.floatSamples());
      case DOUBLE -> new JSONArray(samples.doubleSamples());
      case TEXT -> samples.text();
      case NONE -> null;
    };
  }

  @Override
  public byte[] toByteArray() {
    byte[] headerBytes = m_header.toByteArray();
    try (ByteArrayOutputStream outputStream =
        new ByteArrayOutputStream(headerBytes.length + m_payload.length)) {
      outputStream.write(headerBytes);
      outputStream.write(m_payload);
      return outputStream.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public int length() {
    return m_header.length() + m_payload.length;
  }

  @Override
  public Samples samples() {
    return getDecoder().decode();
  }

  private Decode getDecoder() {
    return header()
        .dataPayloadEncoding()
        .decoder(
            (DataEncoding encoding, byte[] bytes) ->
                switch (encoding) {
                  case STEIM1, STEIM2, STEIM3 -> new BigEndianReader(bytes);
                  default -> new LittleEndianReader(bytes);
                },
            m_payload,
            (int) header().numberOfSamples(),
            (int) header().lengthOfDataPayload());
  }

  public void write(OutputStream output) throws IOException {
    m_header.write(output);
    output.write(m_payload);
  }

  public static Builder builder() {
    return new AutoBuilder_DataRecord3_Builder();
  }

  @AutoBuilder
  public abstract static class Builder {
    public DataRecord3 read(InputStream input) throws IOException {
      DataRecord3Header header = DataRecord3Header.builder().read(input).build();
      header(header);
      payload(input.readNBytes((int) header.lengthOfDataPayload()));
      return build();
    }

    public abstract Builder header(DataRecord3Header header);

    abstract DataRecord3Header header();

    public abstract Builder payload(byte[] payload);

    abstract byte[] payload();

    abstract DataRecord3 autoBuild();

    public DataRecord3 build() {
      if (header().crc() == 0) {
        CRC32C crc = new CRC32C();
        header().updateCrc32C(crc).update(payload());
        header(header().toBuilder().crc(crc.getValue()).build());
      } else {
        validateCrc(header(), payload());
      }
      return autoBuild();
    }

    private static void validateCrc(DataRecord3Header header, byte[] payload) {
      DataRecord3Header zeroCrcHeader = header.toBuilder().crc(0).build();
      CRC32C crc = new CRC32C();
      crc.update(zeroCrcHeader.toByteArray());
      crc.update(payload);
      long crcValue = crc.getValue();
      if (crcValue != header.crc()) {
        throw new IllegalArgumentException(
            "CRC mismatch: expected " + header.crc() + ", but got " + crcValue);
      }
    }
  }
}
