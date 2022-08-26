package ca.nanometrics.miniseed.v2;

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

import ca.nanometrics.miniseed.SourceIdentifier;
import com.google.auto.value.AutoBuilder;
import javax.annotation.Nullable;

public record Scnl(String network, String station, @Nullable String location, String channel)
    implements SourceIdentifier {
  static final char SEPARATOR = '.';
  static final String EMPTY_LOCATION = "--";

  @Override
  public String band() {
    return channel.substring(0, 1);
  }

  @Override
  public String source() {
    return channel.substring(1, 2);
  }

  @Override
  public String subsource() {
    return channel.substring(2, 3);
  }

  boolean hasLocationCode() {
    return location != null;
  }

  @Override
  public String toString() {
    StringBuilder path = new StringBuilder();
    path.append(network);
    path.append(SEPARATOR);
    path.append(station);
    if (hasLocationCode()) {
      path.append(SEPARATOR);
      path.append(location);
    }
    path.append(SEPARATOR);
    path.append(channel);
    return path.toString();
  }

  public static Scnl build(String scnl) {
    return builder().parse(scnl).build();
  }

  public static Builder builder() {
    return new AutoBuilder_Scnl_Builder().location("");
  }

  @AutoBuilder
  public abstract static class Builder {

    public Builder parse(String scnl) {
      String[] split = scnl.split("\\" + SEPARATOR);
      if (split.length < 3 || split.length > 4) {
        throw new IllegalArgumentException("Invalid scnl:" + scnl);
      }
      network(split[0]);
      station(split[1]);
      if (split.length == 4) {
        location(split[2]);
        channel(split[3]);
      } else {
        location(null);
        channel(split[2]);
      }
      return this;
    }

    public abstract Builder network(String network);

    public abstract String network();

    public abstract Builder station(String station);

    public abstract String station();

    public abstract Builder location(@Nullable String location);

    public abstract String location();

    public abstract Builder channel(String channel);

    public abstract String channel();

    public abstract Scnl autoBuild();

    public Scnl build() {
      validateMinMaxLengthOfCode("network", network(), 1, 2);
      validateMinMaxLengthOfCode("station", station(), 1, 5);
      String location = location();
      if (location != null) {
        if (location.equals(EMPTY_LOCATION) || location.isEmpty()) {
          location(null);
        } else {
          validateMinMaxLengthOfCode("location", location, 1, 2);
        }
      }
      validateMinMaxLengthOfCode("channel", channel(), 1, 3);
      return autoBuild();
    }

    private static void validateMinMaxLengthOfCode(
        final String codeName, final String code, final int minLength, final int maxLength) {
      if (code == null || !isLengthGood(code, minLength, maxLength)) {
        throw new IllegalArgumentException(
            minLength == maxLength
                ? String.format(
                    "Invalid length of %s (length must be %d): %s", codeName, minLength, code)
                : String.format(
                    "Invalid length of %s (length must be between %d and %d): %s",
                    codeName, minLength, maxLength, code));
      }
    }

    private static boolean isLengthGood(String code, int minLength, int maxLength) {
      int codeLength = code == null ? 0 : code.length();
      return codeLength >= minLength && codeLength <= maxLength;
    }
  }
}
