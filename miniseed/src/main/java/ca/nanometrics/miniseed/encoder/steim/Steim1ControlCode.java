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

public enum Steim1ControlCode {
  NO_DATA(0, (byte) 0), //
  ONE_BYTE_DATA(1, (byte) 1), //
  TWO_BYTE_DATA(2, (byte) 2), //
  FOUR_BYTE_DATA(4, (byte) 3);

  private final int m_numBytesPerDifference;
  private final byte m_steim1ControlCode;

  Steim1ControlCode(int numBytesPerDifference, byte steim1ControlCode) {
    m_numBytesPerDifference = numBytesPerDifference;
    m_steim1ControlCode = steim1ControlCode;
  }

  public int getNumBytesPerDifference() {
    return m_numBytesPerDifference;
  }

  public byte getTwoBitControlCodeValue() {
    return m_steim1ControlCode;
  }

  public static Steim1ControlCode getControlCodeForNumBytes(int numRequiredBytes) {
    return switch (numRequiredBytes) {
      case 0 -> NO_DATA;
      case 1 -> ONE_BYTE_DATA;
      case 2 -> TWO_BYTE_DATA;
      case 4 -> FOUR_BYTE_DATA;
      default -> throw new IllegalArgumentException(
          "Unexpected number of required bytes: " + numRequiredBytes);
    };
  }
}
