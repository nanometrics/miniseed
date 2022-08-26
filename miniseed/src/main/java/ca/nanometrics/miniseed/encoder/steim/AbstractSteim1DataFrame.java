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
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public abstract class AbstractSteim1DataFrame implements SteimDataFrame {

  static final int NUMBER_ELEMENTS_PER_FRAME = 16;
  static final int NUMBER_BYTES_PER_FRAME = NUMBER_ELEMENTS_PER_FRAME * 4;

  // CHECKSTYLE:OFF long line
  private static final String INVALID_NUMBER_OF_TOTAL_WORDS =
      "%s Attempting to create a Steim 1 frame with %d data words and %d non data words, which is"
          + " incompatible with the expected total of %d words";
  static final String CANNOT_FORM_BYTE_ARRAY_WHEN_FRAME_IS_NOT_FULL =
      "%s Cannot form a valid byte array from incomplete data (%s% full)";
  private static final String HAS_NO_DATA = "%s has no data";

  private static final Predicate<SteimWord> NOT_EMPTY_WORD = input -> !input.isEmpty();

  private final String m_description;
  private final Deque<SteimWord> m_dataWords;
  private final int m_maxNumDataWords;
  private final int m_numNonDataWords;
  private final Steim1WordProvider m_steimWordProvider;
  private boolean m_isFinished = false;
  private int m_finalNumSamples;
  private byte[] m_finalByteArray;

  public AbstractSteim1DataFrame(
      String description,
      Steim1WordProvider wordProvider,
      Sample lastSample,
      int numDataWords,
      int numNonDataWords) {
    m_description = description;
    if (!(numDataWords >= 0
        && numNonDataWords >= 0
        && numDataWords + numNonDataWords == NUMBER_ELEMENTS_PER_FRAME)) {
      throw new IllegalArgumentException(
          String.format(
              description,
              INVALID_NUMBER_OF_TOTAL_WORDS,
              numDataWords,
              numNonDataWords,
              NUMBER_ELEMENTS_PER_FRAME));
    }
    m_steimWordProvider = wordProvider;
    m_dataWords = new ArrayDeque<>(numDataWords);
    m_dataWords.add(m_steimWordProvider.getWord(lastSample));
    m_maxNumDataWords = numDataWords;
    m_numNonDataWords = numNonDataWords;
  }

  public abstract byte[] getAsByteArray();

  @Override
  public Optional<List<Sample>> addSample(Sample sample) {
    Optional<List<Sample>> overflow = getCurrentWord().addSample(sample);

    if (isFull()) {
      blockFilled();
      return overflow.isPresent() ? overflow : Optional.of(Collections.<Sample>emptyList());
    }

    if (!overflow.isPresent()) {
      return Optional.<List<Sample>>empty();
    }

    Optional<Sample> lastSample = getCurrentWord().getLastSample();
    if (!lastSample.isPresent()) {
      throw new IllegalStateException(
          "Last sample of current word was absent, this frame has been padded");
    }
    SteimWord newWord = m_steimWordProvider.getWord(lastSample.get());
    m_dataWords.add(newWord);

    List<Sample> overflowSamples = overflow.get();

    if (overflowSamples.size() == 0) {
      return Optional.<List<Sample>>empty();
    } else if (overflowSamples.size() == 1) {
      // Guaranteed to not overflow, so this is effectively returning empty or absent
      return addSample(overflowSamples.get(0));
    } else if (overflowSamples.size() == 2) {
      // Guaranteed to not overflow, so return value rolls into next call
      addSample(overflowSamples.get(0));
      // Not guaranteed to not overflow
      return addSample(overflowSamples.get(1));
    }

    return Optional.empty();
  }

  void blockFilled() {
    finish();
  }

  @Override
  public boolean isFull() {
    return m_dataWords.size() == m_maxNumDataWords && m_dataWords.getLast().isFull();
  }

  @Override
  public boolean isEmpty() {
    return m_dataWords.isEmpty() || m_dataWords.getFirst().isEmpty();
  }

  @Override
  public int getPercentFull() {
    if (isFull()) {
      return 100;
    }
    if (m_dataWords.isEmpty()) {
      return 0;
    }
    return Math.round(100f * (m_dataWords.size() - 1) / m_maxNumDataWords);
  }

  @Override
  public Optional<Sample> getLastSample() throws IllegalStateException {
    if (!isFull()) {
      throw new IllegalStateException(
          String.format(
              "%s Cannot get the last sample from a frame that is not full (%s%% full)",
              m_description, getPercentFull()));
    }
    return m_dataWords.getLast().getLastSample();
  }

  @Override
  public int getNumSamples() {
    return m_isFinished ? m_finalNumSamples : getCurrentNumSamples();
  }

  @Override
  public byte[] toByteArray() throws IllegalStateException {
    if (!isFull()) {
      throw new IllegalStateException(
          String.format(
              CANNOT_FORM_BYTE_ARRAY_WHEN_FRAME_IS_NOT_FULL, m_description, getPercentFull()));
    }

    if (m_finalByteArray == null) {
      m_finalByteArray = getAsByteArray();
    }
    return m_finalByteArray;
  }

  @Override
  public Optional<Sample> forceComplete() {
    Optional<Sample> overflow = getCurrentWord().forceComplete();

    if (isFull()) {
      finish();
      return overflow;
    }

    if (overflow.isPresent()) {
      addSample(overflow.get());
      getCurrentWord().forceComplete();
    }

    while (!isFull()) {
      m_dataWords.add(Steim1WordProvider.EMPTY_STEIM_WORD);
    }

    finish();
    return Optional.<Sample>empty();
  }

  @Override
  public SteimWord getLastNonEmptyWord() {
    if (isEmpty()) {
      throw new IllegalStateException(String.format(HAS_NO_DATA, m_description));
    }
    SteimWord last = m_dataWords.getLast();
    if (last.isEmpty()) {
      last = m_dataWords.stream().filter(NOT_EMPTY_WORD).reduce((a, b) -> b).get();
    }
    return last;
  }

  public byte[] getBytesForControlCodes() throws IllegalStateException {
    if (!isFull()) {
      throw new IllegalStateException(
          String.format(
              CANNOT_FORM_BYTE_ARRAY_WHEN_FRAME_IS_NOT_FULL, m_description, getPercentFull()));
    }

    byte[] codeBitsAsBytes = new byte[4];

    int indexOfDataWord = m_numNonDataWords;
    for (SteimWord dataWord : m_dataWords) {
      byte twoBitNibble = dataWord.getTwoBitNibbleCode();
      codeBitsAsBytes[indexOfDataWord / 4] |= twoBitNibble << (2 * (3 - (indexOfDataWord % 4)));
      indexOfDataWord++;
    }

    return codeBitsAsBytes;
  }

  public byte[] getDataAsByteArray() {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(NUMBER_BYTES_PER_FRAME);
    try {
      for (SteimWord word : m_dataWords) {
        outputStream.write(word.toByteArray());
      }
    } catch (IOException e) {
      throw new IllegalStateException(
          String.format("{} Error while getting byte array from Steim Frame", m_description), e);
    }
    return outputStream.toByteArray();
  }

  public Sample getFirstSample() {
    return m_dataWords.getFirst().getFirstSample();
  }

  protected void finish() {
    if (!m_isFinished) {
      m_isFinished = true;
      m_finalNumSamples = getCurrentNumSamples();
    }
  }

  private int getCurrentNumSamples() {
    int numSamples = 0;
    for (SteimWord word : m_dataWords) {
      numSamples += word.getNumSamples();
    }

    return numSamples;
  }

  private SteimWord getCurrentWord() {
    return m_dataWords.getLast();
  }

  @Override
  public String toString() {
    return m_description + " " + getPercentFull() + "% full";
  }
}
