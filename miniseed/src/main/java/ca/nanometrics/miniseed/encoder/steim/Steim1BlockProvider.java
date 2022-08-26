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

public class Steim1BlockProvider {
  private static final String INVALID_NUM_FRAMES_FORMAT_STRING =
      "Cannot use %d number of frames per block, must be in [%d,%d]";
  private int m_numFramesPerBlock;
  private final Steim1DataFrameProvider m_frameProvider = new Steim1DataFrameProvider();

  public Steim1BlockProvider(int numFramesPerBlock) {
    setNumFramesPerBlock(numFramesPerBlock);
  }

  public SteimBlock getBlock(String description, Sample lastSample) {
    return new Steim1Block(description, m_frameProvider, lastSample, m_numFramesPerBlock);
  }

  public void setNumFramesPerBlock(int numFramesPerBlock) {
    if (!(numFramesPerBlock >= Steim1Block.MIN_NUM_FRAMES
        && numFramesPerBlock <= Steim1Block.MAX_NUM_FRAMES)) {
      throw new IllegalArgumentException(
          String.format(
              INVALID_NUM_FRAMES_FORMAT_STRING,
              numFramesPerBlock,
              Steim1Block.MIN_NUM_FRAMES,
              Steim1Block.MAX_NUM_FRAMES));
    }
    m_numFramesPerBlock = numFramesPerBlock;
  }
}
