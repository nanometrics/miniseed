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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ScnlTest {
  @Test
  public void testBuilder() {
    assertThrows(IllegalStateException.class, () -> Scnl.builder().build());
    assertThrows(IllegalStateException.class, () -> Scnl.builder().network("NX").build());

    Scnl scnl = Scnl.build("NX.STN.HHZ");
    assertThat(scnl.network(), is("NX"));
    assertThat(scnl.station(), is("STN"));
    assertThat(scnl.channel(), is("HHZ"));
  }

  @Test
  public void testWithLocation() {
    Scnl identifier =
        Scnl.builder().network("NX").station("STN1").location("00").channel("BHZ").build();
    assertThat(identifier.network(), is("NX"));
    assertThat(identifier.station(), is("STN1"));
    assertThat(identifier.location(), is("00"));
    assertThat(identifier.band(), is("B"));
    assertThat(identifier.source(), is("H"));
    assertThat(identifier.subsource(), is("Z"));
    assertThat(identifier.toString(), is("NX.STN1.00.BHZ"));
  }

  @Test
  public void testNoLocation() {
    Scnl identifier = Scnl.builder().network("NX").station("STN1").channel("BHZ").build();
    assertThat(identifier.network(), is("NX"));
    assertThat(identifier.station(), is("STN1"));
    assertThat(identifier.location(), is(nullValue()));
    assertThat(identifier.band(), is("B"));
    assertThat(identifier.source(), is("H"));
    assertThat(identifier.subsource(), is("Z"));
    assertThat(identifier.toString(), is("NX.STN1.BHZ"));
  }

  @Test
  public void testInvalidNetwork() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Scnl.builder().network("NXN").station("STN1").channel("BHZ").build());
  }

  @Test
  public void testInvalidStation() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Scnl.builder().network("NXX").station("STN1").channel("BHZ").build());
  }
}
