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

import ca.nanometrics.miniseed.DataRecordHeader;
import ca.nanometrics.miniseed.SampleRate;
import ca.nanometrics.miniseed.SourceIdentifier;
import ca.nanometrics.miniseed.UnexpectedMiniSeedVersionException;
import ca.nanometrics.miniseed.encoding.DataEncoding;
import ca.nanometrics.miniseed.endian.Endian;
import ca.nanometrics.miniseed.endian.LittleEndian;
import ca.nanometrics.miniseed.util.ByteArray;
import com.google.auto.value.AutoBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.zip.CRC32C;
import javax.annotation.Nullable;
import org.json.JSONObject;

public record DataRecord3Header(
    String recordHeaderIndicator,
    byte formatVersion,
    DataRecord3Header.Flags flags,
    int nanoseconds,
    int year,
    int dayOfYear,
    byte hour,
    byte minute,
    byte second,
    DataEncoding dataPayloadEncoding,
    DataRecord3Header.Float64SampleRate sampleRate,
    int numberOfSamples,
    long crc,
    short dataPublicationVersion,
    long lengthOfDataPayload,
    SourceIdentifier sourceIdentifier,
    @Nullable JSONObject extraHeaderFields,
    // keep raw string as well, to preserve formatting that was read
    @Nullable String extraHeaderFieldsString,
    ByteArray byteArray)
    implements DataRecordHeader {

  public static final int FIXED_HEADER_SIZE = 40;

  @Override
  public OffsetDateTime recordStartTime() {
    return OffsetDateTime.of(year, 1, 1, hour, minute, second, nanoseconds, ZoneOffset.UTC)
        .withDayOfYear(dayOfYear);
  }

  public void write(OutputStream stream) throws IOException {
    stream.write(toByteArray());
  }

  public byte[] toByteArray() {
    return byteArray.byteArray();
  }

  @Override
  public int length() {
    return byteArray.length();
  }

  private static void writeString(byte[] bytes, int index, String string) {
    byte[] stringBytes = string.getBytes(StandardCharsets.US_ASCII);
    System.arraycopy(stringBytes, 0, bytes, index, stringBytes.length);
  }

  public Builder toBuilder() {
    return new AutoBuilder_DataRecord3Header_Builder()
        .recordHeaderIndicator(recordHeaderIndicator)
        .formatVersion(formatVersion)
        .flags(flags)
        .recordStartTime(recordStartTime())
        .dataPayloadEncoding(dataPayloadEncoding)
        .sampleRate(sampleRate)
        .numberOfSamples(numberOfSamples)
        .crc(crc)
        .dataPublicationVersion(dataPublicationVersion)
        .lengthOfDataPayload(lengthOfDataPayload)
        .sourceIdentifier(sourceIdentifier)
        .extraHeaderFields(extraHeaderFields)
        .extraHeaderFieldsString(extraHeaderFieldsString);
  }

  public CRC32C updateCrc32C(CRC32C crcToUpdate) {
    crcToUpdate.update(toByteArray());
    return crcToUpdate;
  }

  public int lengthOfIdentifier() {
    return sourceIdentifier.toString().length();
  }

  public int lengthOfExtraHeaders() {

    return extraHeaderFieldsString == null
        ? (extraHeaderFields == null ? 0 : extraHeaderFields.toString().length())
        : extraHeaderFieldsString.length();
  }

  public long recordLength() {
    return FIXED_HEADER_SIZE + lengthOfIdentifier() + lengthOfExtraHeaders() + lengthOfDataPayload;
  }

  public JSONObject toJson() {
    JSONObject json = new JSONObject();
    json.put("SID", sourceIdentifier.toString());
    json.put("RecordLength", recordLength());
    json.put("FormatVersion", formatVersion);
    json.put("Flags", flags.toJson());
    json.put("StartTime", DATE_TIME_FORMAT.format(recordStartTime()));
    json.put("EncodingFormat", dataPayloadEncoding.code());
    json.put("SampleRate", sampleRate.sampleRateDouble());
    json.put("SampleCount", numberOfSamples);
    json.put("CRC", "0x" + Long.toHexString(crc).toUpperCase());
    json.put("PublicationVersion", dataPublicationVersion);
    json.put("ExtraLength", lengthOfExtraHeaders());
    json.put("DataLength", lengthOfDataPayload);
    if (extraHeaderFields != null) {
      json.put("ExtraHeaders", extraHeaderFields);
    }
    return json;
  }

  public static Builder builder() {
    return new AutoBuilder_DataRecord3Header_Builder()
        .recordHeaderIndicator("MS")
        .formatVersion((byte) 3)
        .crc(0);
  }

  @AutoBuilder
  public abstract static class Builder {

    public Builder read(InputStream input) throws IOException {
      Endian reader = LittleEndian.get();
      byte[] bytes = input.readNBytes(FIXED_HEADER_SIZE);
      if (!DataRecord3.isMiniSeed3(bytes)) {
        throw new UnexpectedMiniSeedVersionException(
            "Expected record header indicator MS, but got "
                + new String(bytes, 0, 2, StandardCharsets.US_ASCII));
      }
      recordHeaderIndicator(new String(bytes, 0, 2, StandardCharsets.US_ASCII));
      byte formatVersion = bytes[2];
      if (formatVersion != 3) {
        throw new UnexpectedMiniSeedVersionException(
            "Expected format version 3, but got " + formatVersion);
      }
      formatVersion(formatVersion);
      flags(Flags.builder().fromByte(bytes[3]));
      nanoseconds(reader.readInt(bytes, 4));
      year(reader.readUShort(bytes, 8));
      dayOfYear(reader.readUShort(bytes, 10));
      hour(bytes[12]);
      minute(bytes[13]);
      second(bytes[14]);
      dataPayloadEncoding(DataEncoding.fromCode(bytes[15]));
      sampleRate(new Float64SampleRate(reader.readDouble(bytes, 16)));
      numberOfSamples((int) reader.readUInt(bytes, 24));
      crc(reader.readUInt(bytes, 28));
      dataPublicationVersion(bytes[32]);
      short lengthOfIdentifier = reader.readUByte(bytes, 33);
      int lengthOfExtraHeaders = reader.readUShort(bytes, 34);
      lengthOfDataPayload(reader.readUInt(bytes, 36));

      sourceIdentifier(
          SourceIdentifier.parse(
              new String(input.readNBytes(lengthOfIdentifier), StandardCharsets.US_ASCII)));
      if (lengthOfExtraHeaders != 0) {
        String string =
            new String(input.readNBytes(lengthOfExtraHeaders), StandardCharsets.US_ASCII);
        extraHeaderFieldsString(string);
        extraHeaderFields(new JSONObject(string));
      }
      return this;
    }

    public abstract Builder recordHeaderIndicator(String recordHeaderIndicator);

    abstract String recordHeaderIndicator();

    public abstract Builder formatVersion(byte formatVersion);

    abstract byte formatVersion();

    public abstract Builder flags(Flags flags);

    abstract Flags flags();

    public Builder recordStartTime(OffsetDateTime recordStartTime) {
      nanoseconds(recordStartTime.get(ChronoField.NANO_OF_SECOND));
      year(recordStartTime.get(ChronoField.YEAR));
      dayOfYear(recordStartTime.get(ChronoField.DAY_OF_YEAR));
      hour((byte) recordStartTime.get(ChronoField.HOUR_OF_DAY));
      minute((byte) recordStartTime.get(ChronoField.MINUTE_OF_HOUR));
      second((byte) recordStartTime.get(ChronoField.SECOND_OF_MINUTE));
      return this;
    }

    public abstract Builder nanoseconds(int nanoseconds);

    abstract int nanoseconds();

    public abstract Builder year(int year);

    abstract int year();

    public abstract Builder dayOfYear(int dayOfYear);

    abstract int dayOfYear();

    public abstract Builder hour(byte hour);

    abstract byte hour();

    public abstract Builder minute(byte minute);

    abstract byte minute();

    public abstract Builder second(byte second);

    abstract byte second();

    public abstract Builder dataPayloadEncoding(DataEncoding dataPayloadEncoding);

    abstract DataEncoding dataPayloadEncoding();

    public Builder sampleRate(int sampleRate) {
      return sampleRate(new Float64SampleRate((double) sampleRate));
    }

    public abstract Builder sampleRate(Float64SampleRate sampleRate);

    abstract Float64SampleRate sampleRate();

    public abstract Builder numberOfSamples(int numberOfSamples);

    abstract int numberOfSamples();

    public abstract Builder crc(long crc);

    abstract long crc();

    public abstract Builder dataPublicationVersion(short dataPublicationVersion);

    abstract short dataPublicationVersion();

    public abstract Builder lengthOfDataPayload(long lengthOfDataPayload);

    abstract long lengthOfDataPayload();

    public abstract Builder sourceIdentifier(SourceIdentifier sourceIdentifier);

    abstract SourceIdentifier sourceIdentifier();

    public abstract Builder extraHeaderFields(JSONObject extraHeaderFields);

    public abstract JSONObject extraHeaderFields();

    public abstract Builder extraHeaderFieldsString(String extraHeaderFieldsString);

    abstract String extraHeaderFieldsString();

    public Builder byteArray(byte[] byteArray) {
      return byteArray(new ByteArray(byteArray));
    }

    public abstract Builder byteArray(ByteArray byteArray);

    private byte[] buildByteArray() {
      Endian writer = LittleEndian.get();
      int lengthOfIdentifier = sourceIdentifier().toString().length();
      String extraHeaderFieldsJson =
          extraHeaderFieldsString() == null
              ? (extraHeaderFields() == null ? "" : extraHeaderFields().toString())
              : extraHeaderFieldsString();
      int lengthOfExtraHeaders = extraHeaderFieldsJson.length();
      byte[] bytes = new byte[FIXED_HEADER_SIZE + lengthOfIdentifier + lengthOfExtraHeaders];
      writeString(bytes, 0, recordHeaderIndicator());
      bytes[2] = formatVersion();
      bytes[3] = flags().toByte();
      writer.writeInt(bytes, 4, nanoseconds());
      writer.writeShort(bytes, 8, (short) year());
      writer.writeShort(bytes, 10, (short) dayOfYear());
      bytes[12] = hour();
      bytes[13] = minute();
      bytes[14] = second();
      bytes[15] = dataPayloadEncoding().code();
      writer.writeDouble(bytes, 16, sampleRate().value());
      writer.writeInt(bytes, 24, (int) numberOfSamples());
      writer.writeInt(bytes, 28, (int) crc());
      bytes[32] = (byte) dataPublicationVersion();
      bytes[33] = (byte) lengthOfIdentifier;
      writer.writeShort(bytes, 34, (short) lengthOfExtraHeaders);
      writer.writeInt(bytes, 36, (int) lengthOfDataPayload());
      writeString(bytes, 40, sourceIdentifier().toString());
      if (lengthOfExtraHeaders > 0) {
        writeString(bytes, 40 + lengthOfIdentifier, extraHeaderFieldsJson);
      }
      return bytes;
    }

    abstract DataRecord3Header autoBuild();

    public DataRecord3Header build() {
      byteArray(buildByteArray());
      if (extraHeaderFieldsString() == null && extraHeaderFields() != null) {
        extraHeaderFieldsString(extraHeaderFields().toString());
      } else if (extraHeaderFieldsString() != null && extraHeaderFields() == null) {
        extraHeaderFields(new JSONObject(extraHeaderFieldsString()));
      }
      return autoBuild();
    }
  }

  public record Flags(
      boolean calibrationSignalPresent,
      boolean timeTagIsQuestionable,
      boolean clockLocked,
      boolean bit3,
      boolean bit4,
      boolean bit5,
      boolean bit6,
      boolean bit7) {

    public byte toByte() {
      byte flags = 0;
      if (calibrationSignalPresent) {
        flags |= 0x01;
      }
      if (timeTagIsQuestionable) {
        flags |= 0x02;
      }
      if (clockLocked) {
        flags |= 0x04;
      }
      return flags;
    }

    public JSONObject toJson() {
      JSONObject json = new JSONObject();
      json.put("RawUInt8", toByte());
      if (calibrationSignalPresent) {
        json.put("CalibrationSignalPresent", true);
      }
      if (timeTagIsQuestionable) {
        json.put("TimeTagIsQuestionable", true);
      }
      if (clockLocked) {
        json.put("ClockLocked", true);
      }
      return json;
    }

    public static Builder builder() {
      return new AutoBuilder_DataRecord3Header_Flags_Builder()
          .calibrationSignalPresent(false)
          .timeTagIsQuestionable(false)
          .clockLocked(false)
          .bit3(false)
          .bit4(false)
          .bit5(false)
          .bit6(false)
          .bit7(false);
    }

    @AutoBuilder(ofClass = Flags.class)
    public abstract static class Builder {

      public Flags fromByte(byte flags) {
        calibrationSignalPresent((flags & 0x01) != 0);
        timeTagIsQuestionable((flags & 0x02) != 0);
        clockLocked((flags & 0x04) != 0);
        return build();
      }

      public abstract Builder calibrationSignalPresent(boolean calibrationSignalPresent);

      public abstract Builder timeTagIsQuestionable(boolean timeTagIsQuestionable);

      public abstract Builder clockLocked(boolean clockLocked);

      public abstract Builder bit3(boolean bit3);

      public abstract Builder bit4(boolean bit4);

      public abstract Builder bit5(boolean bit5);

      public abstract Builder bit6(boolean bit6);

      public abstract Builder bit7(boolean bit7);

      public abstract Flags build();
    }
  }

  public record Float64SampleRate(Double value) implements SampleRate {

    @Override
    public double sampleRateDouble() {
      return value < 0 ? -(1 / value) : value;
    }

    @Override
    public int sampleRateInt() {
      return (int) sampleRateDouble();
    }

    @Override
    public double samplePeriod() {
      return value < 0 ? -value : (1 / value);
    }
  }
}
