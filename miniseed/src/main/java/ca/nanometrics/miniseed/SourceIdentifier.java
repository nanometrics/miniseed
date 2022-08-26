package ca.nanometrics.miniseed;

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

import ca.nanometrics.miniseed.v2.Scnl;
import ca.nanometrics.miniseed.v3.UriSourceIdentifier;
import java.net.URISyntaxException;

public interface SourceIdentifier {
  String network();

  String station();

  /**
   * Location codes are used to logically group channels within a single station deployment. This
   * can be for channels produced by the same sensor, channels produced in a sub-processor, many
   * sensors deployed in a grid or an array, etc.
   */
  String location();

  /**
   * A channel is composed of a sequence of three codes that each describe an aspect of the
   * instrumentation and its digitization, as defined by #band, #source and #subsource.
   */
  String channel();

  /**
   * Indicates the general sampling rate and response band of the data source. May be empty for
   * non-time series data.
   */
  String band();

  /** Identifies an instrument or other general data source. Cannot be empty. */
  String source();

  /**
   * Identifies a sub-category within the source, often the orientation, relative positon, or sensor
   * type. The meaning of subsource codes are specific to the containing source. May be empty.
   */
  String subsource();

  static SourceIdentifier parse(String string) {
    if (string.startsWith(UriSourceIdentifier.SCHEME + ":")) {
      try {
        return UriSourceIdentifier.builder().uri(string);
      } catch (URISyntaxException e) {
        // fall through
      }
    } else {
      try {
        return Scnl.build(string);
      } catch (RuntimeException e) {
        // fall through
      }
    }
    throw new IllegalArgumentException("Unknown source identifier: " + string);
  }
}
