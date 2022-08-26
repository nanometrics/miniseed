package ca.nanometrics.miniseed.v2;

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
import ca.nanometrics.miniseed.DataRecordHeader;
import ca.nanometrics.miniseed.Samples;
import ca.nanometrics.miniseed.encoding.Decode;
import ca.nanometrics.miniseed.v2.blockettes.DataOnlyBlockette_1000;
import com.google.auto.value.AutoBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.json.JSONObject;

public class DataRecord2 implements DataRecord {
  private final DataRecord2Header m_header;
  private final byte[] m_payload;

  DataRecord2(DataRecord2Header header, byte[] payload) {
    m_header = header;
    m_payload = payload;
  }

  public static DataRecord2 read(final InputStream input) throws IOException {
    return builder().read(input);
  }

  @Override
  public DataRecord2Header header() {
    return m_header;
  }

  @Override
  public byte[] payload() {
    return m_payload;
  }

  @Override
  public Samples samples() {
    return getDecoder().decode();
  }

  private Decode getDecoder() {
    DataOnlyBlockette_1000 dataOnlyBlockette = header().dataOnlyBlockette();
    return dataOnlyBlockette
        .encodingFormat()
        .decoder(
            (encoding, bytes) -> header().endian().createReader(bytes),
            m_payload,
            header().numberOfSamples(),
            m_payload.length);
  }

  @Override
  public byte[] toByteArray() {
    byte[] headerBytes = m_header.toByteArray();
    int totalLength = m_header.offsetToBeginningOfData() + m_payload.length;
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(totalLength)) {
      outputStream.write(headerBytes);
      int bytesWritten = headerBytes.length;

      byte[] buffer = new byte[m_header.offsetToBeginningOfData() - bytesWritten];
      Arrays.fill(buffer, (byte) 0);
      outputStream.write(buffer);

      outputStream.write(m_payload);
      return outputStream.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public JSONObject toJson() {
    JSONObject json = new JSONObject();
    DataOnlyBlockette_1000 dataOnly = header().dataOnlyBlockette();
    json.put("SID", header().sourceIdentifier().toString());
    json.put("RecordLength", dataOnly.dataRecordLength());
    json.put("FormatVersion", "2.4");
    json.put("Flags", m_header.flagsJson());
    json.put("StartTime", DataRecordHeader.DATE_TIME_FORMAT.format(m_header.recordStartTime()));
    json.put("EncodingFormat", dataOnly.encodingFormat().code());
    json.put("SampleRate", header().sampleRate().sampleRateDouble());
    json.put("SampleCount", m_header.numberOfSamples());
    return json;
  }

  @Override
  public int length() {
    return m_header.offsetToBeginningOfData() + m_payload.length;
  }

  public static Builder builder() {
    return new AutoBuilder_DataRecord2_Builder();
  }

  @AutoBuilder
  public abstract static class Builder {

    public DataRecord2 read(InputStream input) throws IOException {
      DataRecord2Header header = DataRecord2Header.builder().read(input).build();
      header(header);
      payload(
          input.readNBytes(
              header.dataOnlyBlockette().dataRecordLength() - header.offsetToBeginningOfData()));
      return build();
    }

    public abstract Builder header(DataRecord2Header header);

    abstract DataRecord2Header header();

    public abstract Builder payload(byte[] payload);

    abstract byte[] payload();

    public abstract DataRecord2 build();
  }
}
