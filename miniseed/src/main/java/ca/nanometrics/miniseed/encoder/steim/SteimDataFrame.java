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
import java.util.List;
import java.util.Optional;

public interface SteimDataFrame {
  /**
   * @return If frame is not full after adding the sample, implying the sample and any overflows
   *     were encoded, return Optional.empty(). If adding the sample caused the frame to be full, or
   *     the frame was full to begin with, return a list containing any overflow values.
   */
  Optional<List<Sample>> addSample(Sample sample);

  boolean isFull();

  boolean isEmpty();

  int getPercentFull();

  /**
   * @return - The last sample encoded in this frame, useful for initializing the next frame. This
   *     function is invalid if the frame is not full, as the last sample won't have been
   *     established yet. Returns Optional.empty() if frame has been padded with empty words.
   */
  Optional<Sample> getLastSample();

  int getNumSamples();

  byte[] toByteArray();

  /**
   * Forces the SteimDataFrame to pad out any missing samples and complete itself
   *
   * @return If there is an overflow, return it, otherwise return Optional.empty()
   */
  Optional<Sample> forceComplete();

  SteimWord getLastNonEmptyWord();
}
