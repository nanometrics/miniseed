package ca.nanometrics.miniseed.util;

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

import java.util.Arrays;

/** A simple wrapper of byte[] to handle equals on records that need to hold a byte[] */
public record ByteArray(byte[] byteArray) {

  @Override
  public int hashCode() {
    return Arrays.hashCode(byteArray);
  }

  @Override
  public boolean equals(final Object obj) {
    return obj == this
        || (obj.getClass().equals(ByteArray.class)
            && Arrays.equals(byteArray, ((ByteArray) obj).byteArray));
  }

  public int length() {
    return byteArray.length;
  }
}
