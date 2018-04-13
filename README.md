# AutoTitan
[![Discord Server](https://discordapp.com/api/guilds/279777865434660865/widget.png?style=shield)](https://goo.gl/RGvvbM)
[![Build Status](https://travis-ci.org/serebit/autotitan.svg?branch=master)](https://goo.gl/0Gm2gy)
[![Codacy Grade](https://img.shields.io/codacy/grade/37092324cd0844f198e1a26e6dd17175.svg)](https://app.codacy.com/app/serebit/autotitan)
[![License](https://img.shields.io/github/license/serebit/autotitan.svg)](https://github.com/serebit/autotitan/tree/master/LICENSE.md)
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
