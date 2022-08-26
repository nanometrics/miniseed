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

public class Steim1WordProvider {

  static final String CANNOT_PERFORM_OPERATION_ON_EMPTY_WORD =
      "Cannot perform operation on empty frame";

  static final SteimWord EMPTY_STEIM_WORD =
      new SteimWord() {

        @Override
        public Optional<List<Sample>> addSample(Sample sample) {
          throw new UnsupportedOperationException(CANNOT_PERFORM_OPERATION_ON_EMPTY_WORD);
        }

        @Override
        public boolean isFull() {
          return true;
        }

        @Override
        public boolean isEmpty() {
          return true;
        }

        @Override
        public Optional<Sample> getLastSample() {
          return Optional.empty();
        }

        @Override
        public int getNumSamples() {
          return 0;
        }

        @Override
        public byte[] toByteArray() {
          return new byte[Steim1DataWord.STEIM_ONE_MAX_DIFFERENCE_WIDTH];
        }

        @Override
        public byte getTwoBitNibbleCode() {
          return 0;
        }

        @Override
        public Sample getFirstSample() {
          throw new UnsupportedOperationException(CANNOT_PERFORM_OPERATION_ON_EMPTY_WORD);
        }

        @Override
        public Optional<Sample> forceComplete() {
          throw new UnsupportedOperationException(CANNOT_PERFORM_OPERATION_ON_EMPTY_WORD);
        }

        @Override
        public Sample getLastNonEmptySample() {
          throw new UnsupportedOperationException(CANNOT_PERFORM_OPERATION_ON_EMPTY_WORD);
        }
      };

  public SteimWord getWord(Sample lastSample) {
    return new Steim1DataWord(lastSample);
  }
}
