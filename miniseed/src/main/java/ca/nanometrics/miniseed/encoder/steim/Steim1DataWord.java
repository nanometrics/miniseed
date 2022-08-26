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
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public class Steim1DataWord implements SteimWord {

  static final int STEIM_ONE_MAX_DIFFERENCE_WIDTH = 4;

  private static final String INVALID_NUMBER_OF_SAMPLES = "Invalid number of samples: ";
  private static final String NO_FIRST_SAMPLE =
      "No samples have been added to this word, so it has no first sample";
  private static final String NO_LAST_SAMPLE_WHEN_NOT_FULL_MESSAGE =
      "This word is not full, and therefore does not have a last sample. %s";
  private final Deque<Sample> m_samples = new ArrayDeque<>(STEIM_ONE_MAX_DIFFERENCE_WIDTH);
  private final Sample m_initialValue;
  private Steim1WordState m_state = Steim1WordState.INITIAL;
  private Steim1ControlCode m_controlCode = Steim1ControlCode.NO_DATA;
  private final int[] m_differences = new int[STEIM_ONE_MAX_DIFFERENCE_WIDTH];
  private byte[] m_finalByteArray = new byte[STEIM_ONE_MAX_DIFFERENCE_WIDTH];
  private boolean m_isFinished = false;

  public Steim1DataWord(Sample lastSampleFromPreviousWord) {
    if (lastSampleFromPreviousWord == null) {
      throw new IllegalArgumentException("Must provide last sample from previous word");
    }
    m_initialValue = lastSampleFromPreviousWord;
  }

  @Override
  public Optional<List<Sample>> addSample(Sample sample) {
    if (isFull()) {
      finish();
      return Optional.of(Arrays.asList(sample));
    }

    int sampleValue = sample.sample();
    int difference =
        m_state == Steim1WordState.INITIAL
            ? sampleValue - m_initialValue.sample()
            : sampleValue - m_samples.getLast().sample();

    Optional<List<Sample>> returnValue;
    m_state = m_state.nextState(getNumBytesRequiredToRepresent(difference));
    switch (m_state) {
      case OVERFLOW:
        returnValue = overflow(sample);
        setControlCode();
        break;
      case RETURN:
        addSampleToWord(sample);
        setControlCode();
        returnValue = Optional.of(Collections.<Sample>emptyList());
        break;
      default:
        addSampleToWord(sample);
        returnValue = Optional.empty();
        break;
    }

    if (isFull()) {
      finish();
    }
    return returnValue;
  }

  private void finish() {
    if (!m_isFinished) {
      m_finalByteArray = getCurrentByteArray();
      m_isFinished = true;
    }
  }

  @Override
  public boolean isFull() {
    return m_state == Steim1WordState.RETURN || m_state == Steim1WordState.OVERFLOW;
  }

  @Override
  public boolean isEmpty() {
    return m_samples.isEmpty();
  }

  @Override
  public Optional<Sample> getLastSample() {
    if (!isFull()) {
      throw new IllegalStateException(String.format(NO_LAST_SAMPLE_WHEN_NOT_FULL_MESSAGE, this));
    }

    return Optional.of(m_samples.getLast());
  }

  @Override
  public Sample getLastNonEmptySample() {
    return m_samples.getLast();
  }

  @Override
  public int getNumSamples() {
    return m_samples.size();
  }

  @Override
  public byte[] toByteArray() {
    return m_isFinished ? m_finalByteArray : getCurrentByteArray();
  }

  private byte[] getCurrentByteArray() {
    ByteBuffer byteBuffer = ByteBuffer.allocate(STEIM_ONE_MAX_DIFFERENCE_WIDTH);
    switch (m_controlCode) {
      case NO_DATA:
        return new byte[STEIM_ONE_MAX_DIFFERENCE_WIDTH];
      case ONE_BYTE_DATA:
        for (int i = 0;
            i < STEIM_ONE_MAX_DIFFERENCE_WIDTH / m_controlCode.getNumBytesPerDifference();
            i++) {
          byteBuffer.put((byte) m_differences[i]);
        }
        return byteBuffer.array();
      case TWO_BYTE_DATA:
        for (int i = 0;
            i < STEIM_ONE_MAX_DIFFERENCE_WIDTH / m_controlCode.getNumBytesPerDifference();
            i++) {
          byteBuffer.putShort((short) m_differences[i]);
        }
        return byteBuffer.array();
      case FOUR_BYTE_DATA:
        for (int i = 0;
            i < STEIM_ONE_MAX_DIFFERENCE_WIDTH / m_controlCode.getNumBytesPerDifference();
            i++) {
          byteBuffer.putInt(m_differences[i]);
        }
        return byteBuffer.array();
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  public byte getTwoBitNibbleCode() {
    return m_controlCode.getTwoBitControlCodeValue();
  }

  @Override
  public Sample getFirstSample() {
    if (m_samples.isEmpty()) {
      throw new IllegalStateException(NO_FIRST_SAMPLE);
    }

    return m_samples.getFirst();
  }

  @Override
  public Optional<Sample> forceComplete() {
    Optional<Sample> overflow = Optional.empty();

    switch (m_samples.size()) {
      case 0:
        m_controlCode = Steim1ControlCode.NO_DATA;
        break;
      case 1:
        m_controlCode = Steim1ControlCode.FOUR_BYTE_DATA;
        break;
      case 2:
        m_controlCode = Steim1ControlCode.TWO_BYTE_DATA;
        break;
      case 3:
        m_controlCode = Steim1ControlCode.TWO_BYTE_DATA;
        overflow = Optional.of(m_samples.removeLast());
        break;
      default:
        throw new IllegalStateException(INVALID_NUMBER_OF_SAMPLES + m_samples.size());
    }

    m_state = Steim1WordState.RETURN;
    finish();

    return overflow;
  }

  private static int getNumBytesRequiredToRepresent(long number) {
    int numBits;
    if (number >= Byte.MIN_VALUE && number <= Byte.MAX_VALUE) {
      numBits = Byte.SIZE;
    } else if (number >= Short.MIN_VALUE && number <= Short.MAX_VALUE) {
      numBits = Short.SIZE;
    } else if (number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE) {
      numBits = Integer.SIZE;
    } else {
      numBits = Long.SIZE;
    }
    return numBits / 8;
  }

  /**
   * Only used when adding the sample will not fill the word nor overflow it
   *
   * @param sample
   * @param numRequiredBytes
   */
  private void addSampleToWord(Sample sample) {
    if (m_samples.isEmpty()) {
      m_differences[0] = sample.sample() - m_initialValue.sample();
    } else {
      m_differences[m_samples.size()] = sample.sample() - m_samples.getLast().sample();
    }
    m_samples.add(sample);
  }

  /**
   * Only used when adding the sample will cause the word to overflow
   *
   * @param sample
   * @param numRequiredBytes
   * @return
   */
  private Optional<List<Sample>> overflow(Sample sample) {
    switch (m_samples.size()) {
      case 1:
        return Optional.of(Arrays.asList(sample));
      case 2:
        return Optional.of(Arrays.asList(sample));
      case 3:
        Sample sampleOverflow = m_samples.removeLast();
        return Optional.of(Arrays.asList(sampleOverflow, sample));
      default:
        throw new IllegalStateException(INVALID_NUMBER_OF_SAMPLES + m_samples.size());
    }
  }

  private void setControlCode() {
    switch (m_samples.size()) {
      case 1:
        m_controlCode = Steim1ControlCode.FOUR_BYTE_DATA;
        break;
      case 2:
        m_controlCode = Steim1ControlCode.TWO_BYTE_DATA;
        break;
      case 4:
        m_controlCode = Steim1ControlCode.ONE_BYTE_DATA;
        break;
      default:
        throw new IllegalStateException(INVALID_NUMBER_OF_SAMPLES + m_samples.size());
    }
  }

  @Override
  public String toString() {
    return "Steim1DataWord [m_samples="
        + m_samples
        + ", m_initialValue="
        + m_initialValue
        + ", m_state="
        + m_state
        + ", m_controlCode="
        + m_controlCode
        + ", m_differences="
        + Arrays.toString(m_differences)
        + ", m_finalByteArray="
        + Arrays.toString(m_finalByteArray)
        + ", m_isFinished="
        + m_isFinished
        + "]";
  }
}
