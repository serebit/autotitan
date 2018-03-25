# AutoTitan
[![Discord Server](https://discordapp.com/api/guilds/279777865434660865/widget.png?style=shield)](https://goo.gl/RGvvbM)
[![Build Status](https://travis-ci.org/serebit/autotitan.svg?branch=master)](https://goo.gl/0Gm2gy)
[![Donate](https://img.shields.io/badge/Donate-PayPal-blue.svg)](https://goo.gl/OWpJxJ)

AutoTitan is a modular, self-hosted [Discord](https://discordapp.com) bot built in Kotlin/JVM using the 
[Java Discord API](https://github.com/DV8FromTheWorld/JDA). 

## System Requirements
AutoTitan requires **Java 8** or newer to run.

## Get Started
AutoTitan is not yet available for download as a standalone executable, but if you'd like to build from source, feel
 free to do so using the following instructions. Make sure the current build has passed integration tests first by
 checking the `build` badge at the top of the README!

1. [Install Gradle if you don't have it installed already.](https://gradle.org/install) **Gradle 3.5** or newer is
 required to build AutoTitan. 
2. `git clone` a fresh copy of the AutoTitan repository. 
3. Navigate to the directory you cloned AutoTitan into, and using the command line, run `gradle build`.

If all goes to plan, your newly compiled AutoTitan executable will be resting comfortably in `build/libs/`.

## Dependencies
All dependencies are managed by Gradle. See `build.gradle` for the full list of dependencies.
