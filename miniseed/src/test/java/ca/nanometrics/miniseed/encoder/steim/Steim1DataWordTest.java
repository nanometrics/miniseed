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

import ca.nanometrics.miniseed.Sample;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class Steim1DataWordTest {
  private static final Sample INTEGER_MAX_VALUE = new Sample(Integer.MAX_VALUE);
  private static final Sample SHORT_MAX_VALUE = new Sample(Short.MAX_VALUE);
  private static final Sample BYTE_MAX_VALUE = new Sample(Byte.MAX_VALUE);
  private static final Sample ZERO_VALUE = new Sample(0);
  private static final Sample LAST_SAMPLE = new Sample(1);

  @Test
  public void testFullWordOverflowsSamples() {
    Sample overflow = new Sample(1);
    SteimWord word = new Steim1DataWord(new Sample(0));
    word.addSample(INTEGER_MAX_VALUE);
    assertThat(word.isFull(), is(true));
    Optional<List<Sample>> overflowList = word.addSample(overflow);
    assertThat(overflowList.isPresent(), is(true));
    assertThat(overflowList.get().size(), is(1));
    assertThat(overflowList.get().get(0), is(overflow));
  }

  @Test
  public void testAddOneOneByteValueThenOneTwoByteValueFillsWord() {
    SteimWord word = new Steim1DataWord(BYTE_MAX_VALUE);
    word.addSample(ZERO_VALUE);
    word.addSample(SHORT_MAX_VALUE);
    assertThat(word.isFull(), is(true));
  }

  @Test
  public void testAddOneOneByteValueThenOneFourByteValueFillsWordAndOverflowsFourByteValue() {
    SteimWord word = new Steim1DataWord(BYTE_MAX_VALUE);
    word.addSample(ZERO_VALUE);
    Optional<List<Sample>> overflow = word.addSample(INTEGER_MAX_VALUE);
    assertThat(word.isFull(), is(true));
    assertThat(overflow.isPresent(), is(true));
    assertThat(overflow.get().size(), is(1));
    assertThat(overflow.get().get(0), is(INTEGER_MAX_VALUE));
  }

  @Test
  public void testAddTwoOneByteValuesOneTwoByteValueFillsWordAndOverflowsTwoByteValue() {
    SteimWord word = new Steim1DataWord(ZERO_VALUE);
    word.addSample(BYTE_MAX_VALUE);
    word.addSample(ZERO_VALUE);
    Optional<List<Sample>> overflow = word.addSample(SHORT_MAX_VALUE);
    assertThat(word.isFull(), is(true));
    assertThat(overflow.isPresent(), is(true));
    assertThat(overflow.get().size(), is(1));
    assertThat(overflow.get().get(0), is(SHORT_MAX_VALUE));
  }

  @Test
  public void testAddTwoOneByteValuesOneFourByteValueFillsWordAndOverflowsFourByteValue() {
    SteimWord word = new Steim1DataWord(ZERO_VALUE);
    word.addSample(BYTE_MAX_VALUE);
    word.addSample(ZERO_VALUE);
    Optional<List<Sample>> overflow = word.addSample(INTEGER_MAX_VALUE);
    assertThat(word.isFull(), is(true));
    assertThat(overflow.isPresent(), is(true));
    assertThat(overflow.get().size(), is(1));
    assertThat(overflow.get().get(0), is(INTEGER_MAX_VALUE));
  }

  @Test
  public void testThreeOneByteValuesDoesNotFillWord() {
    SteimWord word = new Steim1DataWord(ZERO_VALUE);
    word.addSample(BYTE_MAX_VALUE);
    word.addSample(ZERO_VALUE);
    word.addSample(BYTE_MAX_VALUE);
    assertThat(word.isFull(), is(false));
  }

  @Test
  public void
      testAddThreeOneByteValuesThenOneTwoByteValuesFillsWordAndOverflowsOneByteValueAndTwoByteValue() {
    Sample oneByteOverflow = new Sample(1);
    SteimWord word = new Steim1DataWord(BYTE_MAX_VALUE);
    word.addSample(ZERO_VALUE);
    word.addSample(BYTE_MAX_VALUE);
    word.addSample(oneByteOverflow);
    Optional<List<Sample>> overflow = word.addSample(SHORT_MAX_VALUE);
    assertThat(word.isFull(), is(true));
    assertThat(overflow.isPresent(), is(true));
    assertThat(overflow.get().size(), is(2));
    assertThat(overflow.get().get(0), is(oneByteOverflow));
    assertThat(overflow.get().get(1), is(SHORT_MAX_VALUE));
  }

  @Test
  public void
      testAddThreeOneByteValuesThenOneFourByteValuesFillsWordAndOverflowsOneByteValueAndFourByteValue() {
    Sample oneByteOverflow = new Sample(1);
    SteimWord word = new Steim1DataWord(BYTE_MAX_VALUE);
    word.addSample(ZERO_VALUE);
    word.addSample(BYTE_MAX_VALUE);
    word.addSample(oneByteOverflow);
    Optional<List<Sample>> overflow = word.addSample(INTEGER_MAX_VALUE);
    assertThat(word.isFull(), is(true));
    assertThat(overflow.isPresent(), is(true));
    assertThat(overflow.get().size(), is(2));
    assertThat(overflow.get().get(0), is(oneByteOverflow));
    assertThat(overflow.get().get(1), is(INTEGER_MAX_VALUE));
  }

  @Test
  public void testAddFourOneByteValuesFillsWord() {
    SteimWord word = new Steim1DataWord(ZERO_VALUE);
    word.addSample(BYTE_MAX_VALUE);
    word.addSample(ZERO_VALUE);
    word.addSample(BYTE_MAX_VALUE);
    word.addSample(ZERO_VALUE);
    assertThat(word.isFull(), is(true));
    assertThat(word.getNumSamples(), is(4));
  }

  @Test
  public void testAddFourOneByteValuesReturnsEmptyList() {
    SteimWord word = new Steim1DataWord(ZERO_VALUE);
    word.addSample(BYTE_MAX_VALUE);
    word.addSample(ZERO_VALUE);
    word.addSample(BYTE_MAX_VALUE);
    Optional<List<Sample>> overflow = word.addSample(ZERO_VALUE);
    assertThat(overflow.isPresent(), is(true));
    assertThat(overflow.get().isEmpty(), is(true));
  }

  @Test
  public void testAddOneTwoByteValueDoesNotFillWord() {
    SteimWord word = new Steim1DataWord(ZERO_VALUE);
    word.addSample(SHORT_MAX_VALUE);
    assertThat(word.isFull(), is(false));
  }

  @Test
  public void testAddTwoTwoByteValuesFillsWord() {
    SteimWord word = new Steim1DataWord(ZERO_VALUE);
    word.addSample(SHORT_MAX_VALUE);
    word.addSample(ZERO_VALUE);
    assertThat(word.isFull(), is(true));
    assertThat(word.getNumSamples(), is(2));
  }

  @Test
  public void testAddTwoTwoByteValuesReturnsEmptyList() {
    SteimWord word = new Steim1DataWord(ZERO_VALUE);
    word.addSample(SHORT_MAX_VALUE);
    Optional<List<Sample>> overflow = word.addSample(ZERO_VALUE);
    assertThat(overflow.isPresent(), is(true));
    assertThat(overflow.get().isEmpty(), is(true));
  }

  @Test
  public void testAddOneTwoByteValueThenOneOneByteValueFillsWord() {
    SteimWord word = new Steim1DataWord(SHORT_MAX_VALUE);
    word.addSample(ZERO_VALUE);
    word.addSample(BYTE_MAX_VALUE);
    assertThat(word.isFull(), is(true));
  }

  @Test
  public void testAddOneTwoByteValueThenOneFourByteValueFillsWordAndOverflowsFourByteValue() {
    SteimWord word = new Steim1DataWord(SHORT_MAX_VALUE);
    word.addSample(ZERO_VALUE);
    Optional<List<Sample>> overflow = word.addSample(INTEGER_MAX_VALUE);
    assertThat(word.isFull(), is(true));
    assertThat(overflow.isPresent(), is(true));
    assertThat(overflow.get().size(), is(1));
    assertThat(overflow.get().get(0), is(INTEGER_MAX_VALUE));
  }

  @Test
  public void testAddOneFourByteValueFillsWord() {
    SteimWord word = new Steim1DataWord(new Sample(0));
    word.addSample(INTEGER_MAX_VALUE);
    assertThat(word.isFull(), is(true));
    assertThat(word.getNumSamples(), is(1));
  }

  @Test
  public void testAddOneFourByteValueReturnsEmptyList() {
    SteimWord word = new Steim1DataWord(new Sample(0));
    Optional<List<Sample>> overflow = word.addSample(INTEGER_MAX_VALUE);
    assertThat(overflow.isPresent(), is(true));
    assertThat(overflow.get().isEmpty(), is(true));
  }

  @Test
  public void testGetLastSample() {
    Sample lastSample = new Sample(1);
    SteimWord word = new Steim1DataWord(ZERO_VALUE);
    word.addSample(BYTE_MAX_VALUE);
    word.addSample(ZERO_VALUE);
    word.addSample(BYTE_MAX_VALUE);
    word.addSample(lastSample);
    assertThat(word.getLastSample().get(), is(lastSample));
  }

  @Test
  public void testGetLastSampleOnNotFullWordThrowsException() {
    assertThrows(
        IllegalStateException.class,
        () -> {
          SteimWord word = new Steim1DataWord(ZERO_VALUE);
          word.getLastSample();
        });
  }

  @Test
  public void testGetFirstSample() {
    SteimWord word = new Steim1DataWord(ZERO_VALUE);
    word.addSample(BYTE_MAX_VALUE);
    assertThat(word.getFirstSample(), is(BYTE_MAX_VALUE));
  }

  @Test
  public void testGetFirstSampleFromEmptyWordThrowsException() {
    assertThrows(
        IllegalStateException.class,
        () -> {
          SteimWord word = new Steim1DataWord(ZERO_VALUE);
          word.getFirstSample();
        });
  }

  @Test
  public void testGetNumSamples() {
    SteimWord oneByteWord = new Steim1DataWord(ZERO_VALUE);
    assertThat(oneByteWord.getNumSamples(), is(0));
    oneByteWord.addSample(BYTE_MAX_VALUE);
    assertThat(oneByteWord.getNumSamples(), is(1));
    oneByteWord.addSample(ZERO_VALUE);
    assertThat(oneByteWord.getNumSamples(), is(2));
    oneByteWord.addSample(BYTE_MAX_VALUE);
    assertThat(oneByteWord.getNumSamples(), is(3));
    oneByteWord.addSample(ZERO_VALUE);
    assertThat(oneByteWord.getNumSamples(), is(4));

    SteimWord twoByteWord = new Steim1DataWord(ZERO_VALUE);
    twoByteWord.addSample(SHORT_MAX_VALUE);
    assertThat(twoByteWord.getNumSamples(), is(1));
    twoByteWord.addSample(ZERO_VALUE);
    assertThat(twoByteWord.getNumSamples(), is(2));

    SteimWord fourByteWord = new Steim1DataWord(ZERO_VALUE);
    fourByteWord.addSample(INTEGER_MAX_VALUE);
    assertThat(fourByteWord.getNumSamples(), is(1));
  }

  @Test
  public void testOneByteDifferencesByteArray() {
    byte[] oneByteDifferences = {100, 12, -15, 36};
    int sampleValue = 0;
    SteimWord word = new Steim1DataWord(new Sample(sampleValue));
    sampleValue += oneByteDifferences[0];
    word.addSample(new Sample(sampleValue));
    sampleValue += oneByteDifferences[1];
    word.addSample(new Sample(sampleValue));
    sampleValue += oneByteDifferences[2];
    word.addSample(new Sample(sampleValue));
    sampleValue += oneByteDifferences[3];
    word.addSample(new Sample(sampleValue));

    assertThat(Arrays.equals(oneByteDifferences, word.toByteArray()), is(true));
  }

  @Test
  public void testTwoByteDifferencesByteArray() {
    short[] twoByteDifferences = {500, -1000};
    int sampleValue = 0;
    SteimWord word = new Steim1DataWord(new Sample(sampleValue));
    sampleValue += twoByteDifferences[0];
    word.addSample(new Sample(sampleValue));
    sampleValue += twoByteDifferences[1];
    word.addSample(new Sample(sampleValue));

    ByteBuffer byteBuffer = ByteBuffer.allocate(4).put(word.toByteArray());
    assertThat(twoByteDifferences[0], is(byteBuffer.getShort(0)));
  }

  @Test
  public void testFourByteDifferencesByteArray() {
    int[] fourByteDifferences = {500000};
    int sampleValue = 0;
    SteimWord word = new Steim1DataWord(new Sample(sampleValue));
    sampleValue += fourByteDifferences[0];
    word.addSample(new Sample(sampleValue));

    ByteBuffer byteBuffer = ByteBuffer.allocate(4).put(word.toByteArray());
    byteBuffer.rewind();
    assertThat(fourByteDifferences[0], is(byteBuffer.getInt()));
  }

  @Test
  public void testOneByteFollowedByTwoByteByteArray() {
    short[] twoByteDifferences = {10, 1000};
    int sampleValue = 0;
    SteimWord word = new Steim1DataWord(new Sample(sampleValue));
    sampleValue += twoByteDifferences[0];
    word.addSample(new Sample(sampleValue));
    sampleValue += twoByteDifferences[1];
    word.addSample(new Sample(sampleValue));

    ByteBuffer byteBuffer = ByteBuffer.allocate(4).put(word.toByteArray());
    assertThat(twoByteDifferences[0], is(byteBuffer.getShort(0)));
  }

  @Test
  public void testTwoByteFollowedByOneByteByteArray() {
    short[] twoByteDifferences = {1000, 10};
    int sampleValue = 0;
    SteimWord word = new Steim1DataWord(new Sample(sampleValue));
    sampleValue += twoByteDifferences[0];
    word.addSample(new Sample(sampleValue));
    sampleValue += twoByteDifferences[1];
    word.addSample(new Sample(sampleValue));

    ByteBuffer byteBuffer = ByteBuffer.allocate(4).put(word.toByteArray());
    assertThat(twoByteDifferences[0], is(byteBuffer.getShort(0)));
  }

  @Test
  public void testForceCompleteEmptyWordReturnsOptionalempty() {
    SteimWord word = new Steim1DataWord(LAST_SAMPLE);
    assertThat(word.forceComplete(), is(Optional.<Sample>empty()));
  }

  @Test
  public void testForceCompleteOneSampleWordReturnsOptionalempty() {
    SteimWord word = new Steim1DataWord(LAST_SAMPLE);
    word.addSample(BYTE_MAX_VALUE);
    assertThat(word.forceComplete(), is(Optional.<Sample>empty()));
  }

  @Test
  public void testForceCompleteTwoSampleWordReturnsOptionalempty() {
    SteimWord word = new Steim1DataWord(LAST_SAMPLE);
    word.addSample(BYTE_MAX_VALUE);
    word.addSample(ZERO_VALUE);
    assertThat(word.forceComplete(), is(Optional.<Sample>empty()));
  }

  @Test
  public void testForceCompleteThreeSampleWordReturnsThirdSample() {
    Sample overflow = new Sample(5);
    SteimWord word = new Steim1DataWord(LAST_SAMPLE);
    word.addSample(BYTE_MAX_VALUE);
    word.addSample(ZERO_VALUE);
    word.addSample(overflow);
    assertThat(word.forceComplete(), is(Optional.of(overflow)));
  }

  @Test
  public void testForceCompleteOneOneByteSampleWordHasFourByteData() {
    SteimWord word = new Steim1DataWord(LAST_SAMPLE);
    word.addSample(BYTE_MAX_VALUE);
    word.forceComplete();
    assertThat(
        word.getTwoBitNibbleCode(),
        is(Steim1ControlCode.FOUR_BYTE_DATA.getTwoBitControlCodeValue()));
  }

  @Test
  public void testForceCompleteTwoOneByteSampleWordHasTwoByteData() {
    SteimWord word = new Steim1DataWord(LAST_SAMPLE);
    word.addSample(BYTE_MAX_VALUE);
    word.addSample(ZERO_VALUE);
    word.forceComplete();
    assertThat(
        word.getTwoBitNibbleCode(),
        is(Steim1ControlCode.TWO_BYTE_DATA.getTwoBitControlCodeValue()));
  }

  @Test
  public void testForceCompleteThreeOneByteSampleWordHasTwoByteData() {
    SteimWord word = new Steim1DataWord(LAST_SAMPLE);
    word.addSample(BYTE_MAX_VALUE);
    word.addSample(ZERO_VALUE);
    word.addSample(BYTE_MAX_VALUE);
    word.forceComplete();
    assertThat(
        word.getTwoBitNibbleCode(),
        is(Steim1ControlCode.TWO_BYTE_DATA.getTwoBitControlCodeValue()));
  }

  @Test
  public void testForceCompleteFillsWord() {
    SteimWord word = new Steim1DataWord(LAST_SAMPLE);
    word.forceComplete();
    assertThat(word.isFull(), is(true));
  }
}
