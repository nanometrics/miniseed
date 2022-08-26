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

import ca.nanometrics.miniseed.v2.DataRecord2Header;
import java.util.List;
import java.util.function.Function;
import org.json.JSONObject;

public class ExtraHeader<T> {

  public static final List<ExtraHeader<?>> FDSN_RESERVED_HEADERS =
      List.of(
          new ExtraHeader<Integer>("FDSN.Sequence", h -> h.sequenceNumber()),
          new ExtraHeader<Boolean>(
              "FDSN.Event.Begin", h -> h.activityFlags().beginningOfEventStationTrigger()),
          new ExtraHeader<Boolean>(
              "FDSN.Event.End", h -> h.activityFlags().endOfEventStationDetrigger()),
          new ExtraHeader<Boolean>(
              "FDSN.Event.InProgress", h -> h.activityFlags().eventInProgress()),
          new ExtraHeader<Integer>(
              "FDSN.Time.LeapSecond",
              h ->
                  (h.activityFlags().positiveLeapSecondDuringRecord()
                          || h.activityFlags().negativeLeapSecondDuringRecord())
                      ? Integer.valueOf(h.activityFlags().positiveLeapSecondDuringRecord() ? 1 : -1)
                      : null),
          new ExtraHeader<Boolean>(
              "FDSN.Flags.StationVolumeParityError",
              h -> h.ioFlags().stationVolumeParityErrorPossiblyPresent(),
              true),
          new ExtraHeader<Boolean>(
              "FDSN.Flags.LongRecordRead", h -> h.ioFlags().longRecordRead(), true),
          new ExtraHeader<Boolean>(
              "FDSN.Flags.ShortRecordRead", h -> h.ioFlags().shortRecordRead(), true),
          new ExtraHeader<Boolean>(
              "FDSN.Flags.StartOfTimeSeries", h -> h.ioFlags().startOfTimeSeries(), true),
          new ExtraHeader<Boolean>(
              "FDSN.Flags.EndOfTimeSeries", h -> h.ioFlags().endOfTimeSeries(), true),
          new ExtraHeader<Boolean>(
              "FDSN.Flags.AmplifierSaturation",
              h -> h.dataQualityFlags().amplifierSaturationDetected()),
          new ExtraHeader<Boolean>(
              "FDSN.Flags.DigitizerClipping",
              h -> h.dataQualityFlags().digitizerClippingDetected()),
          new ExtraHeader<Boolean>("FDSN.Flags.Spikes", h -> h.dataQualityFlags().spikesDetected()),
          new ExtraHeader<Boolean>(
              "FDSN.Flags.Glitches", h -> h.dataQualityFlags().glitchesDetected()),
          new ExtraHeader<Boolean>(
              "FDSN.Flags.MissingData", h -> h.dataQualityFlags().missingDataPresent(), true),
          new ExtraHeader<Boolean>(
              "FDSN.Flags.TelemetrySyncError",
              h -> h.dataQualityFlags().telemetrySynchronizationError(),
              true),
          new ExtraHeader<Boolean>(
              "FDSN.Flags.FilterCharging",
              h -> h.dataQualityFlags().digitizerFilterMayBeCharging()),
          new ExtraHeader<Long>("FDSN.Time.Correction", h -> h.timeCorrection()));
  private final String m_name;
  private final Function<DataRecord2Header, T> m_fromV2;
  private final boolean m_deprecated;

  public ExtraHeader(String name, Function<DataRecord2Header, T> fromV2) {
    this(name, fromV2, false);
  }

  public ExtraHeader(String name, Function<DataRecord2Header, T> fromV2, boolean deprecated) {
    m_name = name;
    m_fromV2 = fromV2;
    m_deprecated = deprecated;
  }

  public String name() {
    return m_name;
  }

  public boolean isDeprecated() {
    return m_deprecated;
  }

  public void add(DataRecord2Header header, JSONObject extraHeaders) {
    Object value = m_fromV2.apply(header);
    if (value != null && !isValueFalse(value)) {
      String[] split = m_name.split("\\.");
      JSONObject current = extraHeaders;
      for (int i = 0; i < split.length - 1; i++) {
        current = getOrAddJsonObject(current, split[i]);
      }
      current.put(split[split.length - 1], value);
    }
  }

  private static boolean isValueFalse(Object value) {
    return value.getClass().equals(Boolean.class) && value.equals(Boolean.FALSE);
  }

  private static JSONObject getOrAddJsonObject(JSONObject json, String name) {
    JSONObject value = json.optJSONObject(name);
    if (value == null) {
      value = new JSONObject();
      json.put(name, value);
    }
    return value;
  }
}
