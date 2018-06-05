# AutoTitan
[![Discord Server](https://discordapp.com/api/guilds/279777865434660865/widget.png?style=shield)](https://goo.gl/RGvvbM)
[![Pipeline Status](https://gitlab.com/serebit/autotitan/badges/master/pipeline.svg)](https://gitlab.com/serebit/autotitan/commits/master)
[![Codacy Grade](https://img.shields.io/codacy/grade/4d9ef218ebde4807bb58d6aba7a61772.svg)](https://app.codacy.com/app/serebit/LoggerKt)
[![License](https://img.shields.io/github/license/serebit/loggerkt.svg)](https://github.com/serebit/loggerkt/tree/master/LICENSE.md)
[![Donate](https://img.shields.io/badge/Donate-PayPal-blue.svg)](https://goo.gl/OWpJxJ)

AutoTitan is a versatile self-hosted [Discord](https://discordapp.com) bot built in Kotlin/JVM using the 
[Java Discord API](https://github.com/DV8FromTheWorld/JDA). 

## System Requirements
AutoTitan requires **Java 8** or newer to run, and **JDK 8** to compile. AutoTitan may have issues with non-LTS 
versions of the JDK, as some libraries it depends on behave unpredictably.

## Compiling
```
wget "https://github.com/serebit/autotitan/archive/master.tar.gz"
tar xvzf master.tar.gz
cd autotitan-master
./gradlew shadowJar
```
The compiled executable will be in the `build/libs` folder in the current directory after running this script.

## Dependencies
All dependencies are managed by Gradle. See `build.gradle.kts` for the full list.
