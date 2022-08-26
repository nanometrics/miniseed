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
import static org.hamcrest.Matchers.is;

import ca.nanometrics.miniseed.endian.BigEndian;
import ca.nanometrics.miniseed.v2.DataRecord2Header;
import ca.nanometrics.miniseed.v2.DataRecord2Header.ActivityFlags;
import ca.nanometrics.miniseed.v2.DataRecord2Header.Builder;
import ca.nanometrics.miniseed.v2.DataRecord2Header.DataQualityFlags;
import ca.nanometrics.miniseed.v2.DataRecord2Header.IOFlags;
import ca.nanometrics.miniseed.v2.DataRecord2Header.QualityIndicator;
import ca.nanometrics.miniseed.v2.FractionalSampleRate;
import ca.nanometrics.miniseed.v2.Scnl;
import java.util.Map;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class ExtraHeaderTest {

  @Test
  public void test() {
    ExtraHeader<?> header = ExtraHeader.FDSN_RESERVED_HEADERS.get(0);
    assertThat(header.name(), is("FDSN.Sequence"));
    DataRecord2Header v2header = v2header().sequenceNumber(52).build();
    JSONObject json = new JSONObject();
    header.add(v2header, json);
    assertThat(json.getJSONObject("FDSN").getInt("Sequence"), is(52));

    header = ExtraHeader.FDSN_RESERVED_HEADERS.get(1);
    assertThat(header.name(), is("FDSN.Event.Begin"));
    v2header =
        v2header()
            .activityFlags(new ActivityFlags(false, false, true, false, false, false, false, false))
            .build();
    json = new JSONObject();
    header.add(v2header, json);
    assertThat(json.getJSONObject("FDSN").getJSONObject("Event").getBoolean("Begin"), is(true));
  }

  private static Builder v2header() {
    return DataRecord2Header.builder()
        .sequenceNumber(1)
        .endian(BigEndian.get())
        .sourceIdentifier(Scnl.build("NX.STN.HHZ"))
        .offsetToBeginningOfData(64)
        .offsetToFirstDataBlockette(0)
        .numberOfBlockettesThatFollow(0)
        .qualityIndicator(QualityIndicator.UNKNOWN)
        .dataQualityFlags(
            new DataQualityFlags(false, false, false, false, false, false, false, false))
        .ioFlags(new IOFlags(false, false, false, false, false, false, false, false))
        .activityFlags(new ActivityFlags(false, false, false, false, false, false, false, false))
        .numberOfSamples(100)
        .sampleRate(FractionalSampleRate.get(100))
        .year(2022)
        .dayOfYear(123)
        .hour((byte) 12)
        .minute((byte) 12)
        .second((byte) 12)
        .hundredMicroseconds((short) 0)
        .timeCorrection(0)
        .blockettes(Map.of());
  }
}
