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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ca.nanometrics.miniseed.Sample;
import ca.nanometrics.miniseed.encoding.steim.DecodeSteim1;
import ca.nanometrics.miniseed.endian.BigEndianReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class Steim1EncoderTest {
  private static final String DESCRIPTION = "testencoder";

  private static final Sample INITIALIZING_LAST_SAMPLE = new Sample(1); // Arbitrary number
  private static final Sample SAMPLE = new Sample(2); // Distinct arbitrary number

  @Test
  public void testCompletedBlockCausesNewBlock() {
    SteimBlock fullBlock = mockFullSteimBlock(INITIALIZING_LAST_SAMPLE, SAMPLE);

    SteimBlock secondBlock = mock(SteimBlock.class);
    when(secondBlock.addSample(isA(Sample.class))).thenReturn(Optional.<List<Sample>>empty());

    Steim1BlockProvider blockProvider = mock(Steim1BlockProvider.class);
    when(blockProvider.getBlock(startsWith(DESCRIPTION), isA(Sample.class)))
        .thenReturn(fullBlock)
        .thenReturn(secondBlock);
    Steim1Encoder steim1Encoder =
        new Steim1Encoder(DESCRIPTION, blockProvider, new Sample(0), null);
    steim1Encoder.addSample(SAMPLE);
    verify(blockProvider, times(2)).getBlock(startsWith(DESCRIPTION), isA(Sample.class));
  }

  @Test
  public void testNewBlockIsInitializedWithForwardIntegrationConstantFromPreviousBlock() {
    SteimBlock fullBlock = mockFullSteimBlock(INITIALIZING_LAST_SAMPLE, SAMPLE);

    SteimBlock secondBlock = mock(SteimBlock.class);
    when(secondBlock.addSample(isA(Sample.class))).thenReturn(Optional.<List<Sample>>empty());

    Steim1BlockProvider blockProvider = mock(Steim1BlockProvider.class);
    when(blockProvider.getBlock(startsWith(DESCRIPTION), isA(Sample.class))).thenReturn(fullBlock);
    when(blockProvider.getBlock(startsWith(DESCRIPTION), eq(INITIALIZING_LAST_SAMPLE)))
        .thenReturn(secondBlock);

    Steim1Encoder steim1Encoder =
        new Steim1Encoder(DESCRIPTION, blockProvider, new Sample(0), null);
    steim1Encoder.addSample(SAMPLE);
    verify(blockProvider, times(2)).getBlock(any(), any());
  }

  @Test
  public void testCompletedBlockNotifiesObserversWithCompletedBlock() {
    SteimBlock fullBlock = mockFullSteimBlock(INITIALIZING_LAST_SAMPLE, SAMPLE);
    SteimBlockObserver observer = mock(SteimBlockObserver.class);

    SteimBlock secondBlock = mock(SteimBlock.class);
    when(secondBlock.addSample(isA(Sample.class))).thenReturn(Optional.<List<Sample>>empty());

    Steim1BlockProvider blockProvider = mock(Steim1BlockProvider.class);
    when(blockProvider.getBlock(startsWith(DESCRIPTION), isA(Sample.class))) //
        .thenReturn(fullBlock) //
        .thenReturn(secondBlock);

    Steim1Encoder steim1Encoder =
        new Steim1Encoder(DESCRIPTION, blockProvider, new Sample(0), Arrays.asList(observer));
    steim1Encoder.addSample(SAMPLE);
    verify(observer).steimBlockComplete(fullBlock);
  }

  @Test
  public void testIncompleteBlockDoesNotNotifyObservers() {
    SteimBlock emptyBlock = mock(SteimBlock.class);
    when(emptyBlock.isFull()).thenReturn(false);
    when(emptyBlock.addSample(isA(Sample.class))).thenReturn(Optional.<List<Sample>>empty());

    SteimBlockObserver observer = mock(SteimBlockObserver.class);

    Steim1BlockProvider blockProvider = mock(Steim1BlockProvider.class);
    when(blockProvider.getBlock(startsWith(DESCRIPTION), isA(Sample.class))).thenReturn(emptyBlock);

    Steim1Encoder steim1Encoder =
        new Steim1Encoder(DESCRIPTION, blockProvider, new Sample(0), Arrays.asList(observer));
    steim1Encoder.addSample(SAMPLE);
    verifyNoInteractions(observer);
  }

  @Test
  public void testFlushWithNoOverflowNotifiesObserverWithOneBlock() {
    SteimBlock nextSteimBlock = mock(SteimBlock.class);

    SteimBlock steimBlock = mock(SteimBlock.class);
    when(steimBlock.getLastSample()).thenReturn(Optional.of(INITIALIZING_LAST_SAMPLE));
    when(steimBlock.forceComplete()).thenReturn(Optional.<Sample>empty());
    when(steimBlock.isEmpty()).thenReturn(false);

    SteimBlockObserver observer = mock(SteimBlockObserver.class);

    Steim1BlockProvider blockProvider = mock(Steim1BlockProvider.class);
    when(blockProvider.getBlock(startsWith(DESCRIPTION), isA(Sample.class))).thenReturn(steimBlock);
    when(blockProvider.getBlock(startsWith(DESCRIPTION), isA(Sample.class)))
        .thenReturn(nextSteimBlock);

    Steim1Encoder steim1Encoder =
        new Steim1Encoder(DESCRIPTION, blockProvider, new Sample(0), Arrays.asList(observer));
    steim1Encoder.flush(null);
    verify(observer, times(1)).steimBlockComplete(isA(SteimBlock.class));
  }

  @Test
  public void testFlushWithOverflowNotifiesObserverWithTwoBlocks() {
    SteimBlockObserver observer = this::assertSteimBlockDecodesProperly;

    Sample[] samples =
        SteimTestHelper.getSamplesWithConstantDifferenceBytes(
            Steim1FirstDataFrame.NUM_DATA_WORDS * Steim1DataWord.STEIM_ONE_MAX_DIFFERENCE_WIDTH, 1);

    Steim1Encoder steim1Encoder =
        new Steim1Encoder(
            DESCRIPTION, new Steim1BlockProvider(1), samples[0], Arrays.asList(observer));

    for (int i = 1; i < samples.length; i++) {
      steim1Encoder.addSample(samples[i]);
    }
    steim1Encoder.flush(null);
  }

  @Test
  public void testFlushEmptyBlockDoesNotNotifyObservers() {
    SteimBlock nextSteimBlock = mock(SteimBlock.class);

    SteimBlock steimBlock = mock(SteimBlock.class);
    when(steimBlock.isEmpty()).thenReturn(true);

    SteimBlockObserver observer = mock(SteimBlockObserver.class);

    Steim1BlockProvider blockProvider = mock(Steim1BlockProvider.class);
    when(blockProvider.getBlock(startsWith(DESCRIPTION), isA(Sample.class))).thenReturn(steimBlock);
    when(blockProvider.getBlock(startsWith(DESCRIPTION), isA(Sample.class)))
        .thenReturn(nextSteimBlock);

    Steim1Encoder steim1Encoder =
        new Steim1Encoder(DESCRIPTION, blockProvider, new Sample(0), Arrays.asList(observer));
    steim1Encoder.flush(null);
    verify(blockProvider, times(2)).getBlock(startsWith(DESCRIPTION), isA(Sample.class));
  }

  private SteimBlock mockFullSteimBlock(Sample initializingLastSample, Sample sample2) {
    SteimBlock steimBlock = mock(SteimBlock.class);
    when(steimBlock.isFull()).thenReturn(true);
    when(steimBlock.getLastSample()).thenReturn(Optional.of(initializingLastSample));
    when(steimBlock.addSample(sample2)).thenReturn(Optional.of(Arrays.asList(sample2)));
    return steimBlock;
  }

  @Test
  public void testEmptyCurrentBlock() {
    Steim1Encoder encoder =
        new Steim1Encoder(
            DESCRIPTION, new Steim1BlockProvider(7), Collections.<SteimBlockObserver>emptyList());
    assertThat(encoder.isCurrentBlockEmpty(), is(true));
    assertThat(encoder.getCurrentBlockPercentFull(), is(0));
    encoder.flush(null);
  }

  @Test
  public void testFlushIncompleteBlock() {
    SteimBlockObserver observer = mock(SteimBlockObserver.class);
    Steim1Encoder encoder =
        new Steim1Encoder(
            DESCRIPTION,
            new Steim1BlockProvider(7),
            INITIALIZING_LAST_SAMPLE,
            Collections.singleton(observer));
    assertThat(encoder.isCurrentBlockEmpty(), is(true));
    assertThat(encoder.getCurrentBlockPercentFull(), is(0));
    encoder.flush(null);

    reset(observer);
    ArgumentCaptor<SteimBlock> completedBlock = ArgumentCaptor.forClass(SteimBlock.class);
    encoder.addSample(new Sample(2));
    encoder.flush(null);
    verify(observer).steimBlockComplete(completedBlock.capture());
    assertThat(completedBlock.getValue().getNumSamples(), is(1));

    completedBlock = ArgumentCaptor.forClass(SteimBlock.class);
    reset(observer);

    encoder.addSample(new Sample(3));
    encoder.addSample(new Sample(4));
    encoder.flush(null);
    verify(observer).steimBlockComplete(completedBlock.capture());
    assertThat(completedBlock.getValue().getNumSamples(), is(2));
    assertSteimBlockDecodesProperly(completedBlock.getValue());
  }

  void assertSteimBlockDecodesProperly(SteimBlock steimBlock) {
    BigEndianReader bigEndianReader = new BigEndianReader(steimBlock.getBytes(), 0);
    DecodeSteim1 decodeSteim1 =
        new DecodeSteim1(bigEndianReader, steimBlock.getNumSamples(), steimBlock.getBytes().length);
    decodeSteim1.decode();
  }
}
