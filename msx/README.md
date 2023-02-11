# msx

## Using msx

TODO

## Building msx

To build msx, you must have [GraalVM](https://www.graalvm.org/downloads/) and [native-image](https://www.graalvm.org/22.0/reference-manual/native-image/#install-native-image) installed. The native-image tool will create a platform-specific executable for the platform the build is performed on.

Running `mvn verify` will do the whole process and generate the executable file in `target/deploy`.
