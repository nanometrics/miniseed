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

public class Steim1DataFrameProvider {

  private static final int NUMBER_ELEMENTS_PER_FRAME = 16;
  private static final int NUMBER_BYTES_PER_FRAME = NUMBER_ELEMENTS_PER_FRAME * 4;

  private final Steim1WordProvider m_wordProvider = new Steim1WordProvider();

  public SteimDataFrame getFrame(String description, Sample lastSample) {
    return new Steim1DataFrame(description, m_wordProvider, lastSample);
  }

  public Steim1FirstDataFrame getFirstDataFrame(
      String description, Sample lastSampleFromPreviousFrame) {
    return new Steim1FirstDataFrame(description, m_wordProvider, lastSampleFromPreviousFrame);
  }

  static final Sample EMPTY_SAMPLE = new Sample(0);
  static final String CANNOT_PERFORM_OPERATION_ON_EMPTY_FRAME =
      "Cannot perform operation on empty frame";
  static final SteimDataFrame FILLER_FULL_FRAME_WITH_NO_SAMPLES =
      new SteimDataFrame() {

        @Override
        public byte[] toByteArray() {
          return new byte[NUMBER_BYTES_PER_FRAME];
        }

        @Override
        public boolean isFull() {
          return true;
        }

        @Override
        public int getPercentFull() {
          return 100;
        }

        @Override
        public boolean isEmpty() {
          return true;
        }

        @Override
        public int getNumSamples() {
          return 0;
        }

        @Override
        public Optional<Sample> getLastSample() {
          return Optional.empty();
        }

        @Override
        public Optional<List<Sample>> addSample(Sample sample) {
          throw new UnsupportedOperationException(CANNOT_PERFORM_OPERATION_ON_EMPTY_FRAME);
        }

        @Override
        public Optional<Sample> forceComplete() {
          throw new UnsupportedOperationException(CANNOT_PERFORM_OPERATION_ON_EMPTY_FRAME);
        }

        @Override
        public SteimWord getLastNonEmptyWord() {
          throw new UnsupportedOperationException(CANNOT_PERFORM_OPERATION_ON_EMPTY_FRAME);
        }
      };
}
