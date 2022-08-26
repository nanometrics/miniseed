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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.nanometrics.miniseed.Sample;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.OngoingStubbing;

public class Steim1DataFrameTest {
  private static final String DESCRIPTION = "testframe";
  private static final Sample INITIALIZING_SAMPLE = new Sample(10); // Arbitrary value

  @Test
  public void testAddingSampleThatFillsCurrentWordButDoesNotFillFrameReturnsOptionalempty() {
    Sample[] samples = SteimTestHelper.getSamplesWithFourByteDifferences(2);

    Steim1DataFrame dataFrame =
        new Steim1DataFrame(DESCRIPTION, new Steim1WordProvider(), samples[0]);
    assertThat(dataFrame.addSample(samples[1]).isPresent(), is(false));
  }

  @Test
  public void testAddingSampleThatDoesNotFillCurrentWordOrFrameReturnsempty() {
    Sample sample = new Sample(1);

    Steim1DataFrame dataFrame =
        new Steim1DataFrame(DESCRIPTION, new Steim1WordProvider(), INITIALIZING_SAMPLE);
    assertThat(dataFrame.addSample(sample).isPresent(), is(false));
  }

  @Test
  public void testAddingSampleThatFillsFrameExactlyReturnsEmptyList() {
    Sample[] differences =
        SteimTestHelper.getSamplesWithFourByteDifferences(Steim1DataFrame.NUM_DATA_WORDS + 1);
    int currentSample = 0;
    Steim1DataFrame dataFrame =
        new Steim1DataFrame(DESCRIPTION, new Steim1WordProvider(), differences[currentSample++]);
    for (int i = 0; i < Steim1DataFrame.NUM_DATA_WORDS - 1; i++) {
      dataFrame.addSample(differences[currentSample++]);
    }
    Optional<List<Sample>> overflow = dataFrame.addSample(differences[currentSample++]);
    assertThat(overflow.isPresent(), is(true));
    assertThat(overflow.get().isEmpty(), is(true));
  }

  @Test
  public void testAddingSampleThatFillsAndOverflowsFrameReturnsListOfOverflowValues() {
    int maxNumOneByteDifferences =
        Steim1DataFrame.NUM_DATA_WORDS * Steim1DataWord.STEIM_ONE_MAX_DIFFERENCE_WIDTH;
    Sample[] samplesWithOneByteDifferences =
        SteimTestHelper.getSamplesWithOneByteDifferences(maxNumOneByteDifferences);
    int currentSample = 0;
    Steim1DataFrame dataFrame =
        new Steim1DataFrame(
            DESCRIPTION, new Steim1WordProvider(), samplesWithOneByteDifferences[currentSample++]);

    for (int i = 0; i < maxNumOneByteDifferences - 2; i++) {
      dataFrame.addSample(samplesWithOneByteDifferences[currentSample++]);
    }

    // Frame is now loaded so it is full except for the last word, which has 2 1-byte differences in
    // it
    Sample lastOneByteSample = samplesWithOneByteDifferences[currentSample];
    Sample sampleWithFourByteDifference =
        SteimTestHelper.getSampleWithFourByteDifferenceFrom(lastOneByteSample);

    dataFrame.addSample(lastOneByteSample);
    Optional<List<Sample>> overflow = dataFrame.addSample(sampleWithFourByteDifference);

    assertThat(overflow.isPresent(), is(true));
    assertThat(overflow.get().size(), is(2));
    assertThat(overflow.get().get(0), is(lastOneByteSample));
    assertThat(overflow.get().get(1), is(sampleWithFourByteDifference));
  }

  @Test
  public void testFrameThatDoesNotHaveEnoughWordsIsNotFull() {
    Steim1DataFrame dataFrame =
        new Steim1DataFrame(DESCRIPTION, new Steim1WordProvider(), INITIALIZING_SAMPLE);
    assertThat(dataFrame.isFull(), is(false));
  }

  @Test
  public void testFrameThatHasMaxNumWordsButLastWordIsNotFullIsNotFull() {
    Sample[] samplesWithFourBytesDifference =
        SteimTestHelper.getSamplesWithFourByteDifferences(Steim1DataFrame.NUM_DATA_WORDS + 1);
    int currentSample = 0;
    Steim1DataFrame dataFrame =
        new Steim1DataFrame(
            DESCRIPTION, new Steim1WordProvider(), samplesWithFourBytesDifference[currentSample++]);
    for (int i = 0; i < Steim1DataFrame.NUM_DATA_WORDS - 1; i++) {
      dataFrame.addSample(samplesWithFourBytesDifference[currentSample++]);
    }
    Sample oneByteDifferenceSample =
        SteimTestHelper.getSampleWithOneByteDifferenceFrom(
            samplesWithFourBytesDifference[currentSample - 1]);

    dataFrame.addSample(oneByteDifferenceSample);
    assertThat(dataFrame.isFull(), is(false));
  }

  @Test
  public void testFrameThatIsFilledWithFullWordsIsFull() {
    Sample[] differences =
        SteimTestHelper.getSamplesWithFourByteDifferences(Steim1DataFrame.NUM_DATA_WORDS + 1);
    int currentSample = 0;
    Steim1DataFrame dataFrame =
        new Steim1DataFrame(DESCRIPTION, new Steim1WordProvider(), differences[currentSample++]);
    for (int i = 0; i < Steim1DataFrame.NUM_DATA_WORDS; i++) {
      dataFrame.addSample(differences[currentSample++]);
    }
    assertThat(dataFrame.isFull(), is(true));
  }

  @Test
  public void testGetLastSampleOnFullFrameReturnsLastSample() {
    Sample[] differences =
        SteimTestHelper.getSamplesWithFourByteDifferences(Steim1DataFrame.NUM_DATA_WORDS + 1);
    int currentSample = 0;
    Steim1DataFrame dataFrame =
        new Steim1DataFrame(DESCRIPTION, new Steim1WordProvider(), differences[currentSample++]);
    for (int i = 0; i < Steim1DataFrame.NUM_DATA_WORDS - 1; i++) {
      dataFrame.addSample(differences[currentSample++]);
    }

    Sample lastSample = differences[currentSample];
    dataFrame.addSample(lastSample);
    assertThat(dataFrame.isFull(), is(true));
    assertThat(dataFrame.getLastSample().get(), is(lastSample));
  }

  @Test
  public void testGetLastSampleOnNotFullFrameThrowsException() {
    assertThrows(
        IllegalStateException.class,
        () -> {
          Steim1DataFrame dataFrame =
              new Steim1DataFrame(DESCRIPTION, new Steim1WordProvider(), INITIALIZING_SAMPLE);
          assertThat(dataFrame.isFull(), is(false));
          dataFrame.getLastSample();
        });
  }

  @Test
  public void testByteArrayIsProperlyFormed() {
    SteimWord[] steimWords = new SteimWord[AbstractSteim1DataFrame.NUMBER_ELEMENTS_PER_FRAME];
    Sample[] samplesAtTimes = new Sample[AbstractSteim1DataFrame.NUMBER_ELEMENTS_PER_FRAME - 1];
    byte[] nibbleCodes = new byte[] {1, 2, 3};

    for (int i = 0; i < AbstractSteim1DataFrame.NUMBER_ELEMENTS_PER_FRAME - 1; i++) {
      samplesAtTimes[i] = new Sample(Integer.MAX_VALUE / (i + 1));
    }

    for (int i = 0; i < AbstractSteim1DataFrame.NUMBER_ELEMENTS_PER_FRAME - 1; i++) {
      steimWords[i] = mock(SteimWord.class);
      when(steimWords[i].addSample(samplesAtTimes[i]))
          .thenReturn(Optional.of(Collections.<Sample>emptyList()));
      Optional<Sample> lastSample = Optional.of(samplesAtTimes[i]);
      when(steimWords[i].getLastSample()).thenAnswer(inv -> lastSample);
      when(steimWords[i].getNumSamples()).thenReturn(1);
      when(steimWords[i].isFull()).thenAnswer(inv -> true);
      byte twoBitNibbleCode = nibbleCodes[i % 3];
      when(steimWords[i].getTwoBitNibbleCode()).thenAnswer(inv -> twoBitNibbleCode);
      int sample = samplesAtTimes[i].sample();
      when(steimWords[i].toByteArray())
          .thenAnswer(
              inv ->
                  ByteBuffer.allocate(Steim1DataWord.STEIM_ONE_MAX_DIFFERENCE_WIDTH)
                      .putInt(sample)
                      .array());
    }

    Steim1WordProvider wordProvider = mock(Steim1WordProvider.class);
    OngoingStubbing<SteimWord> when = when(wordProvider.getWord(isA(Sample.class)));
    for (int i = 0; i < AbstractSteim1DataFrame.NUMBER_ELEMENTS_PER_FRAME - 1; i++) {
      when = when.thenReturn(steimWords[i]);
    }

    Steim1DataFrame dataFrame = new Steim1DataFrame(DESCRIPTION, wordProvider, INITIALIZING_SAMPLE);
    for (int i = 0; i < AbstractSteim1DataFrame.NUMBER_ELEMENTS_PER_FRAME - 1; i++) {
      dataFrame.addSample(samplesAtTimes[i]);
    }
    byte[] byteArray = dataFrame.getAsByteArray();
    assertThat(byteArray[0], is((byte) 0b00011011));
    assertThat(byteArray[1], is((byte) 0b01101101));
    assertThat(byteArray[2], is((byte) 0b10110110));
    assertThat(byteArray[3], is((byte) 0b11011011));
    for (int i = 0; i < AbstractSteim1DataFrame.NUMBER_ELEMENTS_PER_FRAME - 1; i++) {
      assertThat(
          ByteBuffer.wrap(
                  Arrays.copyOfRange(
                      byteArray,
                      (i + 1) * Steim1DataWord.STEIM_ONE_MAX_DIFFERENCE_WIDTH,
                      (i + 2) * Steim1DataWord.STEIM_ONE_MAX_DIFFERENCE_WIDTH))
              .getInt(),
          is(samplesAtTimes[i].sample()));
    }
  }

  @Test
  public void testForceCompleteWithNoOverflowReturnsOptionalempty() {
    Steim1DataFrame dataFrame =
        new Steim1DataFrame(DESCRIPTION, new Steim1WordProvider(), INITIALIZING_SAMPLE);
    assertThat(dataFrame.forceComplete(), is(Optional.<Sample>empty()));
  }

  @Test
  public void testForceCompleteWithOverflowReturnsOverflow() {
    int maxNumOneByteDifferences =
        Steim1DataFrame.NUM_DATA_WORDS * Steim1DataWord.STEIM_ONE_MAX_DIFFERENCE_WIDTH;
    Sample[] samplesWithOneByteDifferences =
        SteimTestHelper.getSamplesWithOneByteDifferences(maxNumOneByteDifferences);
    int currentSample = 0;
    Steim1DataFrame dataFrame =
        new Steim1DataFrame(
            DESCRIPTION, new Steim1WordProvider(), samplesWithOneByteDifferences[currentSample++]);

    for (int i = 0; i < maxNumOneByteDifferences - 2; i++) {
      dataFrame.addSample(samplesWithOneByteDifferences[currentSample++]);
    }

    // Frame is now loaded so it is full except for the last word, which has 2 1-byte differences in
    // it
    Sample lastOneByteSample = samplesWithOneByteDifferences[currentSample];
    dataFrame.addSample(lastOneByteSample);
    assertThat(dataFrame.forceComplete(), is(Optional.of(lastOneByteSample)));
  }

  @Test
  public void testForceCompleteFillsFrame() {
    Steim1DataFrame dataFrame =
        new Steim1DataFrame(DESCRIPTION, new Steim1WordProvider(), INITIALIZING_SAMPLE);
    dataFrame.forceComplete();
    assertThat(dataFrame.isFull(), is(true));
  }

  @Test
  public void testOverflowingWordsAreForceCompleted_CENT_998() {
    Sample sample = new Sample(0);
    Steim1DataFrame dataFrame = new Steim1DataFrame(DESCRIPTION, new Steim1WordProvider(), sample);

    // This loads the dataframe so that the first word has 3 one byte differences (0) in it
    for (int i = 0; i < 3; i++) {
      dataFrame.addSample(sample);
    }

    dataFrame.forceComplete();
    assertThat(dataFrame.getNumSamples(), is(3));
    assertThat(
        dataFrame.getLastNonEmptyWord().getTwoBitNibbleCode(),
        is(Steim1ControlCode.FOUR_BYTE_DATA.getTwoBitControlCodeValue()));
  }
}
