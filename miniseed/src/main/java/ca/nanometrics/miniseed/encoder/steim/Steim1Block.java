package ca.nanometrics.miniseed.encoder.steim;

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

import ca.nanometrics.miniseed.Sample;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class Steim1Block implements SteimBlock {

  public static final int MIN_NUM_FRAMES = 1;
  public static final int MAX_NUM_FRAMES = 63;
  public static final int DEFAULT_NUM_DATA_FRAMES = 7;
  public static final int NUMBER_ELEMENTS_PER_FRAME = 16;
  public static final int NUMBER_BYTES_PER_FRAME = NUMBER_ELEMENTS_PER_FRAME * 4;

  private static final String NO_LAST_SAMPLE_WHEN_NOT_FULL_MESSAGE =
      "%s Cannot get the last sample from a block that is not full (%s%% full)";
  private static final String CANNOT_FORM_BYTE_BLOCK_WHEN_BLOCK_IS_NOT_FULL =
      "%s Cannot form a valid SteimBlock from incomplete data (%s%% full)";

  private static final Predicate<SteimDataFrame> NOT_EMPTY_FRAME = input -> !input.isEmpty();

  private final String m_description;
  private final Steim1DataFrameProvider m_dataFrameProvider;
  private final Deque<SteimDataFrame> m_dataFrames;
  private final int m_maxNumDataFrames;
  private boolean m_isFinished;
  private int m_finalNumSamples;
  private int m_numFramesCreated;
  private byte[] m_finalByteBlock;

  public Steim1Block(
      String description,
      Steim1DataFrameProvider steim1DataFrameProvider,
      Sample lastSample,
      int numDataFrames) {
    m_description = description;
    m_maxNumDataFrames = numDataFrames;
    if (steim1DataFrameProvider == null) {
      throw new IllegalArgumentException("Must provide data frame provider");
    }
    m_dataFrameProvider = steim1DataFrameProvider;
    m_dataFrames = new ArrayDeque<>(numDataFrames);
    m_dataFrames.add(m_dataFrameProvider.getFirstDataFrame(getNextFrameDescription(), lastSample));
    m_isFinished = false;
  }

  public Steim1Block(
      String description, Steim1DataFrameProvider steim1DataFrameProvider, Sample lastSample) {
    this(description, steim1DataFrameProvider, lastSample, DEFAULT_NUM_DATA_FRAMES);
  }

  private String getNextFrameDescription() {
    return m_description + " Frame #" + (++m_numFramesCreated);
  }

  @Override
  public Optional<List<Sample>> addSample(Sample sample) {
    Optional<List<Sample>> overflow = getCurrentFrame().addSample(sample);

    if (isFull()) {
      finish();
      return overflow;
    }

    if (!overflow.isPresent()) {
      return Optional.<List<Sample>>empty();
    }

    Optional<Sample> lastSample = getCurrentFrame().getLastSample();
    if (!lastSample.isPresent()) {
      throw new IllegalStateException(
          "Last sample of current frame was absent, this block has been padded");
    }
    SteimDataFrame newFrame =
        m_dataFrameProvider.getFrame(getNextFrameDescription(), lastSample.get());
    m_dataFrames.add(newFrame);

    for (Sample overflowSample : overflow.get()) {
      addSampleWithNoOverflow(overflowSample);
    }

    return Optional.empty();
  }

  @Override
  public Optional<Sample> getLastSample() throws IllegalStateException {
    if (!isFull()) {
      throw new IllegalStateException(
          String.format(NO_LAST_SAMPLE_WHEN_NOT_FULL_MESSAGE, m_description, getPercentFull()));
    }

    return m_dataFrames.getLast().getLastSample();
  }

  @Override
  public boolean isFull() {
    return m_dataFrames.size() == m_maxNumDataFrames && m_dataFrames.getLast().isFull();
  }

  @Override
  public int getPercentFull() {
    if (isEmpty()) {
      return 0;
    }
    float others = (m_dataFrames.size() - 1) * (100f / m_maxNumDataFrames);
    float lastFrame = (float) m_dataFrames.getLast().getPercentFull() / m_maxNumDataFrames;
    return Math.round(others + lastFrame);
  }

  @Override
  public int getNumSamples() {
    return m_isFinished ? m_finalNumSamples : getCurrentNumSamples();
  }

  @Override
  public byte[] getBytes() throws IllegalStateException {
    if (!isFull()) {
      throw new IllegalStateException(
          String.format(
              CANNOT_FORM_BYTE_BLOCK_WHEN_BLOCK_IS_NOT_FULL, m_description, getPercentFull()));
    }

    if (m_finalByteBlock == null) {
      m_finalByteBlock = buildByteBlock();
    }

    return m_finalByteBlock;
  }

  @Override
  public Optional<Sample> forceComplete() {
    if (isEmpty()) {
      throw new IllegalStateException(String.format("Attempting to pad out an empty block"));
    }
    Optional<Sample> overflow = getCurrentFrame().forceComplete();

    if (isFull()) {
      ((Steim1FirstDataFrame) m_dataFrames.getFirst())
          .setSteimBlockLastSample(getLastNonEmptySample());
      finish();
      return overflow;
    }

    if (overflow.isPresent()) {
      // Adding to a new frame, as last frame was completed, so guaranteed no overflow
      addSample(overflow.get());
      getCurrentFrame().forceComplete();
      ((Steim1FirstDataFrame) m_dataFrames.getFirst())
          .setSteimBlockLastSample(getLastNonEmptySample());
    }

    while (m_dataFrames.size() < m_maxNumDataFrames) {
      m_dataFrames.add(Steim1DataFrameProvider.FILLER_FULL_FRAME_WITH_NO_SAMPLES);
    }
    finish();
    return Optional.<Sample>empty();
  }

  public Sample getLastNonEmptySample() {
    SteimDataFrame lastFrame = getLastNonEmptyFrame();
    SteimWord lastWord = lastFrame.getLastNonEmptyWord();
    return lastWord.getLastNonEmptySample();
  }

  @Override
  public boolean isEmpty() {
    return getCurrentNumSamples() == 0;
  }

  private void finish() {
    if (!m_isFinished) {
      Steim1FirstDataFrame firstDataFrame = (Steim1FirstDataFrame) m_dataFrames.getFirst();
      if (!firstDataFrame.getLastSampleOfSteimBlock().isPresent()) {
        firstDataFrame.setSteimBlockLastSampleAndFinish(getLastNonEmptySample());
      }
      m_finalNumSamples = getCurrentNumSamples();
      m_isFinished = true;
    }
  }

  private int getCurrentNumSamples() {
    int numSamples = 0;
    for (SteimDataFrame frame : m_dataFrames) {
      numSamples += frame.getNumSamples();
    }

    return numSamples;
  }

  private byte[] buildByteBlock() {
    @SuppressWarnings("resource")
    ByteArrayOutputStream outputStream =
        new ByteArrayOutputStream(NUMBER_BYTES_PER_FRAME * m_dataFrames.size());
    try {
      for (SteimDataFrame frame : m_dataFrames) {
        outputStream.write(frame.toByteArray());
      }
    } catch (IOException e) {
      throw new IllegalStateException("Error while getting byte[] from SteimBlock", e);
    }

    return outputStream.toByteArray();
  }

  private SteimDataFrame getCurrentFrame() {
    return m_dataFrames.getLast();
  }

  /**
   * The maximum possible overflow from adding a single sample to a word in Steim 1 encoding is 2
   * samples, so adding after creating a new frame is 'safe'. This precludes the possibility of
   * 1-word frame.
   */
  private void addSampleWithNoOverflow(Sample overflowSample) {
    getCurrentFrame().addSample(overflowSample);
  }

  private SteimDataFrame getLastNonEmptyFrame() {
    if (m_dataFrames.isEmpty() || m_dataFrames.getFirst().isEmpty()) {
      return null;
    }
    SteimDataFrame last = m_dataFrames.getLast();
    if (last.isEmpty()) {
      last = m_dataFrames.stream().filter(NOT_EMPTY_FRAME).reduce((a, b) -> b).get();
    }
    return last;
  }

  @Override
  public String toString() {
    return m_description + " " + getPercentFull() + "% full";
  }
}
