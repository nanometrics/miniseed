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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Steim1Encoder implements SteimEncoder {
  static final Sample NO_PREVIOUS_SAMPLE = new Sample(0);

  private final String m_description;
  private SteimBlock m_currentSteimBlock;
  private final Collection<SteimBlockObserver> m_observers;
  private final Steim1BlockProvider m_blockProvider;
  private int m_numBlocksCreated = 0;

  public Steim1Encoder(
      String description, Steim1BlockProvider provider, Collection<SteimBlockObserver> observers) {
    this(description, provider, NO_PREVIOUS_SAMPLE, observers);
  }

  public Steim1Encoder(
      String description,
      Steim1BlockProvider provider,
      Sample lastSample,
      Collection<SteimBlockObserver> observers) {
    m_description = description;
    if (provider == null) {
      throw new IllegalArgumentException("Must provide block provider");
    }
    m_blockProvider = provider;
    m_observers = new ArrayList<>();
    if (observers != null && !observers.isEmpty()) {
      m_observers.addAll(observers);
    }
    if (lastSample == null) {
      throw new IllegalArgumentException("Must provide last sample");
    }
    m_currentSteimBlock = m_blockProvider.getBlock(getNextBlockDescription(), lastSample);
  }

  private String getNextBlockDescription() {
    return getBlockDescription(++m_numBlocksCreated);
  }

  private String getBlockDescription(int blockNumber) {
    return m_description + " Block #" + blockNumber;
  }

  @Override
  public void addSample(Sample sample) {
    Optional<List<Sample>> overflow = m_currentSteimBlock.addSample(sample);

    if (!overflow.isPresent()) {
      return;
    }

    SteimBlock returnBlock = getCompletedBlockAndAdvanceCurrent();
    notifyBlocksComplete(Collections.singletonList(returnBlock));
    for (Sample overflowSample : overflow.get()) {
      addSampleWithNoOverflow(overflowSample);
    }
  }

  @Override
  public boolean addObserver(SteimBlockObserver observer) {
    return m_observers.add(observer);
  }

  @Override
  public boolean removeObserver(SteimBlockObserver observer) {
    return m_observers.remove(observer);
  }

  @Override
  public boolean flush(Sample initializingSample) {
    if (m_currentSteimBlock.isEmpty()) {
      advanceCurrentBlock(initializingSample);
      return false;
    }

    List<SteimBlock> completedBlocks = new ArrayList<>(2);

    Optional<Sample> overflow = m_currentSteimBlock.forceComplete();
    completedBlocks.add(m_currentSteimBlock);

    if (overflow.isPresent()) {
      advanceCurrentBlock(m_currentSteimBlock.getLastSample().orElse(null));
      // Essentially create a new block and add a single sample to that block
      m_currentSteimBlock.addSample(overflow.get());
      // Guaranteed to not overflow, as this is a new block
      m_currentSteimBlock.forceComplete();
      completedBlocks.add(m_currentSteimBlock);
    }

    advanceCurrentBlock(initializingSample);
    notifyBlocksComplete(completedBlocks);
    return true;
  }

  @Override
  public void setNumFramesPerBlockAndFlush(int numFramesPerBlock) {
    m_blockProvider.setNumFramesPerBlock(numFramesPerBlock);
    flush(null);
  }

  private void advanceCurrentBlock(Sample initializingSample) {
    m_currentSteimBlock =
        m_blockProvider.getBlock(
            getNextBlockDescription(),
            initializingSample == null ? NO_PREVIOUS_SAMPLE : initializingSample);
  }

  private void notifyBlocksComplete(List<SteimBlock> steimBlocks) {
    for (SteimBlock steimBlock : steimBlocks) {
      for (SteimBlockObserver observer : m_observers) {
        observer.steimBlockComplete(steimBlock);
      }
    }
  }

  private SteimBlock getCompletedBlockAndAdvanceCurrent() {
    SteimBlock returnBlock = m_currentSteimBlock;
    m_currentSteimBlock =
        m_blockProvider.getBlock(getNextBlockDescription(), returnBlock.getLastSample().get());
    return returnBlock;
  }

  /**
   * The maximum possible overflow is a 1 byte difference followed by a 4 byte difference, so adding
   * after creating a new block is 'safe'. This precludes the possibility of a block containing a
   * single 1-word frame.
   */
  private void addSampleWithNoOverflow(Sample overflowSample) {
    m_currentSteimBlock.addSample(overflowSample);
  }

  @Override
  public boolean isCurrentBlockEmpty() {
    return m_currentSteimBlock.isEmpty();
  }

  @Override
  public int getCurrentBlockPercentFull() {
    return m_currentSteimBlock.getPercentFull();
  }

  public SteimBlock getCurrentBlock() {
    return m_currentSteimBlock;
  }
}
