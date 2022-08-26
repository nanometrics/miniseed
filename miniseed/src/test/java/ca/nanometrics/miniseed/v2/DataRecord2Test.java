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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import ca.nanometrics.miniseed.DataRecordTestHelper;
import ca.nanometrics.miniseed.DataRecordTestHelper.ReferenceData;
import ca.nanometrics.miniseed.Samples;
import ca.nanometrics.miniseed.encoding.DataEncoding;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DataRecord2Test {
  /** https://github.com/iris-edu/libmseed/tree/main/test/data */
  public static Stream<Arguments> provideReferenceData() {
    return Stream.of(
        Arguments.of(
            new ReferenceData(
                "https://github.com/iris-edu/libmseed/blob/a6e464d87f7ce3e42272d43520e36473d093dee4/test/data/Int16-encoded.mseed",
                Samples.Type.INTEGER,
                DataEncoding.INTEGER_16BIT)),
        Arguments.of(
            new ReferenceData(
                "https://github.com/iris-edu/libmseed/blob/a6e464d87f7ce3e42272d43520e36473d093dee4/test/data/Int32-1024byte.mseed",
                Samples.Type.INTEGER,
                DataEncoding.INTEGER_32BIT)),
        Arguments.of(
            new ReferenceData(
                "https://github.com/iris-edu/libmseed/blob/a6e464d87f7ce3e42272d43520e36473d093dee4/test/data/Steim1-AllDifferences-BE.mseed",
                Samples.Type.INTEGER,
                DataEncoding.STEIM1)),
        Arguments.of(
            new ReferenceData(
                "https://github.com/iris-edu/libmseed/blob/a6e464d87f7ce3e42272d43520e36473d093dee4/test/data/Steim1-AllDifferences-LE.mseed",
                Samples.Type.INTEGER,
                DataEncoding.STEIM1)),
        Arguments.of(
            new ReferenceData(
                "https://github.com/iris-edu/libmseed/blob/a6e464d87f7ce3e42272d43520e36473d093dee4/test/data/Steim2-AllDifferences-BE.mseed",
                Samples.Type.INTEGER,
                DataEncoding.STEIM2)),
        Arguments.of(
            new ReferenceData(
                "https://github.com/iris-edu/libmseed/blob/a6e464d87f7ce3e42272d43520e36473d093dee4/test/data/Steim2-AllDifferences-LE.mseed",
                Samples.Type.INTEGER,
                DataEncoding.STEIM2)));
  }

  @ParameterizedTest
  @MethodSource("provideReferenceData")
  public void testReferenceData(ReferenceData referenceData) throws IOException {
    byte[] expectedBytes = DataRecordTestHelper.getBytes(referenceData.miniSeedUrl() + "?raw=true");
    DataRecord2 read = DataRecord2.read(new ByteArrayInputStream(expectedBytes));
    byte[] actualBytes = read.toByteArray();
    assertThat(actualBytes.length, is(expectedBytes.length));
    for (int i = 0; i < expectedBytes.length; i++) {
      assertThat(
          "byte#"
              + i
              + " Expected 0x"
              + Integer.toHexString(expectedBytes[i])
              + ", but was 0x"
              + Integer.toHexString(actualBytes[i]),
          actualBytes[i],
          is(expectedBytes[i]));
    }
    Samples samples = read.samples();
    assertThat(samples, is(notNullValue()));
    assertThat(samples.type(), is(referenceData.samplesType()));
  }

  @Test
  public void testParseMultipleRecords() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    try (BufferedInputStream inputStream =
        new BufferedInputStream(classLoader.getResourceAsStream("miniseed2-2records.mseed"))) {
      DataRecord2 record1 = DataRecord2.read(inputStream);
      assertThat(record1, is(notNullValue()));
      assertThat(1, is(record1.header().sequenceNumber()));
      DataRecord2 record2 = DataRecord2.read(inputStream);
      assertThat(record2, is(notNullValue()));
      assertThat(2, is(record2.header().sequenceNumber()));
    }
  }
}
