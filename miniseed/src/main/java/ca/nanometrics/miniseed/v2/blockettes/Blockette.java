package ca.nanometrics.miniseed.v2.blockettes;

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

import ca.nanometrics.miniseed.endian.Endian;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;

public sealed interface Blockette
    permits DataOnlyBlockette_1000, DataExtensionBlockette_1001, SampleRateBlockette_100 {
  Map<Integer, Supplier<Loader>> LOADERS =
      Map.of(
          SampleRateBlockette_100.TYPE, () -> new SampleRateBlockette_100.Loader(),
          DataOnlyBlockette_1000.TYPE, () -> new DataOnlyBlockette_1000.Loader(),
          DataExtensionBlockette_1001.TYPE, () -> new DataExtensionBlockette_1001.Loader());

  int blocketteType();

  int nextBlocketteOffset();

  int length();

  byte[] toByteArray(Endian endian);

  interface Loader {
    /**
     * @param input an input stream positioned after the next blockette offset field
     * @param endian
     */
    Blockette load(int blocketteType, int nextBlocketteOffset, InputStream input, Endian endian)
        throws IOException;
  }
}
