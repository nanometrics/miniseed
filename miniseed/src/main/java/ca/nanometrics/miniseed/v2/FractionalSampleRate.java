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

import ca.nanometrics.miniseed.endian.BigEndian;
import com.google.auto.value.AutoBuilder;

public record FractionalSampleRate(
    int factor,
    int multiplier,
    short shortEncoding,
    int intValue,
    double doubleValue,
    int numerator,
    int denominator)
    implements V2SampleRate {

  @Override
  public byte[] encoding() {
    byte[] bytes = new byte[2];
    BigEndian.get().writeShort(bytes, 0, shortEncoding);
    return bytes;
  }

  @Override
  public double sampleRateDouble() {
    if (isFloatingPoint()) {
      return doubleValue;
    }
    return intValue;
  }

  @Override
  public int sampleRateInt() {
    if (isFloatingPoint()) {
      throw new IllegalStateException(
          "Sample rate cannot be represented as an integer: " + sampleRateDouble());
    }
    return intValue;
  }

  @Override
  public double samplePeriod() {
    return 1.0 / sampleRateDouble();
  }

  @Override
  public long samplePeriodNanos() {
    if (isFloatingPoint()) {
      return (long) (1_000_000_000.0 / doubleValue);
    }
    return 1_000_000_000 / intValue;
  }

  @Override
  public boolean isFloatingPoint() {
    return Math.abs(doubleValue - Double.MIN_VALUE) > 0.0000001;
  }

  public static V2SampleRate get(int factor, int multiplier) {
    return builder().factor(factor).multiplier(multiplier).build();
  }

  public static V2SampleRate get(int sampleRate) {
    return builder().sampleRate(sampleRate).build();
  }

  public static V2SampleRate getFromPeriod(int periodSeconds) {
    if (periodSeconds <= 0) {
      throw new IllegalArgumentException("Illegal period: " + periodSeconds);
    }
    int multiplier = 1;
    int factor = periodSeconds;
    while (factor > Byte.MAX_VALUE && ++multiplier <= Byte.MAX_VALUE) {
      if (periodSeconds % multiplier == 0) {
        factor = periodSeconds / multiplier;
      }
    }
    if (multiplier > Byte.MAX_VALUE) {
      throw new IllegalArgumentException(
          "Cannot represent period " + periodSeconds + " sec as a fractional sample rate");
    }
    return get(-factor, -multiplier);
  }

  public static Builder builder() {
    return new AutoBuilder_FractionalSampleRate_Builder()
        .factor(0)
        .multiplier(0)
        .doubleValue(Double.MIN_VALUE);
  }

  @AutoBuilder
  public abstract static class Builder {

    public abstract Builder factor(int factor);

    abstract int factor();

    public abstract Builder multiplier(int multiplier);

    abstract int multiplier();

    public abstract Builder shortEncoding(short encoding);

    public Builder encoding(byte[] bytes, int index) {
      return shortEncoding(BigEndian.get().readShort(bytes, index));
    }

    public Builder encoding(byte[] bytes) {
      return encoding(bytes, 0);
    }

    abstract short shortEncoding();

    public abstract Builder intValue(int value);

    public abstract Builder doubleValue(double doubleValue);

    public abstract Builder numerator(int numerator);

    public abstract Builder denominator(int denominator);

    Builder sampleRate(int sampleRate) {
      if (sampleRate < Byte.MAX_VALUE) {
        factor(sampleRate);
        multiplier(1);
        return this;
      }
      for (int i = 2; i < Byte.MAX_VALUE; i++) {
        int base = sampleRate / i;
        if (base < Byte.MAX_VALUE && base * i == sampleRate) {
          factor(base);
          multiplier(i);
          return this;
        }
      }
      throw new IllegalArgumentException("Cannot Encode Sample Rate: " + sampleRate);
    }

    public abstract FractionalSampleRate autoBuild();

    private static short calculateEncoding(int factor, int multiplier) {
      return (short) ((factor & 0x00FF) << 8 | multiplier & 0x00FF);
    }

    private static V2SampleRate getBiggerThenOneByteFactorOrMultiplier(int factor, int multiplier) {
      if (factor >= 0 && multiplier >= 0) {
        if (multiplier == 1) {
          return get(factor);
        }
        if (factor == 1) {
          return get(multiplier);
        }
        return Float32SampleRate.get(factor * multiplier);
      }
      if (factor >= 0 && multiplier < 0) {
        if (factor % multiplier == 0) {
          return get(factor / -multiplier);
        }
        return Float32SampleRate.get((float) factor / -multiplier);
      } else if (factor < 0 && multiplier >= 0) {
        if (multiplier % factor == 0) {
          return get(multiplier / -factor);
        }
        return Float32SampleRate.get((float) multiplier / -factor);
      } else {
        return Float32SampleRate.get(1 / ((float) multiplier * factor));
      }
    }

    public V2SampleRate build() {
      int multiplier = multiplier();
      int factor = factor();
      if (multiplier == 0 || factor == 0) {
        short shortEncoding = shortEncoding();
        if (shortEncoding != 0) {
          factor((byte) (shortEncoding >> 8));
          multiplier((byte) shortEncoding);
        }
        multiplier = multiplier();
        factor = factor();
        if (multiplier == 0 || factor == 0) {
          throw new IllegalStateException("Unsupported sample rate 0");
        }
      }

      if ((byte) factor != factor || (byte) multiplier != multiplier) {
        numerator(Integer.MIN_VALUE);
        denominator(Integer.MIN_VALUE);
        if (factor == 0 || multiplier == 0) {
          return get(0);
        }
        return getBiggerThenOneByteFactorOrMultiplier(factor, multiplier);
      }

      if (multiplier > 0 && factor > 0) {
        int value = multiplier * factor;
        intValue(value);
        numerator(value);
        denominator(1);
      } else {
        try {
          if (factor > 0 && multiplier < 0) {
            if (factor % multiplier == 0) {
              int value = factor / -multiplier;
              intValue(value);
              numerator(value);
              denominator(1);
            } else {
              intValue(-1);
              doubleValue((double) factor / -multiplier);
              numerator(factor);
              denominator(-multiplier);
            }
          } else if (factor < 0 && multiplier > 0) {
            if (multiplier % factor == 0) {
              int value = multiplier / -factor;
              intValue(value);
              numerator(value);
              denominator(1);
            } else {
              doubleValue((double) multiplier / -factor);
              intValue(multiplier / -factor);
              numerator(multiplier);
              denominator(-factor);
            }
          } else {
            doubleValue(1 / ((double) multiplier * factor));
            intValue(1 / (multiplier * factor));
            numerator(1);
            denominator(multiplier * factor);
          }
        } catch (ArithmeticException e) {
          throw new IllegalStateException(
              "Unsupported sample rate: " + "Factor: " + factor + " Multiplier: " + multiplier, e);
        }
      }
      shortEncoding(calculateEncoding(factor, multiplier));
      return autoBuild();
    }
  }
}
