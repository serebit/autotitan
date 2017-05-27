# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

### Added
- All other types of event listeners.
- `Audio::pause`, a command that pauses the currently playing track.
- `Audio::resume`, a command that resumes the currently playing track if it is paused.
- `General::serverList`, a command that returns an embed with the servers that the bot is connected to.

### Changed
- Most audio commands now require the command invoker to be in the same voice channel as the bot. (Thanks, Emu.)
- Renamed `Owner::rename` to `Owner::setName`.
- Renamed `Owner::invite` to `Owner::getInvite`.
- Renamed `Audio::volume` to `Owner::setVolume`.