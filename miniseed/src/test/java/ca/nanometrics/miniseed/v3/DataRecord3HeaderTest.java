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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.nanometrics.miniseed.SourceIdentifier;
import ca.nanometrics.miniseed.encoding.DataEncoding;
import ca.nanometrics.miniseed.endian.LittleEndian;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.zip.CRC32C;
import org.junit.jupiter.api.Test;

class DataRecord3HeaderTest {

  @Test
  public void testBuilder() {
    assertThrows(IllegalStateException.class, () -> DataRecord3Header.builder().build());

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    SourceIdentifier sid =
        UriSourceIdentifier.builder().network("NX").station("STN1").channel("BHZ").build();
    DataRecord3Header header =
        DataRecord3Header.builder()
            .flags(DataRecord3Header.Flags.builder().calibrationSignalPresent(true).build())
            .recordStartTime(now)
            .dataPayloadEncoding(DataEncoding.STEIM2)
            .sampleRate(100)
            .numberOfSamples(1000)
            .dataPublicationVersion((short) 1)
            .sourceIdentifier(sid)
            .lengthOfDataPayload(1000)
            .build();
    assertThat(header.recordHeaderIndicator(), is("MS"));
    assertThat(header.formatVersion(), is((byte) 3));
    assertThat(header.flags().toByte(), is((byte) 0x01));
    assertThat(header.recordStartTime(), is(now));
    assertThat(header.dataPayloadEncoding(), is(DataEncoding.STEIM2));
    assertThat(header.sampleRate().sampleRateDouble(), is(100d));
    assertThat(header.numberOfSamples(), is(1000));
    assertThat(header.crc(), is(0L));
  }

  @Test
  public void testToBuilder() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    SourceIdentifier sid =
        UriSourceIdentifier.builder().network("NX").station("STN1").channel("BHZ").build();
    DataRecord3Header header =
        DataRecord3Header.builder()
            .flags(DataRecord3Header.Flags.builder().calibrationSignalPresent(true).build())
            .recordStartTime(now)
            .dataPayloadEncoding(DataEncoding.STEIM2)
            .sampleRate(100)
            .numberOfSamples(1000)
            .dataPublicationVersion((short) 1)
            .sourceIdentifier(sid)
            .lengthOfDataPayload(1000)
            .build();

    assertThat(header.toBuilder().build(), is(equalTo(header)));
    assertThat(
        header.toBuilder().crc(header.updateCrc32C(new CRC32C()).getValue()).build(),
        is(not(equalTo(header))));
  }

  @Test
  public void testToByteArray() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    SourceIdentifier sid =
        UriSourceIdentifier.builder().network("NX").station("STN1").channel("BHZ").build();
    DataRecord3Header header =
        DataRecord3Header.builder()
            .flags(DataRecord3Header.Flags.builder().calibrationSignalPresent(true).build())
            .recordStartTime(now)
            .dataPayloadEncoding(DataEncoding.STEIM2)
            .sampleRate(100)
            .numberOfSamples(1000)
            .dataPublicationVersion((short) 1)
            .sourceIdentifier(sid)
            .lengthOfDataPayload(1000)
            .build();
    byte[] bytes = header.toByteArray();
    assertThat(new String(bytes, 0, 2, StandardCharsets.US_ASCII), is("MS"));
    assertThat("formatVersion", bytes[2], is((byte) 3));
    assertThat("flags", bytes[3], is((byte) 1));
    assertThat("nanoseconds", LittleEndian.get().readInt(bytes, 4), is(now.getNano()));
    assertThat("year", LittleEndian.get().readUShort(bytes, 8), is(now.getYear()));
    assertThat("dayOfYear", LittleEndian.get().readUShort(bytes, 10), is(now.getDayOfYear()));
    assertThat("hour", bytes[12], is((byte) now.getHour()));
    assertThat("minute", bytes[13], is((byte) now.getMinute()));
    assertThat("second", bytes[14], is((byte) now.getSecond()));
    assertThat("dataPayloadEncoding", bytes[15], is(DataEncoding.STEIM2.code()));
    assertThat("sampleRate", LittleEndian.get().readDouble(bytes, 16), is(100d));
    assertThat("numberOfSamples", LittleEndian.get().readUInt(bytes, 24), is(1000L));
    assertThat("crc", LittleEndian.get().readUInt(bytes, 28), is(0L));
    assertThat("dataPublicationVersion", LittleEndian.get().readUByte(bytes, 32), is((short) 1));
    assertThat(
        "lengthOfIdentifier",
        LittleEndian.get().readUByte(bytes, 33),
        is((short) sid.toString().length()));
    assertThat("lengthOfExtraHeaders", LittleEndian.get().readUShort(bytes, 34), is(0));
    assertThat("lengthOfDataPayload", LittleEndian.get().readUInt(bytes, 36), is(1000L));
    assertThat(
        "sourceIdentifier",
        new String(bytes, 40, sid.toString().length(), StandardCharsets.US_ASCII),
        is(sid.toString()));
  }
}
