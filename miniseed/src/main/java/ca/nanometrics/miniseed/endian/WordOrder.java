package ca.nanometrics.miniseed.endian;

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

public enum WordOrder {
  LITTLE_ENDIAN(0, LittleEndian.get()),
  BIG_ENDIAN(1, BigEndian.get());
  private final int m_code;
  private final Endian m_reader;

  WordOrder(int code, final Endian reader) {
    m_code = code;
    m_reader = reader;
  }

  public int code() {
    return m_code;
  }

  public Endian reader() {
    return m_reader;
  }

  public static WordOrder fromCode(int code) {
    for (WordOrder value : values()) {
      if (value.m_code == code) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unknown word order code: " + code);
  }
}
