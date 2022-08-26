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

public interface SteimEncoder {
  boolean addObserver(SteimBlockObserver observer);

  boolean removeObserver(SteimBlockObserver observer);

  void addSample(Sample sample);

  /**
   * Force the encoder to flush all samples, emitting Steim Blocks as necessary.
   *
   * @param initializingSample - The sample to initialize the new SteimBlock with. If
   *     Optional.empty(), the default value will be used (0)
   * @return true if there was a block to flush, false otherwise
   */
  boolean flush(Sample initializingSample);

  void setNumFramesPerBlockAndFlush(int numFramesPerBlock);

  boolean isCurrentBlockEmpty();

  int getCurrentBlockPercentFull();
}
