package ca.nanometrics.miniseed.msx.convert;

/*-
 * #%L
 * msx
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
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.io.FileMatchers.anExistingFile;

import ca.nanometrics.miniseed.DataRecord;
import ca.nanometrics.miniseed.DataRecordTestHelper;
import ca.nanometrics.miniseed.DataRecordTestHelper.ReferenceData;
import ca.nanometrics.miniseed.MiniSeed;
import ca.nanometrics.miniseed.Samples;
import ca.nanometrics.miniseed.encoding.DataEncoding;
import ca.nanometrics.miniseed.v2.DataRecord2Test;
import ca.nanometrics.miniseed.v3.DataRecord3;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import picocli.CommandLine;

class MiniSeed2To3Test {

  public static Stream<Arguments> provideReferenceData() {
    return DataRecord2Test.provideReferenceData();
  }

  @ParameterizedTest
  @MethodSource("provideReferenceData")
  public void testReferenceData(ReferenceData referenceData) throws IOException {
    byte[] v2Bytes = DataRecordTestHelper.getBytes(referenceData.miniSeedUrl() + "?raw=true");
    File root = Files.createTempDirectory("miniseedV2").toFile();
    String subdirectoryPath = "subdir1/subdir2";
    Path path = new File(root, subdirectoryPath).toPath();
    Files.createDirectories(path);
    File inputFile = Files.createTempFile(path, "ms2", ".mseed").toFile();
    Files.write(inputFile.toPath(), v2Bytes);
    File outputDirectory = Files.createTempDirectory("miniseedV3").toFile();
    MiniSeed2To3 converter = new MiniSeed2To3();
    CommandLine cmd = new CommandLine(converter);
    cmd.parseArgs("-i", root.getPath(), "-o", outputDirectory.getPath());
    try {
      converter.run();
    } catch (IllegalStateException e) {
      // TODO when converting Little to Big Endian is added, remove this exception catch to properly
      // verify all Little Endian reference data
      assertThat(e.getMessage(), startsWith("Miniseed v3 only supports Big Endian in"));
      assertThat(referenceData.encoding(), is(DataEncoding.STEIM2));
      return;
    }

    File outputFile = new File(new File(outputDirectory, subdirectoryPath), inputFile.getName());
    assertThat(outputFile, is(anExistingFile()));
    Iterator<DataRecord> inputRecords = MiniSeed.stream(inputFile).toList().iterator();
    MiniSeed.stream(outputFile)
        .forEach(
            record -> {
              assertThat(record, isA(DataRecord3.class));
              DataRecord inputRecord = inputRecords.next();
              assertThat(
                  record.header().sourceIdentifier().network(),
                  is(inputRecord.header().sourceIdentifier().network()));
              assertThat(
                  record.header().sourceIdentifier().station(),
                  is(inputRecord.header().sourceIdentifier().station()));
              assertThat(
                  record.header().sourceIdentifier().location(),
                  is(inputRecord.header().sourceIdentifier().location()));
              assertThat(
                  record.header().sourceIdentifier().channel().replaceAll("_", ""),
                  is(inputRecord.header().sourceIdentifier().channel()));

              Samples samples = record.samples();
              switch (referenceData.samplesType()) {
                case INTEGER -> {
                  assertThat(samples.isInt(), is(true));
                  assertThat(samples.intSamples(), is(inputRecord.samples().intSamples()));
                }
                case DOUBLE -> {
                  assertThat(samples.isInt(), is(false));
                  assertThat(samples.doubleSamples(), is(inputRecord.samples().doubleSamples()));
                }
                default -> throw new IllegalArgumentException(
                    "Unexpected value: " + referenceData.samplesType());
              }
            });
  }
}
