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

import ca.nanometrics.miniseed.DataRecord;
import ca.nanometrics.miniseed.MiniSeed;
import ca.nanometrics.miniseed.Sample;
import ca.nanometrics.miniseed.Samples;
import ca.nanometrics.miniseed.encoder.steim.Steim1Block;
import ca.nanometrics.miniseed.encoder.steim.Steim1BlockProvider;
import ca.nanometrics.miniseed.encoder.steim.Steim1Encoder;
import ca.nanometrics.miniseed.encoder.steim.SteimBlockObserver;
import ca.nanometrics.miniseed.encoding.DataEncoding;
import ca.nanometrics.miniseed.endian.Endian;
import ca.nanometrics.miniseed.v2.DataRecord2;
import ca.nanometrics.miniseed.v2.DataRecord2Header;
import ca.nanometrics.miniseed.v2.DataRecord2Header.QualityIndicator;
import ca.nanometrics.miniseed.v2.Scnl;
import ca.nanometrics.miniseed.v3.DataRecord3;
import ca.nanometrics.miniseed.v3.DataRecord3Header;
import ca.nanometrics.miniseed.v3.DataRecord3Header.Flags;
import ca.nanometrics.miniseed.v3.DataRecord3Header.Float64SampleRate;
import ca.nanometrics.miniseed.v3.ExtraHeader;
import ca.nanometrics.miniseed.v3.UriSourceIdentifier;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongConsumer;
import java.util.stream.Stream;
import me.tongfei.progressbar.ProgressBar;
import org.json.JSONObject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "2to3",
    description = {"Convert miniseed files from version 2.4 to 3.%n"},
    optionListHeading = "Options:%n",
    mixinStandardHelpOptions = true,
    sortOptions = false)
public class MiniSeed2To3 implements Runnable {
  static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MiniSeed2To3.class);

  @Option(
      paramLabel = "input",
      names = {"-i", "--input"},
      required = true,
      description = "Input directory or file(s)")
  private List<File> files;

  @Option(
      paramLabel = "output",
      names = {"-o", "--output"},
      required = true,
      description = "Output directory")
  private File outputDirectory;

  @Option(
      names = {"-g", "--progress"},
      description = "Show progress of the conversion for each file.")
  private boolean showProgress;

  @Override
  public void run() {
    createDirectories(outputDirectory);
    for (File file : files) {
      if (file.isFile()) {

        convertFile(file, new File(outputDirectory, file.getName()));
      } else if (file.isDirectory()) {
        try {
          Files.walk(file.toPath())
              .filter(Files::isRegularFile)
              .map(Path::toFile)
              .forEach(
                  f -> {
                    File directory =
                        new File(
                            outputDirectory,
                            file.toPath()
                                .relativize(f.getParentFile().toPath())
                                .toFile()
                                .toString());
                    createDirectories(directory);
                    convertFile(f, new File(directory, f.getName()));
                  });
        } catch (IOException e) {
          LOG.error("Could not search for files in " + file, e);
        }
      } else {
        LOG.error("Unknown file type: {}", file);
      }
    }
  }

  private void createDirectories(File file) {
    try {
      Files.createDirectories(file.toPath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void convertFile(File input, File output) {
    AtomicInteger numDataRecords = new AtomicInteger(0);
    AtomicInteger totalBytes = new AtomicInteger(0);

    LOG.info("Processing file {} to {}", input, output);

    try (Progress progressBar = getProgressBar(input)) {
      MiniSeed.stream(input)
          .forEach(
              record -> {
                DataRecord2 v2Record = (DataRecord2) record;
                DataRecord2Header v2Header = v2Record.header();
                Scnl scnl = v2Header.sourceIdentifier();
                DataEncoding dataEncoding = v2Header.dataOnlyBlockette().encodingFormat();
                byte[] payload = v2Record.payload();

                if (v2Header.endian().type() == Endian.Type.LITTLE) {
                  if (dataEncoding == DataEncoding.STEIM1) {
                    payload = recodeToSteim1BigEndian(record);
                  } else if (dataEncoding == DataEncoding.STEIM2) {
                    // TODO: support either converting directly from Little to Big Endian in place,
                    // or decoding and re-encoding the samples (as with Steim1 above)
                    throw new IllegalStateException(
                        String.format(
                            "Miniseed v3 only supports Big Endian in %s, but cannot convert from"
                                + " Little Endian to Big Endian here yet.",
                            dataEncoding));
                  }
                }

                DataRecord3Header v3Header =
                    DataRecord3Header.builder()
                        .sourceIdentifier(
                            UriSourceIdentifier.builder()
                                .network(scnl.network())
                                .station(scnl.station())
                                .location(scnl.location())
                                .channel(scnl.channel())
                                .build())
                        .recordStartTime(v2Header.recordStartTime())
                        .numberOfSamples(v2Header.numberOfSamples())
                        .sampleRate(new Float64SampleRate(v2Header.sampleRate().sampleRateDouble()))
                        .dataPayloadEncoding(dataEncoding)
                        .lengthOfDataPayload(payload.length)
                        .dataPublicationVersion(
                            toDataPublicationVersion(v2Header.qualityIndicator()))
                        .flags(toFlags(v2Header))
                        .extraHeaderFields(toExtraHeaders(v2Header))
                        .build();

                DataRecord3 v3Record =
                    DataRecord3.builder().header(v3Header).payload(payload).build();
                try {
                  if (!output.exists()) {
                    Files.createFile(output.toPath());
                  }
                  Files.write(output.toPath(), v3Record.toByteArray(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
                progressBar.accept(v2Record.length());
                totalBytes.addAndGet(v2Record.length());
                numDataRecords.incrementAndGet();
              });
    } catch (IOException e) {
      LOG.info("Error occurred while processing file {}: {}", input, e);
      e.printStackTrace();
    }

    LOG.info(
        "{} Read {} data record{} ({} bytes)",
        input,
        numDataRecords.get(),
        (numDataRecords.get() != 1 ? "s" : ""),
        totalBytes);
  }

  private byte[] recodeToSteim1BigEndian(DataRecord record) {
    Samples samples = record.samples();
    if (samples.type() != Samples.Type.INTEGER) {
      throw new IllegalArgumentException(
          String.format("Cannot convert %s samples to steim", samples.type()));
    }
    List<byte[]> result = new ArrayList<>(1);
    SteimBlockObserver observer = block -> result.add(block.getBytes());
    Collection<SteimBlockObserver> observers = Set.of(observer);
    int framesPerBlock = record.payload().length / Steim1Block.NUMBER_BYTES_PER_FRAME;
    Steim1Encoder encoder =
        new Steim1Encoder(
            "convert to big endian", new Steim1BlockProvider(framesPerBlock), observers);
    for (int sample : samples.intSamples()) {
      encoder.addSample(new Sample(sample));
    }
    encoder.flush(null);
    if (result.isEmpty()) {
      throw new IllegalStateException(
          "Failed to convert Steim1 data from little endian to big endian");
    }
    if (result.size() != 1) {
      throw new IllegalStateException(
          "Big endian and Little endiand data should be the same size.");
    }
    return result.get(0);
  }

  private Flags toFlags(DataRecord2Header v2header) {
    return Flags.builder()
        .calibrationSignalPresent(v2header.activityFlags().calibrationSignalsPresent())
        .timeTagIsQuestionable(v2header.dataQualityFlags().timeTagIsQuestionable())
        .clockLocked(v2header.ioFlags().clockLocked())
        .build();
  }

  private static short toDataPublicationVersion(QualityIndicator qualityIndicator) {
    return switch (qualityIndicator) {
      case RAW -> (short) 1;
      case UNKNOWN -> (short) 2;
      case QUALITY_CONTROLLED -> (short) 3;
      case MODIFIED -> (short) 4;
    };
  }

  private static JSONObject toExtraHeaders(DataRecord2Header v2header) {
    JSONObject extraHeaders = new JSONObject();
    for (ExtraHeader<?> header : ExtraHeader.FDSN_RESERVED_HEADERS) {
      header.add(v2header, extraHeaders);
    }
    return extraHeaders;
  }

  /**
   * @return the file, or expand the directory to a list of files.
   */
  static Stream<File> getInputFiles(File file) {
    if (file.isFile()) {
      return Stream.of(file);
    }
    if (file.isDirectory()) {
      try {
        return Files.walk(file.toPath()).filter(Files::isRegularFile).map(Path::toFile);
      } catch (IOException e) {
        LOG.error("Could not search for files in " + file, e);
        return Stream.empty();
      }
    }
    LOG.error("Unknown file type: {}", file);
    return Stream.empty();
  }

  private Progress getProgressBar(File file) {
    if (showProgress) {
      ProgressBar progressBar = new ProgressBar(file.getName(), file.length());
      return new Progress() {

        @Override
        public void close() throws IOException {
          progressBar.stepTo(progressBar.getMax()).close();
        }

        @SuppressWarnings("resource")
        @Override
        public void accept(long value) {
          progressBar.stepBy(value);
        }
      };
    }
    return new Progress() {

      @Override
      public void close() throws IOException {
        // do nothing
      }

      @Override
      public void accept(long value) {
        // do nothing
      }
    };
  }

  interface Progress extends Closeable, LongConsumer {
    // aggregate interface
  }
}
