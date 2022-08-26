package ca.nanometrics.miniseed.v3;

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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.IntPredicate;
import javax.annotation.Nullable;

/**
 * As defined at <a href="http://docs.fdsn.org/projects/source-identifiers/en/v1.0/index.html">FDSN
 * Source Identifiers</a>
 */
public record UriSourceIdentifier(
    String network,
    String station,
    @Nullable String location,
    String band,
    String source,
    String subsource)
    implements SourceIdentifier {

  public static final String SCHEME = "FDSN";
  public static final String EMPTY = "";

  @Override
  public String channel() {
    return String.format("%s_%s_%s", band, source, subsource);
  }

  @Override
  public String toString() {
    return String.format(
        "%s:%s_%s_%s_%s_%s_%s",
        SCHEME,
        network(),
        station(),
        location() == null ? EMPTY : location(),
        band(),
        source(),
        subsource());
  }

  public static Builder builder() {
    return new AutoBuilder_UriSourceIdentifier_Builder().location(EMPTY);
  }

  @AutoBuilder
  public abstract static class Builder {
    private static final IntPredicate UPPERCASE = c -> (c >= 65 && c <= 90);
    private static final IntPredicate NUMERIC = c -> (c >= 48 && c <= 57);
    private static final IntPredicate DASH = c -> c == 45;

    public UriSourceIdentifier uri(String uri) throws URISyntaxException {
      return uri(new URI(uri));
    }

    public UriSourceIdentifier uri(URI uri) {
      if (!uri.getScheme().equals(SCHEME)) {
        throw new IllegalArgumentException("Invalid scheme: " + uri.getScheme());
      }
      String path = uri.getSchemeSpecificPart();
      String[] split = path.split("_");
      if (split.length != 6) {
        throw new IllegalArgumentException("Invalid source identifier path: " + path);
      }
      network(split[0]);
      station(split[1]);
      location(split[2]);
      band(split[3]);
      source(split[4]);
      subsource(split[5]);
      return build();
    }

    /**
     * @return true if all characters are upper case ASCII [A-Z] or Numeric [0-9] or "-" (dash).
     */
    boolean isAllowedAscii(String string) {
      return string.chars().allMatch(UPPERCASE.or(NUMERIC).or(DASH));
    }

    public abstract Builder network(String network);

    public abstract Builder station(String station);

    public abstract Builder location(String location);

    abstract String location();

    /**
     * @param channel a legacy 3-character channel code
     */
    public Builder channel(String channel) {
      if (channel.length() != 3) {
        throw new IllegalArgumentException("Expected channel code: " + channel);
      }
      band(channel.substring(0, 1));
      source(channel.substring(1, 2));
      subsource(channel.substring(2, 3));
      return this;
    }

    public abstract Builder band(String band);

    public abstract Builder source(String source);

    public abstract Builder subsource(String subsource);

    public abstract UriSourceIdentifier autoBuild();

    public UriSourceIdentifier build() {
      String location = location();
      if (location != null && location.isEmpty()) {
        location(null);
      }
      return autoBuild();
    }
  }
}
