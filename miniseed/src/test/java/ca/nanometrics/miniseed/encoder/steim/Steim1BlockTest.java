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
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.nanometrics.miniseed.Sample;
import ca.nanometrics.miniseed.Samples;
import ca.nanometrics.miniseed.encoding.steim.DecodeSteim1;
import ca.nanometrics.miniseed.endian.BigEndianReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class Steim1BlockTest {
  private static final String DESCRIPTION = "testblock";
  private static final Sample INITIALIZING_LAST_SAMPLE = new Sample(0); // Arbitrary number
  private static final Sample SAMPLE = new Sample(1); // Distinct arbitrary number

  @Test
  public void testAddSampleToBlockThatIsNotFullButDoesNotCompleteBlockReturnsOptionalEmpty() {
    Steim1Block steimBlock = new Steim1Block(DESCRIPTION, new Steim1DataFrameProvider(), SAMPLE);
    Optional<List<Sample>> returnValue = steimBlock.addSample(new Sample(3));
    assertThat(returnValue.isPresent(), is(false));
  }

  @Test
  public void testCompleting1FrameBlockWithNoOverflowReturnsEmptyList() {
    int numWordsInBlock = Steim1FirstDataFrame.NUM_DATA_WORDS;
    Sample[] differences = SteimTestHelper.getSamplesWithFourByteDifferences(numWordsInBlock + 1);
    int currentSample = 0;
    Steim1Block steimBlock =
        new Steim1Block(
            DESCRIPTION, new Steim1DataFrameProvider(), differences[currentSample++], 1);
    for (int i = 0; i < numWordsInBlock - 1; i++) {
      steimBlock.addSample(differences[currentSample++]);
    }

    Sample lastSample = differences[currentSample];
    assertThat(steimBlock.isFull(), is(false));
    assertThat(steimBlock.getPercentFull(), is(both(greaterThan(50)).and(lessThan(100))));

    Optional<List<Sample>> overflow = steimBlock.addSample(lastSample);
    assertThat(steimBlock.isFull(), is(true));
    assertThat(steimBlock.getPercentFull(), is(100));
    assertThat(overflow.isPresent(), is(true));
    assertThat(overflow.get().isEmpty(), is(true));
  }

  @Test
  public void testCompleting7FrameBlockWithNoOverflowReturnsEmptyList() {
    int numFrames = 7;
    int numWordsInBlock =
        Steim1FirstDataFrame.NUM_DATA_WORDS + (numFrames - 1) * Steim1DataFrame.NUM_DATA_WORDS;
    Sample[] differences = SteimTestHelper.getSamplesWithFourByteDifferences(numWordsInBlock + 1);
    int currentSample = 0;
    Steim1Block steimBlock =
        new Steim1Block(DESCRIPTION, new Steim1DataFrameProvider(), differences[currentSample++]);
    for (int i = 0; i < numWordsInBlock - 1; i++) {
      steimBlock.addSample(differences[currentSample++]);
    }

    Sample lastSample = differences[currentSample];
    assertThat(steimBlock.isFull(), is(false));
    assertThat(steimBlock.getPercentFull(), is(both(greaterThan(50)).and(lessThan(100))));
    Optional<List<Sample>> overflow = steimBlock.addSample(lastSample);
    assertThat(steimBlock.isFull(), is(true));
    assertThat(steimBlock.getPercentFull(), is(100));
    assertThat(overflow.isPresent(), is(true));
    assertThat(overflow.get().isEmpty(), is(true));
  }

  @Test
  public void testAddingSampleThatOverflowsBlockReturnsOverflow() {
    int numFrames = 7;
    int numWordsInBlock =
        Steim1FirstDataFrame.NUM_DATA_WORDS + (numFrames - 1) * Steim1DataFrame.NUM_DATA_WORDS;
    Sample[] differences = SteimTestHelper.getSamplesWithFourByteDifferences(numWordsInBlock + 1);
    int currentSample = 0;
    Steim1Block steimBlock =
        new Steim1Block(DESCRIPTION, new Steim1DataFrameProvider(), differences[currentSample++]);
    for (int i = 0; i < numWordsInBlock - 1; i++) {
      steimBlock.addSample(differences[currentSample++]);
    }
    // At this point the block is filled except for the last word, which is empty

    Sample firstOneByteDifference =
        SteimTestHelper.getSampleWithOneByteDifferenceFrom(differences[(currentSample - 1)]);
    Sample secondOneByteDifference =
        SteimTestHelper.getSampleWithOneByteDifferenceFrom(firstOneByteDifference);
    Sample thirdOneByteDifference =
        SteimTestHelper.getSampleWithOneByteDifferenceFrom(secondOneByteDifference);
    Sample fourByteLastSample =
        SteimTestHelper.getSampleWithFourByteDifferenceFrom(thirdOneByteDifference);
    steimBlock.addSample(firstOneByteDifference);
    steimBlock.addSample(secondOneByteDifference);
    steimBlock.addSample(thirdOneByteDifference);

    // Block is now full except for the last word which has 3 one byte samples in it

    assertThat(steimBlock.isFull(), is(false));
    assertThat(steimBlock.getPercentFull(), is(both(greaterThan(50)).and(lessThan(100))));

    Optional<List<Sample>> overflow = steimBlock.addSample(fourByteLastSample);
    assertThat(steimBlock.isFull(), is(true));
    assertThat(steimBlock.getPercentFull(), is(100));
    assertThat(overflow.isPresent(), is(true));
    assertThat(overflow.get().size(), is(2));
    assertThat(overflow.get().get(0), is(thirdOneByteDifference));
    assertThat(overflow.get().get(1), is(fourByteLastSample));
  }

  @Test
  public void testBlockThatContainsLessThanMaxNumFramesIsNotFull() {
    Steim1Block steimBlock = new Steim1Block(DESCRIPTION, new Steim1DataFrameProvider(), SAMPLE);
    assertThat(steimBlock.isFull(), is(false));
    assertThat(steimBlock.getPercentFull(), is(0));
  }

  @Test
  public void testBlockThatContainsMaxNumFramesButLastFrameIsNotFullIsNotFull() {
    int numWordsInBlock = Steim1FirstDataFrame.NUM_DATA_WORDS;
    Sample[] differences = SteimTestHelper.getSamplesWithFourByteDifferences(numWordsInBlock + 1);
    int currentSample = 0;
    Steim1Block steimBlock =
        new Steim1Block(
            DESCRIPTION, new Steim1DataFrameProvider(), differences[currentSample++], 1);
    for (int i = 0; i < numWordsInBlock - 1; i++) {
      steimBlock.addSample(differences[currentSample++]);
    }

    assertThat(steimBlock.isFull(), is(false));
    assertThat(steimBlock.getPercentFull(), is(both(greaterThan(50)).and(lessThan(100))));
  }

  @Test
  public void testBlockThatContainsMaxNumOfFullFramesIsFull() {
    int numWordsInBlock = Steim1FirstDataFrame.NUM_DATA_WORDS;
    Sample[] differences = SteimTestHelper.getSamplesWithFourByteDifferences(numWordsInBlock + 1);
    int currentSample = 0;
    Steim1Block steimBlock =
        new Steim1Block(
            DESCRIPTION, new Steim1DataFrameProvider(), differences[currentSample++], 1);
    for (int i = 0; i < numWordsInBlock; i++) {
      steimBlock.addSample(differences[currentSample++]);
    }

    assertThat(steimBlock.isFull(), is(true));
    assertThat(steimBlock.getPercentFull(), is(100));
  }

  @Test
  public void testGetLastSampleOnNotFullBlockThrowsException() {
    assertThrows(
        IllegalStateException.class,
        () -> {
          Steim1Block steimBlock =
              new Steim1Block(DESCRIPTION, new Steim1DataFrameProvider(), SAMPLE);
          steimBlock.getLastSample();
        });
  }

  @Test
  public void testGetLastSampleOnFullBlockReturnsLastSampleOfLastFrame() {
    int numFrames = 7;
    int numWordsInBlock =
        Steim1FirstDataFrame.NUM_DATA_WORDS + (numFrames - 1) * Steim1DataFrame.NUM_DATA_WORDS;
    Sample[] differences = SteimTestHelper.getSamplesWithFourByteDifferences(numWordsInBlock + 1);
    int currentSample = 0;
    Steim1Block steimBlock =
        new Steim1Block(DESCRIPTION, new Steim1DataFrameProvider(), differences[currentSample++]);
    for (int i = 0; i < numWordsInBlock - 1; i++) {
      steimBlock.addSample(differences[currentSample++]);
    }

    Sample lastSample = new Sample(differences[currentSample].sample() - 1);
    steimBlock.addSample(lastSample);
    assertThat(steimBlock.isFull(), is(true));
    assertThat(steimBlock.getLastSample().get(), is(lastSample));
  }

  @Test
  public void testGetNumSamplesReturnsCurrentNumSamples() {
    Steim1Block steimBlock = new Steim1Block(DESCRIPTION, new Steim1DataFrameProvider(), SAMPLE);
    steimBlock.addSample(SAMPLE);
    steimBlock.addSample(SAMPLE);
    assertThat(steimBlock.getNumSamples(), is(2));
  }

  @Test
  public void testGetByteBlockOnNotFullBlockThrowsException() {
    assertThrows(
        IllegalStateException.class,
        () -> {
          Steim1Block steimBlock =
              new Steim1Block(DESCRIPTION, new Steim1DataFrameProvider(), SAMPLE);
          steimBlock.getBytes();
        });
  }

  @Test
  public void testGetByteBlockOnFullBlockReturnsByteBlock() {
    byte[] byteArray = new byte[1];

    SteimWord steimWord = mock(SteimWord.class);
    when(steimWord.getLastNonEmptySample()).thenReturn(SAMPLE);

    Steim1FirstDataFrame firstDataFrame = mock(Steim1FirstDataFrame.class);
    when(firstDataFrame.addSample(isA(Sample.class)))
        .thenReturn(Optional.of(Collections.<Sample>emptyList()));
    when(firstDataFrame.isFull()).thenReturn(true);
    when(firstDataFrame.getPercentFull()).thenReturn(100);
    when(firstDataFrame.toByteArray()).thenReturn(byteArray);
    when(firstDataFrame.isEmpty()).thenReturn(false);
    when(firstDataFrame.getLastNonEmptyWord()).thenReturn(steimWord);
    when(firstDataFrame.getLastSampleOfSteimBlock()).thenReturn(Optional.<Sample>empty());
    when(firstDataFrame.getNumSamples()).thenReturn(1);

    Steim1DataFrameProvider frameProvider = mock(Steim1DataFrameProvider.class);
    when(frameProvider.getFirstDataFrame(startsWith(DESCRIPTION), isA(Sample.class)))
        .thenReturn(firstDataFrame);

    Steim1Block steimBlock =
        new Steim1Block(DESCRIPTION, frameProvider, INITIALIZING_LAST_SAMPLE, 1);
    steimBlock.addSample(SAMPLE);
    assertThat(steimBlock.getBytes(), is(byteArray));
  }

  @Test
  public void testGetByteBlockOnForceCompletedBlockWithMultipleFrames() {
    testGetByteBlockOnForceCompletedBlock(2, 1);
    testGetByteBlockOnForceCompletedBlock(2, 2);
    testGetByteBlockOnForceCompletedBlock(
        2, Steim1FirstDataFrame.NUM_DATA_WORDS * Steim1DataWord.STEIM_ONE_MAX_DIFFERENCE_WIDTH - 1);
  }

  @Test
  public void testGetByteBlockOnForceCompletedBlockWithOneFrame() {
    testGetByteBlockOnForceCompletedBlock(1, 1);
    testGetByteBlockOnForceCompletedBlock(1, 2);
    testGetByteBlockOnForceCompletedBlock(
        1, Steim1FirstDataFrame.NUM_DATA_WORDS * Steim1DataWord.STEIM_ONE_MAX_DIFFERENCE_WIDTH - 1);
  }

  private void testGetByteBlockOnForceCompletedBlock(int numFrames, int numSamplesMissing) {
    int numWordsInBlock =
        Steim1FirstDataFrame.NUM_DATA_WORDS + (numFrames - 1) * Steim1DataFrame.NUM_DATA_WORDS;
    int numOneByteSamplesInBlock = Steim1DataWord.STEIM_ONE_MAX_DIFFERENCE_WIDTH * numWordsInBlock;
    Sample[] samples =
        SteimTestHelper.getSamplesWithOneByteDifferences(numOneByteSamplesInBlock + 1);
    int currentSample = 0;
    Steim1Block steimBlock =
        new Steim1Block(
            DESCRIPTION, new Steim1DataFrameProvider(), samples[currentSample++], numFrames);
    int[] sampleValues = new int[numOneByteSamplesInBlock - numSamplesMissing];
    for (int i = 0; i < sampleValues.length; i++) {
      Optional<List<Sample>> overflow = steimBlock.addSample(samples[currentSample]);
      if (!overflow.isPresent()) {
        sampleValues[i] = samples[currentSample].sample();
      }
      currentSample++;
    }

    Optional<Sample> overflow = steimBlock.forceComplete();
    if (overflow.isPresent()) {
      // Remove any samples that were initially added, but then removed from forceComplete()
      sampleValues = Arrays.copyOf(sampleValues, sampleValues.length - 1);
    }

    BigEndianReader bigEndianReader = new BigEndianReader(steimBlock.getBytes(), 0);
    DecodeSteim1 decodeSteim1 =
        new DecodeSteim1(bigEndianReader, steimBlock.getNumSamples(), steimBlock.getBytes().length);
    Samples decode = decodeSteim1.decode();

    assertThat(decodeSteim1.initialSample(), is(samples[1].sample()));
    assertThat(decode.intSamples(), is(sampleValues));
  }

  @Test
  public void testForceCompleteEmptyBlockThrowsException() {
    assertThrows(
        IllegalStateException.class,
        () -> {
          Steim1Block steimBlock =
              new Steim1Block(DESCRIPTION, new Steim1DataFrameProvider(), INITIALIZING_LAST_SAMPLE);
          steimBlock.forceComplete();
        });
  }

  @Test
  public void testForceCompleteWithNoOverflowReturnsOptionalempty() {
    Steim1Block steimBlock =
        new Steim1Block(DESCRIPTION, new Steim1DataFrameProvider(), INITIALIZING_LAST_SAMPLE);
    steimBlock.addSample(SAMPLE);
    assertThat(steimBlock.forceComplete(), is(Optional.<Sample>empty()));
  }

  @Test
  public void testForceCompleteWithOverflowReturnsOverflow() {
    int numFrames = 7;
    int numWordsInBlock =
        Steim1FirstDataFrame.NUM_DATA_WORDS + (numFrames - 1) * Steim1DataFrame.NUM_DATA_WORDS;
    int numOneByteSamplesInBlock = Steim1DataWord.STEIM_ONE_MAX_DIFFERENCE_WIDTH * numWordsInBlock;
    Sample[] differences =
        SteimTestHelper.getSamplesWithOneByteDifferences(numOneByteSamplesInBlock + 1);
    int currentSample = 0;
    Steim1Block steimBlock =
        new Steim1Block(DESCRIPTION, new Steim1DataFrameProvider(), differences[currentSample++]);
    for (int i = 0; i < numOneByteSamplesInBlock - 2; i++) {
      steimBlock.addSample(differences[currentSample++]);
    }
    Sample lastSample = differences[currentSample];
    steimBlock.addSample(lastSample);

    assertThat(steimBlock.forceComplete(), is(Optional.of(lastSample)));
  }

  @Test
  public void testForceCompleteWithFullFirstFrameAndPartiallyFullSecondFrame() {
    int numFrames = 2;
    int numWordsInBlock =
        Steim1FirstDataFrame.NUM_DATA_WORDS + (numFrames - 1) * Steim1DataFrame.NUM_DATA_WORDS;
    int numOneByteSamplesInBlock = Steim1DataWord.STEIM_ONE_MAX_DIFFERENCE_WIDTH * numWordsInBlock;
    Sample[] differences =
        SteimTestHelper.getSamplesWithOneByteDifferences(numOneByteSamplesInBlock + 1);
    int currentSample = 0;
    Steim1Block steimBlock =
        new Steim1Block(
            DESCRIPTION, new Steim1DataFrameProvider(), differences[currentSample++], numFrames);
    for (int i = 0; i < numOneByteSamplesInBlock - 10; i++) {
      steimBlock.addSample(differences[currentSample++]);
    }

    steimBlock.forceComplete();
  }

  @Test
  public void testForceCompletedBlockGetLastSampleReturnsLastActualSample() {
    int numFrames = 2;
    int numWordsInBlock =
        Steim1FirstDataFrame.NUM_DATA_WORDS + (numFrames - 1) * Steim1DataFrame.NUM_DATA_WORDS;
    int numOneByteSamplesInBlock = Steim1DataWord.STEIM_ONE_MAX_DIFFERENCE_WIDTH * numWordsInBlock;
    Sample[] samples =
        SteimTestHelper.getSamplesWithOneByteDifferences(numOneByteSamplesInBlock + 1);
    int currentSample = 0;
    Steim1Block steimBlock =
        new Steim1Block(
            DESCRIPTION, new Steim1DataFrameProvider(), samples[currentSample++], numFrames);
    for (int i = 0; i < numOneByteSamplesInBlock - 10; i++) {
      steimBlock.addSample(samples[currentSample++]);
    }

    Sample lastSample = samples[currentSample - 1];
    steimBlock.forceComplete();
    assertThat(steimBlock.getLastNonEmptySample(), is(lastSample));
  }

  @Test
  public void testForceCompleteFills1FrameBlock() {
    Steim1Block steimBlock =
        new Steim1Block(DESCRIPTION, new Steim1DataFrameProvider(), INITIALIZING_LAST_SAMPLE, 1);
    steimBlock.addSample(SAMPLE);
    steimBlock.forceComplete();
    assertThat(steimBlock.isFull(), is(true));
    assertThat(steimBlock.getPercentFull(), is(100));
  }

  @Test
  public void testForceCompleteFills2FrameBlock() {
    Steim1Block steimBlock =
        new Steim1Block(DESCRIPTION, new Steim1DataFrameProvider(), INITIALIZING_LAST_SAMPLE, 2);
    steimBlock.addSample(SAMPLE);
    steimBlock.forceComplete();
    assertThat(steimBlock.isFull(), is(true));
    assertThat(steimBlock.getPercentFull(), is(100));
  }

  @Test
  public void testForceCompleteFills7FrameBlock() {
    Steim1Block steimBlock =
        new Steim1Block(DESCRIPTION, new Steim1DataFrameProvider(), INITIALIZING_LAST_SAMPLE, 7);
    steimBlock.addSample(SAMPLE);
    steimBlock.forceComplete();
    assertThat(steimBlock.isFull(), is(true));
    assertThat(steimBlock.getPercentFull(), is(100));
  }

  @Test
  public void testOverflowingFramesAreForceCompleted_CENT_998() {
    Sample sample = new Sample(0);
    int numWordsInBlock =
        Steim1FirstDataFrame.NUM_DATA_WORDS * Steim1DataWord.STEIM_ONE_MAX_DIFFERENCE_WIDTH - 1;
    Steim1Block steimBlock = new Steim1Block(DESCRIPTION, new Steim1DataFrameProvider(), sample, 7);
    for (int i = 0; i < numWordsInBlock; i++) {
      steimBlock.addSample(sample);
    }

    steimBlock.forceComplete();

    BigEndianReader bigEndianReader = new BigEndianReader(steimBlock.getBytes(), 0);
    DecodeSteim1 decodeSteim1 =
        new DecodeSteim1(bigEndianReader, steimBlock.getNumSamples(), steimBlock.getBytes().length);
    Samples decode = decodeSteim1.decode();

    assertThat(decode.intSamples().length, is(numWordsInBlock));
    assertThat(steimBlock.getNumSamples(), is(numWordsInBlock));
  }
}
