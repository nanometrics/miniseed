# miniSEED


## Usage

To use this library in your Java projects:

### Maven
In a Maven project, include the dagger artifact in the dependencies section of your pom.xml:

```
<dependencies>
  <dependency>
    <groupId>ca.nanometrics</groupId>
    <artifactId>miniseed</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```

### Gradle

```
dependencies {
  implementation 'ca.nanometrics:miniseed:1.0.0'
}
```

## Parsing Data Records

To parse and process a stream of data records from a file, use `MiniSeed.stream(File)`.

For example, a main class that takes a single argument that is a directory path:

```
import ca.nanometrics.miniseed.MiniSeed;

public class Processor {

  public void process(DataRecord record) {
    Samples samples = record.samples();
    OffsetDateTime startTime = record.recordStartTime();
  }

  public static void main(String[] args) {
    File directory = new File(args[0]);
    Processor processor = new Processor();
    Files.walk(file.toPath())
              .filter(Files::isRegularFile)
              .map(Path::toFile)
              .forEach(processor::process);
  }
}
```