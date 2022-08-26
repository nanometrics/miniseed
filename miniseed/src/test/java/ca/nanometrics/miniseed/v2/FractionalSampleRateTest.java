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

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import ca.nanometrics.miniseed.*;
import org.junit.jupiter.api.*;

class FractionalSampleRateTest {
  private static final double DELTA = 0.00001;

  @Test
  public void testIllegalSampleRate() throws Exception {
    assertThrows(IllegalStateException.class, () -> FractionalSampleRate.get(0));
    assertThrows(IllegalStateException.class, () -> FractionalSampleRate.get(0, 0));
    assertThrows(IllegalStateException.class, () -> FractionalSampleRate.get(0, 127));
    assertThrows(IllegalStateException.class, () -> FractionalSampleRate.get(127, 0));
    assertThrows(IllegalStateException.class, () -> FractionalSampleRate.get(0, 128));
    assertThrows(IllegalStateException.class, () -> FractionalSampleRate.get(128, 0));
  }

  @Test
  public void testNegatives() throws Exception {
    SampleRate rate = FractionalSampleRate.get(33, 10);
    assertEquals(330, rate.sampleRateDouble());
    assertEquals(330, rate.sampleRateDouble(), DELTA);

    rate = FractionalSampleRate.get(336, -10);
    assertEquals(33.6, rate.sampleRateDouble(), DELTA);

    rate = FractionalSampleRate.get(-60, 1);
    assertEquals(0.0166666, rate.sampleRateDouble(), DELTA);

    rate = FractionalSampleRate.get(1, -10);
    assertEquals(0.1, rate.sampleRateDouble(), DELTA);
    rate = FractionalSampleRate.get(-10, 1);
    assertEquals(0.1, rate.sampleRateDouble(), DELTA);
    rate = FractionalSampleRate.get(-1, -10);
    assertEquals(0.1, rate.sampleRateDouble(), DELTA);
  }

  @Test
  public void testLoad() throws Exception {
    byte[] bytes = {5, -5};
    SampleRate rate = FractionalSampleRate.builder().encoding(bytes, 0).build();
    assertEquals(1, rate.sampleRateDouble());
    bytes[1] = 5;
    rate = FractionalSampleRate.builder().encoding(bytes, 0).build();
    assertEquals(25, rate.sampleRateDouble());
    bytes[0] = -5;
    rate = FractionalSampleRate.builder().encoding(bytes, 0).build();
    assertEquals(1, rate.sampleRateDouble());
    bytes[1] = -5;
    rate = FractionalSampleRate.builder().encoding(bytes, 0).build();
    assertEquals(0.04, rate.sampleRateDouble(), DELTA);
  }

  @Test
  public void testBiggerThanOneByteFactorOrMultiplier() {
    V2SampleRate rate = FractionalSampleRate.get(32760, -819);
    rate = FractionalSampleRate.builder().encoding(rate.encoding()).build();
    assertEquals(40, rate.sampleRateInt());

    rate = FractionalSampleRate.get(500, -5);
    rate = FractionalSampleRate.builder().encoding(rate.encoding()).build();
    assertEquals(100, rate.sampleRateInt());

    rate = FractionalSampleRate.get(250, -5);
    rate = FractionalSampleRate.builder().encoding(rate.encoding()).build();
    assertEquals(50, rate.sampleRateInt());

    rate = FractionalSampleRate.get(250, -500);
    rate = Float32SampleRate.get(rate.encoding());
    assertEquals(0.5, rate.sampleRateDouble(), DELTA);

    rate = FractionalSampleRate.get(32000, -819);
    rate = Float32SampleRate.get(rate.encoding());
    assertEquals(39.072040, rate.sampleRateDouble(), DELTA);
  }

  @Test
  public void testFraction() {
    SampleRate rate = FractionalSampleRate.get(4, -10);
    assertThat(rate.sampleRateDouble(), is(0.4d));
    assertThat(((FractionalSampleRate) rate).numerator(), is(4));
    assertThat(((FractionalSampleRate) rate).denominator(), is(10));

    rate = FractionalSampleRate.get(-4, -10);
    assertThat(rate.sampleRateDouble(), is(0.025d));
    assertThat(((FractionalSampleRate) rate).numerator(), is(1));
    assertThat(((FractionalSampleRate) rate).denominator(), is(40));

    rate = FractionalSampleRate.get(-4, 10);
    assertThat(rate.sampleRateDouble(), is(2.5d));
    assertThat(((FractionalSampleRate) rate).numerator(), is(10));
    assertThat(((FractionalSampleRate) rate).denominator(), is(4));

    rate = FractionalSampleRate.get(2, -5);
    assertThat(rate.sampleRateDouble(), is(0.4d));
    assertThat(((FractionalSampleRate) rate).numerator(), is(2));
    assertThat(((FractionalSampleRate) rate).denominator(), is(5));

    rate = FractionalSampleRate.get(100);
    assertThat(rate.sampleRateInt(), is(100));
    assertThat(((FractionalSampleRate) rate).numerator(), is(100));
    assertThat(((FractionalSampleRate) rate).denominator(), is(1));
  }

  @Test
  public void testGetFromPeriod() {
    assertFractionalSampleRateFromPeriod(1);
    assertFractionalSampleRateFromPeriod(5);
    assertFractionalSampleRateFromPeriod(60);
    assertFractionalSampleRateFromPeriod(300);
    assertFractionalSampleRateFromPeriod(3600);

    assertThrows(
        IllegalArgumentException.class, () -> FractionalSampleRate.getFromPeriod(24 * 3600));
  }

  private void assertFractionalSampleRateFromPeriod(int periodSeconds) {
    SampleRate sampleRate = FractionalSampleRate.getFromPeriod(periodSeconds);
    String label = periodSeconds + " - " + sampleRate.toString();
    assertThat(label, sampleRate.sampleRateDouble(), is(1d / periodSeconds));
  }

  @Test
  public void testGetFromPeriodZero() {
    assertThrows(IllegalArgumentException.class, () -> FractionalSampleRate.getFromPeriod(0));
  }

  @Test
  public void testGetFromPeriodNegative() {
    assertThrows(IllegalArgumentException.class, () -> FractionalSampleRate.getFromPeriod(-1));
  }

  @Test
  public void testSampleRateDouble() {
    assertThat(FractionalSampleRate.get(10, 100).sampleRateDouble(), is(1000.));
    assertThat(FractionalSampleRate.get(-10, 100).sampleRateDouble(), is(10.));
    assertThat(FractionalSampleRate.get(10, -100).sampleRateDouble(), is(.1));
    assertThat(FractionalSampleRate.get(-10, -100).sampleRateDouble(), is(.001));
    assertThat(FractionalSampleRate.get(100, 10).sampleRateDouble(), is(1000.));
    assertThat(FractionalSampleRate.get(-100, 10).sampleRateDouble(), is(.1));
    assertThat(FractionalSampleRate.get(100, -10).sampleRateDouble(), is(10.));
    assertThat(FractionalSampleRate.get(-100, -10).sampleRateDouble(), is(.001));
  }

  @Test
  public void test188() {
    SampleRate rate1 = FractionalSampleRate.get(1, 188);
    assertThat(((FractionalSampleRate) rate1).factor(), is(94));
    assertThat(((FractionalSampleRate) rate1).multiplier(), is(2));
    SampleRate rate2 = FractionalSampleRate.get(188, 1);
    assertThat(rate1, is(rate2));
    SampleRate rate3 = FractionalSampleRate.get(188);
    assertThat(rate1, is(rate3));
  }
}
