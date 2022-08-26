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

public enum Steim1WordState {
  INITIAL {
    @Override
    Steim1WordState nextState(int numBytes) {
      switch (numBytes) {
        case 1:
          return ONE_ONE_BYTE_DIFFERENCE;
        case 2:
          return ONE_TWO_BYTE_DIFFERENCE;
        case 4:
          return RETURN;
        default:
          throw new IllegalArgumentException();
      }
    }
  },
  ONE_ONE_BYTE_DIFFERENCE {
    @Override
    Steim1WordState nextState(int numBytes) {
      switch (numBytes) {
        case 1:
          return TWO_ONE_BYTE_DIFFERENCES;
        case 2:
          return RETURN;
        case 4:
          return OVERFLOW;
        default:
          throw new IllegalArgumentException();
      }
    }
  },
  ONE_TWO_BYTE_DIFFERENCE {
    @Override
    Steim1WordState nextState(int numBytes) {
      switch (numBytes) {
        case 1:
        case 2:
          return RETURN;
        case 4:
          return OVERFLOW;
        default:
          throw new IllegalArgumentException();
      }
    }
  },
  TWO_ONE_BYTE_DIFFERENCES {
    @Override
    Steim1WordState nextState(int numBytes) {
      switch (numBytes) {
        case 1:
          return THREE_ONE_BYTE_DIFFERENCES;
        case 2:
        case 4:
          return OVERFLOW;
        default:
          throw new IllegalArgumentException();
      }
    }
  },
  THREE_ONE_BYTE_DIFFERENCES {
    @Override
    Steim1WordState nextState(int numBytes) {
      switch (numBytes) {
        case 1:
          return RETURN;
        case 2:
        case 4:
          return OVERFLOW;
        default:
          throw new IllegalArgumentException();
      }
    }
  },
  OVERFLOW {
    @Override
    Steim1WordState nextState(int numBytes) {
      return OVERFLOW;
    }
  },
  RETURN {
    @Override
    Steim1WordState nextState(int numBytes) {
      return OVERFLOW;
    }
  };

  abstract Steim1WordState nextState(int numBytes);
}
