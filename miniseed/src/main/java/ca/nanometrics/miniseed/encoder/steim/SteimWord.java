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

public interface SteimWord {
  /**
   * @return If word is not full after adding the sample return Optional.empty(). If adding the
   *     sample caused the word to be full, or the word was full to begin with, return an int array
   *     containing any overflow values.
   */
  Optional<List<Sample>> addSample(Sample sample);

  boolean isFull();

  boolean isEmpty();

  /**
   * @return - The last sample encoded in this word, useful for initializing the next word. This
   *     function is invalid if the word is not full, as the last sample won't have been established
   *     yet. Returns Optional.empty() if word has been padded with empty samples.
   */
  Optional<Sample> getLastSample();

  int getNumSamples();

  byte[] toByteArray();

  byte getTwoBitNibbleCode();

  Sample getFirstSample();

  /**
   * Forces the word to complete, padding itself as necessary.
   *
   * @return If there is an overflow, return the overflow value, otherwise return Optional.empty()
   */
  Optional<Sample> forceComplete();

  Sample getLastNonEmptySample();
}
