# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]
- Fix issue #77 Invalid Class Exceptions in Tape

## [1.4.1] - 2019-05-24

### Fixed
- Improved support for future versions of Firebase

## [1.4.0] - 2019-03-26

### Added
 - Added support for open/delivery acknowledgement webhooks for Authenticated
   Users

## [1.3.0] - 2019-02-28

### Added
 - onNewToken method to MessagingService to allow for easier integration with
   other push notification services

## [1.2.1] - 2019-02-20

### Fixed
 - Sometimes notifications were being marked as delivered with the app being in background.

## [1.2.0] - 2019-02-06

### Added
 - Support for [Authenticated Users](https://docs.pusher.com/beams/concepts/authenticated-users):
 `setUserId` and `clearAllState` functions were added

### Changed
 - Renamed interests methods to clarify that they are device interests

## [1.1.0] - 2018-12-20

### Changed
 - Bumped version to 1.1.0 to denote the release of the SDK stop feature

## [1.0.4] - 2018-12-19

### Added
 - Added method to stop the SDK, deleting all interests and the device from the server

### Fixed
 - Reliability improvements when Beams service is unavailable during application start

## [1.0.3] - 2018-12-19

### Changed
 - Performance and reliability improvements

## [1.0.2] - 2018-12-14

### Fixed
 - Set subscriptions didn't sync interests with server correctly (thanks [@julioromano](https://github.com/julioromano))

## [1.0.1] - 2018-08-08

### Changed
 - Updated token refresh process to its latest form


## [1.0.0] - 2018-07-31

### Changed
 - Bumped version to 1.0.0 for General availability (GA) release.


## [0.10.3] - 2018-07-30

### Changed
 - Updated to the latest FCM dependencies

## [0.10.2] - 2018-07-25

### Changed
 - Improve interest set synchronization when booting

## [0.10.1] - 2018-06-25

### Added
 - Support for initial interest set, which enables migration from 3rd-party services
 - Implemented `setOnSubscriptionsChangedListener` which allows user to register a callback when subscriptions change


## [0.10.0] - 2018-04-23

### Added
 - Example project now includes `NotificationsMessagingService` - this service will handle all
 data notifications sent to the application.

### Changed
 - Changed `setOnMessageReceived` to `setOnMessageReceivedListenerForVisibleActivity` to make it
 clear that it will only run when the activity is visible.

## [0.9.14] - 2018-04-19

### Fixed
 - Bug which caused the SDK to "forget" interest subscriptions on app restart

## [0.9.13] - 2018-04-04

### Changed
 - Add more properties to the DELIVERY event

## [0.9.12] - 2018-03-20

### Fixed
 - Send delivery time in seconds rather than milliseconds when tracking delivery
 - Internal Open Activity to forward the intent extras

## [0.9.11] - 2018-03-12

### Changed
 - Update dependencies to target latest Android API (27) and latest Firebase SDK (11.8.0)
 - Allow '-' in the interest name as service now supports it

## [0.9.10] - 2018-02-26

### Changed
 - Keep metadata in sync

### Fixed
 - Improved invalid JSON error handling
