package ca.nanometrics.miniseed.msx;

/*-
 * #%L
 * msx
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

import ca.nanometrics.miniseed.msx.convert.MiniSeed2To3;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "msx", //
    description = {"Miniseed utility.%n"}, //
    optionListHeading = "Options:%n", //
    mixinStandardHelpOptions = true,
    sortOptions = false,
    versionProvider = MsxVersionProvider.class,
    subcommands = {MiniSeed2To3.class})
public class Msx {

  @Option(
      paramLabel = "level",
      names = {"-l", "--level"},
      description = {"Log level", "Valid values: ${COMPLETION-CANDIDATES}"},
      defaultValue = "INFO",
      showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
  private void setLogLevel(LogLevel value) {
    setRootLevel(value.level());
  }

  private static void setRootLevel(Level level) {
    Logger root = LogManager.getLogManager().getLogger("");
    root.setLevel(level);
    for (Handler handler : root.getHandlers()) {
      handler.setLevel(level);
    }
  }

  public static void configureLogger() {
    try (InputStream stream = Msx.class.getResourceAsStream("/logging.properties")) {
      LogManager.getLogManager().readConfiguration(stream);
    } catch (IOException | SecurityException | ExceptionInInitializerError ex) {
      Logger.getLogger(Msx.class.getName())
          .log(Level.SEVERE, "Failed to read logging.properties file", ex);
    }
  }

  public static org.slf4j.Logger log() {
    return org.slf4j.LoggerFactory.getLogger(Msx.class);
  }

  public static void main(String[] args) {
    configureLogger();
    int exitCode = new CommandLine(new Msx()).execute(args);
    System.exit(exitCode);
  }

  enum LogLevel {
    INFO(Level.INFO),
    WARNING(Level.WARNING),
    ERROR(Level.SEVERE),
    DEBUG(Level.FINER),
    TRACE(Level.FINEST);

    private final Level m_level;

    LogLevel(Level level) {
      m_level = level;
    }

    public Level level() {
      return m_level;
    }
  }
}
