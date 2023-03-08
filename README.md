# miniSEED

![Maven Central](https://img.shields.io/maven-central/v/ca.nanometrics/miniseed?style=plastic)

A Java library and command-line tool, *msx*, for handling miniSEED data, in both
[Version 2.4](http://www.fdsn.org/pdf/SEEDManual_V2.4.pdf)
and [Version 3](https://miniseed3.readthedocs.io/en/latest/).

Note that running *msx* does not require having Java installed, and can be run on Windows, Mac and
Linux.

For details on the library see [miniseed](miniseed/README.md).
For details on *msx*, the command-line tools, see [msx](msx/README.md).

## Building miniSEED

See [the CONTRIBUTING.md docs](CONTRIBUTING.md#building-miniseed).

## Releasing this library

To create a release of this project (library and msx), do the following:

(Note: Only project maintainers can perform the release.)

* On a local clone:
    * Remove snapshot from the version, setting the desired release version:
        * `mvn versions:set -DnewVersion=1.0.0`
    * Ensure CHANGELOG.md is updated with details of the new version
    * Commit the changes
    * Create a git tag with the version as the tag name
      * `git tag 1.0.0`
      * `git push --tags`
      * `git push origin main`
    * Bump the version to the next snapshot version, and push.
      * `mvn versions:set -DnewVersion=1.0.1-SNAPSHOT`
      * `git push`
* When you push the tag, the release.yml workflow will automatically run in GitHub Actions:
  * The workflow will:
      * Build the library and msx
      * Publish the library to Maven Central
      * Publish the msx to GitHub Releases
```

## License

```

    Copyright 2022-2023 Nanometrics

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

```
