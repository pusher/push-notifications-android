# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]


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
