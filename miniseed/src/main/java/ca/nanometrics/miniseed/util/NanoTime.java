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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.concurrent.TimeUnit;

/** Helper functions for dealing with time as epoch nanoseconds. */
public final class NanoTime {

  public static final long NS_PER_SEC = 1_000_000_000L;

  private NanoTime() { // do not instantiate
  }

  public static long nowNanos() {
    return TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
  }

  public static double secondsToNanos(double seconds) {
    return seconds * NS_PER_SEC;
  }

  public static Instant epochNanosToInstant(long epochNanos) {
    long epochSeconds = TimeUnit.NANOSECONDS.toSeconds(epochNanos);
    return Instant.ofEpochSecond(epochSeconds, epochNanos - TimeUnit.SECONDS.toNanos(epochSeconds));
  }

  public static OffsetDateTime epochNanosToDateTime(long epochNanos) {
    return OffsetDateTime.ofInstant(epochNanosToInstant(epochNanos), ZoneOffset.UTC);
  }

  public static long toEpochNanos(TemporalAccessor time) {
    return TimeUnit.SECONDS.toNanos(time.getLong(ChronoField.INSTANT_SECONDS))
        + time.getLong(ChronoField.NANO_OF_SECOND);
  }
}
