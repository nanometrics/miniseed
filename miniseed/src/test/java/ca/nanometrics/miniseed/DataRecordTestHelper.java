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

import ca.nanometrics.miniseed.encoding.DataEncoding;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;

public class DataRecordTestHelper {
  private static final OkHttpClient OKHTTP =
      new OkHttpClient.Builder()
          .cache(new Cache(new File("target/referenceData/"), 20 * 1024 * 1024))
          .build();

  public static InputStream getInputStream(String url) throws IOException {
    try (ResponseBody body =
        OKHTTP.newCall(new Request.Builder().url(url).build()).execute().body()) {
      return body.byteStream();
    }
  }

  public static byte[] getBytes(String url) throws IOException {
    try (ResponseBody body =
        OKHTTP.newCall(new Request.Builder().url(url).build()).execute().body()) {
      return body.bytes();
    }
  }

  public static String getString(String url) throws IOException {
    try (ResponseBody body =
        OKHTTP.newCall(new Request.Builder().url(url).build()).execute().body()) {
      return body.string();
    }
  }

  public static record ReferenceData(
      String miniSeedUrl, String jsonUrl, Samples.Type samplesType, DataEncoding encoding) {
    public ReferenceData(String url, Samples.Type samplesType, DataEncoding encoding) {
      this(url, null, samplesType, encoding);
    }

    @Override
    public String toString() {
      return miniSeedUrl.substring(miniSeedUrl.lastIndexOf('/') + 1);
    }
  }
}
