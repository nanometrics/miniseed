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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;

import ca.nanometrics.miniseed.DataRecordTestHelper;
import ca.nanometrics.miniseed.DataRecordTestHelper.ReferenceData;
import ca.nanometrics.miniseed.Samples;
import ca.nanometrics.miniseed.encoding.DataEncoding;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DataRecord3Test {

  /** https://miniseed3.readthedocs.io/en/latest/appendix.html#b-reference-data */
  private static Stream<Arguments> provideReferenceData() {
    return Stream.of(
        Arguments.of(
            new ReferenceData(
                "https://miniseed3.readthedocs.io/en/latest/_downloads/df7451369f84432864fd5f08fa7d362f/reference-text.mseed3",
                "https://miniseed3.readthedocs.io/en/latest/_downloads/7f744bc96dfb8f0ded199fb0cc7a7fa8/reference-text.json",
                Samples.Type.TEXT,
                DataEncoding.TEXT)),
        Arguments.of(
            new ReferenceData(
                "https://miniseed3.readthedocs.io/en/latest/_downloads/0e8e14b8758c460c2456d4ace91bb667/reference-detectiononly.mseed3",
                "https://miniseed3.readthedocs.io/en/latest/_downloads/a74ac3610ec361ee1255814af2a7367b/reference-detectiononly.json",
                Samples.Type.TEXT,
                DataEncoding.TEXT)),
        Arguments.of(
            new ReferenceData(
                "https://miniseed3.readthedocs.io/en/latest/_downloads/8811da7ada5b4e2b62077c9f19bd6262/reference-sinusoid-int16.mseed3",
                "https://miniseed3.readthedocs.io/en/latest/_downloads/b2b14123d0bbf63e862c3ed50e4ecc47/reference-sinusoid-int16.json",
                Samples.Type.INTEGER,
                DataEncoding.INTEGER_16BIT)),
        Arguments.of(
            new ReferenceData(
                "https://miniseed3.readthedocs.io/en/latest/_downloads/ee50b92886782af46262e390e24984d8/reference-sinusoid-int32.mseed3",
                "https://miniseed3.readthedocs.io/en/latest/_downloads/72bc44a55e2db4e696a6ddda92642409/reference-sinusoid-int32.json",
                Samples.Type.INTEGER,
                DataEncoding.INTEGER_32BIT)),
        Arguments.of(
            new ReferenceData(
                "https://miniseed3.readthedocs.io/en/latest/_downloads/ee3e63aba8b2dd85dd3ec52daadb839f/reference-sinusoid-float32.mseed3",
                "https://miniseed3.readthedocs.io/en/latest/_downloads/cd5867dc18af5f2b5b5ea7967a30a61b/reference-sinusoid-float32.json",
                Samples.Type.FLOAT,
                DataEncoding.FLOAT_32BIT)),
        Arguments.of(
            new ReferenceData(
                "https://miniseed3.readthedocs.io/en/latest/_downloads/ccd7dd8e53364cd4eaa36ca696122a2b/reference-sinusoid-float64.mseed3",
                "https://miniseed3.readthedocs.io/en/latest/_downloads/7bbf9cefd249e4ef3ab9d05b9b3bc743/reference-sinusoid-float64.json",
                Samples.Type.DOUBLE,
                DataEncoding.FLOAT_64BIT)),
        Arguments.of(
            new ReferenceData(
                "https://miniseed3.readthedocs.io/en/latest/_downloads/c90a6fa16641c8f760b408297987eb4e/reference-sinusoid-steim1.mseed3",
                "https://miniseed3.readthedocs.io/en/latest/_downloads/8d7be9aca12c4f27970d7fdd1030fd4e/reference-sinusoid-steim1.json",
                Samples.Type.INTEGER,
                DataEncoding.STEIM1)),
        Arguments.of(
            new ReferenceData(
                "https://miniseed3.readthedocs.io/en/latest/_downloads/7726fd31d0dac9059dc0f31d88e44c99/reference-sinusoid-steim2.mseed3",
                "https://miniseed3.readthedocs.io/en/latest/_downloads/9bad088d31f1e86d9922d0d07539373d/reference-sinusoid-steim2.json",
                Samples.Type.INTEGER,
                DataEncoding.STEIM2)),
        Arguments.of(
            new ReferenceData(
                "https://miniseed3.readthedocs.io/en/latest/_downloads/deb152626dc8f57aa56bd66bd8884051/reference-sinusoid-TQ-TC-ED.mseed3",
                "https://miniseed3.readthedocs.io/en/latest/_downloads/fd7f391ddd87b5d84dad05dbb75bfb37/reference-sinusoid-TQ-TC-ED.json",
                Samples.Type.INTEGER,
                DataEncoding.STEIM2)),
        Arguments.of(
            new ReferenceData(
                "https://miniseed3.readthedocs.io/en/latest/_downloads/11efecaa7c58b04f3beab5aff7a1ddee/reference-sinusoid-FDSN-Other.mseed3",
                "https://miniseed3.readthedocs.io/en/latest/_downloads/9f853670865c4b2012b8c9bf77b26d43/reference-sinusoid-FDSN-Other.json",
                Samples.Type.INTEGER,
                DataEncoding.STEIM2)),
        Arguments.of(
            new ReferenceData(
                "https://miniseed3.readthedocs.io/en/latest/_downloads/29ff65e4d652e86cfdafea6e68cacbfe/reference-sinusoid-FDSN-All.mseed3",
                "https://miniseed3.readthedocs.io/en/latest/_downloads/797c15cdf4c0721aee0476f9fc81c1dd/reference-sinusoid-FDSN-All.json",
                Samples.Type.INTEGER,
                DataEncoding.STEIM2)));
  }

  @ParameterizedTest
  @MethodSource("provideReferenceData")
  public void testReferenceData(ReferenceData referenceData) throws IOException {
    byte[] expectedBytes = DataRecordTestHelper.getBytes(referenceData.miniSeedUrl());
    DataRecord3 read = DataRecord3.read(new ByteArrayInputStream(expectedBytes));
    byte[] actualBytes = read.toByteArray();
    assertThat(actualBytes.length, is(expectedBytes.length));
    for (int i = 0; i < expectedBytes.length; i++) {
      assertThat(
          i
              + " Expected 0x"
              + Integer.toHexString(expectedBytes[i])
              + ", but was 0x"
              + Integer.toHexString(actualBytes[i]),
          actualBytes[i],
          is(expectedBytes[i]));
    }
    assertThat(read.samples().type(), is(referenceData.samplesType()));
    assertThat(read.header().dataPayloadEncoding(), is(referenceData.encoding()));
    String jsonURL = referenceData.jsonUrl();
    assertThat(actualBytes, is(expectedBytes));
    JSONObject expectedJson =
        new JSONArray(DataRecordTestHelper.getString(jsonURL)).getJSONObject(0);
    JSONObject actualJson = read.toJson();
    assertJson(jsonURL.substring(jsonURL.lastIndexOf('/')), expectedJson, actualJson);

    Samples samples = read.samples();
    assertThat(samples, is(notNullValue()));
    assertThat(samples.type(), is(referenceData.samplesType()));
  }

  private void assertJson(String description, JSONObject expected, JSONObject actual) {
    try {
      for (String name : expected.keySet()) {
        assertThat(description, actual.keySet(), hasItem(name));
        Object valueExpected = expected.get(name);
        Object valueActual = actual.get(name);
        if (valueExpected == valueActual) {
          continue;
        }
        String reason = description + ":" + name;
        assertThat(reason, valueExpected, notNullValue());
        if (valueExpected instanceof JSONObject) {
          assertJson(description, (JSONObject) valueExpected, (JSONObject) valueActual);
        } else if (valueExpected instanceof JSONArray expectedArray) {
          JSONArray actualArray = (JSONArray) valueActual;
          assertThat(actualArray.length(), is(expectedArray.length()));
          for (int i = 0; i < expectedArray.length(); i++) {
            if (actualArray.get(i) instanceof JSONObject) {
              assertJson(reason, expectedArray.getJSONObject(i), actualArray.getJSONObject(i));
            } else if (actualArray.get(i) instanceof Number) {
              assertNumber(reason, expectedArray.getNumber(i), actualArray.getNumber(i));
            } else {
              assertThat(reason, actualArray.get(i), is(expectedArray.get(i)));
            }
          }
        } else if (valueExpected instanceof Number) {
          assertNumber(reason, (Number) valueActual, (Number) valueExpected);
        } else {
          assertThat(reason, valueActual, is(valueExpected));
        }
      }
      assertThat(description, actual.keySet(), containsInAnyOrder(expected.keySet().toArray()));
      assertThat(actual.keySet(), hasSize(expected.keySet().size()));
    } catch (RuntimeException exception) {
      fail(exception);
    }
  }

  private void assertNumber(String reason, Number expected, Number actual) {
    if (expected instanceof Integer || expected instanceof Long || expected instanceof Short) {
      assertThat(reason, actual.longValue(), is(expected.longValue()));
    } else if (expected instanceof Double
        || expected instanceof Float
        || expected instanceof BigDecimal) {
      assertThat(reason, actual.doubleValue(), is(expected.doubleValue()));
    } else {
      assertThat(reason, actual, is(expected));
    }
  }
}
