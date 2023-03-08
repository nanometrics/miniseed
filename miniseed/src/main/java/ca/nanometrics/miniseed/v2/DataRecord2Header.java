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

import ca.nanometrics.miniseed.DataRecordHeader;
import ca.nanometrics.miniseed.endian.BigEndian;
import ca.nanometrics.miniseed.endian.Endian;
import ca.nanometrics.miniseed.endian.LittleEndian;
import ca.nanometrics.miniseed.util.ByteArray;
import ca.nanometrics.miniseed.v2.blockettes.Blockette;
import ca.nanometrics.miniseed.v2.blockettes.Blockette.Loader;
import ca.nanometrics.miniseed.v2.blockettes.DataOnlyBlockette_1000;
import com.google.auto.value.AutoBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.json.JSONObject;

public record DataRecord2Header(
    Endian endian,
    int sequenceNumber,
    DataRecord2Header.QualityIndicator qualityIndicator,
    Scnl sourceIdentifier,
    int year,
    int dayOfYear,
    byte hour,
    byte minute,
    byte second,
    short hundredMicroseconds,
    int numberOfSamples,
    V2SampleRate sampleRate,
    DataRecord2Header.ActivityFlags activityFlags,
    DataRecord2Header.IOFlags ioFlags,
    DataRecord2Header.DataQualityFlags dataQualityFlags,
    int numberOfBlockettesThatFollow,
    long timeCorrection,
    int offsetToBeginningOfData,
    int offsetToFirstDataBlockette,
    Map<Integer, Blockette> blockettes,
    ByteArray byteArray)
    implements DataRecordHeader {

  public static final int FIXED_HEADER_SIZE = 48;

  public void write(OutputStream stream) throws IOException {
    stream.write(toByteArray());
  }

  public byte[] toByteArray() {
    return byteArray.byteArray();
  }

  @Override
  public int length() {
    return FIXED_HEADER_SIZE + blockettes.values().stream().mapToInt(Blockette::length).sum();
  }

  @Override
  public OffsetDateTime recordStartTime() {
    return OffsetDateTime.of(
            year, 1, 1, hour, minute, second, hundredMicroseconds * 100_000, ZoneOffset.UTC)
        .withDayOfYear(dayOfYear);
  }

  public DataOnlyBlockette_1000 dataOnlyBlockette() {
    return (DataOnlyBlockette_1000) blockettes().get(DataOnlyBlockette_1000.TYPE);
  }

  JSONObject flagsJson() {
    JSONObject json = new JSONObject();
    if (activityFlags().calibrationSignalsPresent()) {
      json.put("CalibrationSignalPresent", true);
    }
    if (ioFlags().clockLocked()) {
      json.put("ClockLocked", true);
    }
    return json;
  }

  public enum QualityIndicator {
    UNKNOWN('D'),
    RAW('R'),
    QUALITY_CONTROLLED('Q'),
    MODIFIED('M');

    private final char m_code;

    QualityIndicator(char code) {
      m_code = code;
    }

    public char code() {
      return m_code;
    }

    public static QualityIndicator fromCode(char code) {
      for (QualityIndicator value : values()) {
        if (value.m_code == code) {
          return value;
        }
      }
      throw new IllegalArgumentException("Unknown code: [" + code + "]");
    }
  }

  private static byte bit(boolean b) {
    return (byte) (b ? 1 : 0);
  }

  public record ActivityFlags(
      boolean calibrationSignalsPresent,
      boolean timeCorrectionApplied,
      boolean beginningOfEventStationTrigger,
      boolean endOfEventStationDetrigger,
      boolean positiveLeapSecondDuringRecord,
      boolean negativeLeapSecondDuringRecord,
      boolean eventInProgress,
      boolean bit7) {

    public static ActivityFlags fromByte(byte b) {
      return new ActivityFlags(
          (b & 0x01) == 0x01,
          (b & 0x02) == 0x02,
          (b & 0x04) == 0x04,
          (b & 0x08) == 0x08,
          (b & 0x10) == 0x10,
          (b & 0x20) == 0x20,
          (b & 0x40) == 0x40,
          (b & 0x80) == 0x80);
    }

    public byte toByte() {
      return (byte)
          (bit(calibrationSignalsPresent)
              | bit(timeCorrectionApplied) << 1
              | bit(beginningOfEventStationTrigger) << 2
              | bit(endOfEventStationDetrigger) << 3
              | bit(positiveLeapSecondDuringRecord) << 4
              | bit(negativeLeapSecondDuringRecord) << 5
              | bit(eventInProgress) << 6
              | bit(bit7) << 7);
    }
  }

  public record IOFlags(
      boolean stationVolumeParityErrorPossiblyPresent,
      boolean longRecordRead,
      boolean shortRecordRead,
      boolean startOfTimeSeries,
      boolean endOfTimeSeries,
      boolean clockLocked,
      boolean bit6,
      boolean bit7) {

    public static IOFlags fromByte(byte b) {
      return new IOFlags(
          (b & 0x01) == 0x01,
          (b & 0x02) == 0x02,
          (b & 0x04) == 0x04,
          (b & 0x08) == 0x08,
          (b & 0x10) == 0x10,
          (b & 0x20) == 0x20,
          (b & 0x40) == 0x40,
          (b & 0x80) == 0x80);
    }

    public byte toByte() {
      return (byte)
          (bit(stationVolumeParityErrorPossiblyPresent)
              | bit(longRecordRead) << 1
              | bit(shortRecordRead) << 2
              | bit(startOfTimeSeries) << 3
              | bit(endOfTimeSeries) << 4
              | bit(clockLocked) << 5
              | bit(bit6) << 6
              | bit(bit7) << 7);
    }
  }

  public record DataQualityFlags(
      boolean amplifierSaturationDetected,
      boolean digitizerClippingDetected,
      boolean spikesDetected,
      boolean glitchesDetected,
      boolean missingDataPresent,
      boolean telemetrySynchronizationError,
      boolean digitizerFilterMayBeCharging,
      boolean timeTagIsQuestionable) {

    public static DataQualityFlags fromByte(byte b) {
      return new DataQualityFlags(
          (b & 0x01) == 0x01,
          (b & 0x02) == 0x02,
          (b & 0x04) == 0x04,
          (b & 0x08) == 0x08,
          (b & 0x10) == 0x10,
          (b & 0x20) == 0x20,
          (b & 0x40) == 0x40,
          (b & 0x80) == 0x80);
    }

    public byte toByte() {
      return (byte)
          (bit(amplifierSaturationDetected)
              | bit(digitizerClippingDetected) << 1
              | bit(spikesDetected) << 2
              | bit(glitchesDetected) << 3
              | bit(missingDataPresent) << 4
              | bit(telemetrySynchronizationError) << 5
              | bit(digitizerFilterMayBeCharging) << 6
              | bit(timeTagIsQuestionable) << 7);
    }
  }

  public static Builder builder() {
    return new AutoBuilder_DataRecord2Header_Builder();
  }

  @AutoBuilder
  public abstract static class Builder {

    static final int OFFSET_DATA_RECORD_START_TIME_YEAR = 20; // 2-bytes
    static final int OFFSET_DATA_RECORD_START_TIME_DAY_OF_YEAR = 22; // 2-bytes

    public Builder read(final InputStream input) throws IOException {
      byte[] bytes = input.readNBytes(FIXED_HEADER_SIZE);
      Endian endian = isBigEndian(bytes) ? BigEndian.get() : LittleEndian.get();
      endian(endian);
      sequenceNumber(Integer.parseInt(new String(bytes, 0, 6, StandardCharsets.US_ASCII)));
      qualityIndicator(QualityIndicator.fromCode((char) bytes[6]));
      // reserved bit 7
      String station = new String(bytes, 8, 5, StandardCharsets.UTF_8).trim();
      String location = new String(bytes, 13, 2, StandardCharsets.UTF_8).trim();
      String channel = new String(bytes, 15, 3, StandardCharsets.UTF_8).trim();
      String network = new String(bytes, 18, 2, StandardCharsets.UTF_8).trim();
      sourceIdentifier(
          Scnl.builder()
              .network(network)
              .station(station)
              .location(location)
              .channel(channel)
              .build());
      year(endian.readUShort(bytes, OFFSET_DATA_RECORD_START_TIME_YEAR)); // offset 20
      dayOfYear(endian.readUShort(bytes, OFFSET_DATA_RECORD_START_TIME_DAY_OF_YEAR)); // offset 22
      hour(bytes[24]);
      minute(bytes[25]);
      second(bytes[26]);
      // byte 27 unused
      hundredMicroseconds(endian.readShort(bytes, 28));
      numberOfSamples(endian.readUShort(bytes, 30));
      short sampleRateFactor = endian.readShort(bytes, 32);
      short sampleRateMultiplier = endian.readShort(bytes, 34);
      sampleRate(FractionalSampleRate.get(sampleRateFactor, sampleRateMultiplier));
      activityFlags(ActivityFlags.fromByte(bytes[36]));
      ioFlags(IOFlags.fromByte(bytes[37]));
      dataQualityFlags(DataQualityFlags.fromByte(bytes[38]));
      numberOfBlockettesThatFollow(endian.readUByte(bytes, 39));
      timeCorrection(endian.readUInt(bytes, 40));
      offsetToBeginningOfData(endian.readUShort(bytes, 44));
      offsetToFirstDataBlockette(endian.readUShort(bytes, 46));

      int numberOfBlockettesThatFollow = numberOfBlockettesThatFollow();
      int offsetToFirstDataBlockette = offsetToFirstDataBlockette();
      Map<Integer, Blockette> blockettes = new LinkedHashMap<>(numberOfBlockettesThatFollow());
      if (numberOfBlockettesThatFollow > 0 && offsetToFirstDataBlockette > 0) {
        if (offsetToFirstDataBlockette > DataRecord2Header.FIXED_HEADER_SIZE) {
          input.readNBytes(
              offsetToFirstDataBlockette - DataRecord2Header.FIXED_HEADER_SIZE); // skip bytes
        }
        for (int i = 0; i < numberOfBlockettesThatFollow; i++) {
          int blocketteType = endian.readUShort(input.readNBytes(2), 0);
          int nextBlockettesByteNumber = endian.readUShort(input.readNBytes(2), 0);
          Supplier<Loader> loader = Blockette.LOADERS.get(blocketteType);
          if (loader == null) {
            throw new IllegalStateException("Unknown blockette type " + blocketteType);
          }
          Blockette blockette =
              loader.get().load(blocketteType, nextBlockettesByteNumber, input, endian);
          blockettes.put(blocketteType, blockette);
        }
        blockettes(blockettes);
      }
      DataOnlyBlockette_1000 dataOnly =
          (DataOnlyBlockette_1000) blockettes.get(DataOnlyBlockette_1000.TYPE);
      if (dataOnly == null) {
        throw new IllegalStateException("Missing data only SEED blockette");
      }

      // skip bytes to beginning of data
      input.readNBytes(
          offsetToBeginningOfData()
              - DataRecord2Header.FIXED_HEADER_SIZE
              - blockettes.values().stream().mapToInt(Blockette::length).sum());

      return this;
    }

    /**
     * Check the year and day-of-year fields to determine if the record is big-endian or
     * little-endian.
     */
    private boolean isBigEndian(byte[] bytes) {
      return isValidYearDay(bytes, BigEndian.get()::readShort);
    }

    public abstract Builder endian(Endian endian);

    abstract Endian endian();

    private static boolean isValidYearDay(
        byte[] bytes, BiFunction<byte[], Integer, Short> shortReader) {
      short year = shortReader.apply(bytes, OFFSET_DATA_RECORD_START_TIME_YEAR);
      short dayOfYear = shortReader.apply(bytes, OFFSET_DATA_RECORD_START_TIME_DAY_OF_YEAR);
      return year >= 1900 && year <= 2100 && dayOfYear >= 1 && dayOfYear <= 366;
    }

    public abstract Builder sequenceNumber(int sequenceNumber);

    abstract int sequenceNumber();

    public abstract Builder qualityIndicator(QualityIndicator qualityIndicator);

    abstract QualityIndicator qualityIndicator();

    public abstract Builder sourceIdentifier(Scnl sourceIdentifier);

    abstract Scnl sourceIdentifier();

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

    /** 0.0001 seconds (0-9999) */
    public abstract Builder hundredMicroseconds(short value);

    abstract short hundredMicroseconds();

    public DataRecord2Header.Builder byteArray(byte[] byteArray) {
      return byteArray(new ByteArray(byteArray));
    }

    public abstract DataRecord2Header.Builder byteArray(ByteArray byteArray);

    public Builder recordStartTime(OffsetDateTime time) {
      year(time.getYear());
      dayOfYear(time.getDayOfYear());
      hour((byte) time.getHour());
      minute((byte) time.getMinute());
      second((byte) time.getSecond());
      hundredMicroseconds((short) (time.getNano() / 100_000));
      return this;
    }

    public abstract Builder numberOfSamples(int numberOfSamples);

    abstract int numberOfSamples();

    public abstract Builder sampleRate(V2SampleRate sampleRate);

    abstract V2SampleRate sampleRate();

    public abstract Builder activityFlags(ActivityFlags activityFlags);

    abstract ActivityFlags activityFlags();

    public abstract Builder ioFlags(IOFlags ioFlags);

    abstract IOFlags ioFlags();

    public abstract Builder dataQualityFlags(DataQualityFlags dataQualityFlags);

    abstract DataQualityFlags dataQualityFlags();

    public abstract Builder numberOfBlockettesThatFollow(int numberOfBlockettesThatFollow);

    abstract int numberOfBlockettesThatFollow();

    public abstract Builder timeCorrection(long timeCorrection);

    abstract long timeCorrection();

    public abstract Builder offsetToBeginningOfData(int offsetToBeginningOfData);

    abstract int offsetToBeginningOfData();

    public abstract Builder offsetToFirstDataBlockette(int offsetToFirstDataBlockette);

    abstract int offsetToFirstDataBlockette();

    public abstract Builder blockettes(Map<Integer, Blockette> blockettes);

    abstract Map<Integer, Blockette> blockettes();

    private byte[] buildByteArray(Endian writer) {
      byte[] result = new byte[offsetToBeginningOfData()];
      writer.writeString(result, 0, padStart(Integer.toString(sequenceNumber()), 6, '0'));
      result[6] = (byte) qualityIndicator().code();
      result[7] = ' '; // reserved byte
      if (sourceIdentifier() == null) {
        throw new IllegalStateException("Must specify sourceIdentifier");
      }
      writer.writeString(result, 8, pad(sourceIdentifier().station(), 5));
      writer.writeString(result, 13, pad(sourceIdentifier().location(), 2));
      writer.writeString(result, 15, pad(sourceIdentifier().channel(), 3));
      writer.writeString(result, 18, pad(sourceIdentifier().network(), 2));
      writeBTime(writer, result, 20);
      writer.writeShort(result, 30, (short) numberOfSamples());
      writer.writeShort(result, 32, (short) sampleRate().factor());
      writer.writeShort(result, 34, (short) sampleRate().multiplier());
      result[36] = activityFlags().toByte();
      result[37] = ioFlags().toByte();
      result[38] = dataQualityFlags().toByte();
      result[39] = (byte) numberOfBlockettesThatFollow();
      writer.writeInt(result, 40, (int) timeCorrection());
      writer.writeShort(result, 44, (short) offsetToBeginningOfData());
      writer.writeShort(result, 46, (short) offsetToFirstDataBlockette());
      int offset = 48;
      for (Blockette blockette : blockettes().values()) {
        byte[] buffer = blockette.toByteArray(endian());
        System.arraycopy(buffer, 0, result, offset, buffer.length);
        offset += buffer.length;
      }
      return result;
    }

    private static String pad(String string, int length) {
      return string == null ? padEnd(" ", length, ' ') : padEnd(string, length, ' ');
    }

    private static String padStart(String string, int length, char c) {
      return String.format("%1$" + length + "s", string).replace(' ', c);
    }

    private static String padEnd(String string, int length, char c) {
      return String.format("%1$-" + length + "s", string).replace(' ', c);
    }

    /**
     * Binary data types are used in the BTIME structure: Field type Number of bits Field
     * description
     *
     * <pre>
     * UWORD 16 Year (e.g., 1987)
     * UWORD 16 Day of Year (Jan 1 is 1)
     * UBYTE 8 Hours of day (0—23)
     * UBYTE 8 Minutes of day (0—59)
     * UBYTE 8 Seconds of day (0—59, 60 for leap seconds)
     * UBYTE 8 Unused for data (required for alignment)
     * UWORD 16 .0001 seconds (0—9999)
     * </pre>
     */
    void writeBTime(Endian writer, byte[] bytes, int offset) {
      writer.writeShort(bytes, offset, (short) year());
      writer.writeShort(bytes, offset + 2, (short) dayOfYear());
      bytes[offset + 4] = hour();
      bytes[offset + 5] = minute();
      bytes[offset + 6] = second();
      bytes[offset + 7] = 0; // unused
      writer.writeShort(bytes, offset + 8, hundredMicroseconds());
    }

    abstract DataRecord2Header autoBuild();

    public DataRecord2Header build() {
      byteArray(buildByteArray(endian()));
      return autoBuild();
    }
  }
}
