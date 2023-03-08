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

import ca.nanometrics.miniseed.v2.DataRecord2;
import ca.nanometrics.miniseed.v3.DataRecord3;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class MiniSeed {

  /**
   * @return a stream of DataRecords read from the given input file.
   * @throws IOException if the given file does not exist or cannot be read.
   */
  public static Stream<DataRecord> stream(File file) throws IOException {
    return stream(new BufferedInputStream(new FileInputStream(file)));
  }

  /**
   * The input stream will be automatically closed when the stream is closed.
   *
   * @return a stream of DataRecords read from the given input stream.
   * @throws IOException if the given stream cannot be read
   */
  public static Stream<DataRecord> stream(InputStream input) throws IOException {
    DataRecordIterator iterator = new DataRecordIterator(input);
    return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                iterator,
                Spliterator.ORDERED
                    & Spliterator.NONNULL
                    & Spliterator.DISTINCT
                    & Spliterator.IMMUTABLE),
            false)
        .onClose(iterator::close);
  }

  /**
   * NOTE: The input stream will not be closed by this method, so the caller must close it when the
   * iteration is complete.
   *
   * @return an iterable of DataRecords loaded from the given input stream
   * @throws IOException if the given stream cannot be read
   */
  public static Iterable<DataRecord> iterable(InputStream input) throws IOException {
    DataRecordIterator iterator = new DataRecordIterator(input);
    return () -> iterator;
  }

  private static class DataRecordIterator implements Iterator<DataRecord> {

    private final InputStream input;
    private final DataRecordReader reader;
    private DataRecord next;

    private DataRecordIterator(InputStream input) throws IOException {
      this.input = input;
      if (DataRecord3.isMiniSeed3(input)) {
        reader = DataRecord3::read;
      } else {
        reader = DataRecord2::read;
      }
    }

    public void close() {
      try {
        input.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public boolean hasNext() {
      try {
        if (next == null && input.available() > 0) {
          next = reader.read(input);
          return true;
        }
      } catch (IOException e) {
        // do nothing
      }
      return next != null;
    }

    @Override
    public DataRecord next() {
      if (next != null || hasNext()) {
        DataRecord record = next;
        next = null;
        return record;
      }
      throw new NoSuchElementException();
    }
  }

  interface DataRecordReader {

    DataRecord read(InputStream input) throws IOException;
  }
}
