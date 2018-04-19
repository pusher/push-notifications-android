# Push Notifications Android 

[![Build Status](https://www.bitrise.io/app/45610b9746e396f5/status.svg?token=OsxReMr5vbhXk7Y0wRuynQ&branch=master)](https://www.bitrise.io/app/45610b9746e396f5)
[![Twitter](https://img.shields.io/badge/twitter-@Pusher-blue.svg?style=flat)](http://twitter.com/Pusher)

This is the Android SDK for the [Push Notifications](https://pusher.com/push-notifications) service.

The SDK is written in Kotlin, but aimed to be as Java-friendly as possible

## Installation

### Update your project level gradle config

Add the Google Services classpath to the dependencies section of your project-level `build.gradle`:

```
buildscript {
    ...

    dependencies {
        // Add this line
        classpath 'com.google.gms:google-services:3.1.0'
    }
}
```

### Update your app level gradle config

```
dependencies {
    ...

    // Add these lines
    implementation 'com.google.firebase:firebase-messaging:11.8.0'
    implementation 'com.pusher:push-notifications-android:0.9.14'
}

// Add this line to the end of the file
apply plugin: 'com.google.gms.google-services'
```

## Documentation

You can find our up-to-date documentation in [here](https://docs.pusher.com/push-notifications/).

## Communication

- Found a bug? Please open an [issue](https://github.com/pusher/push-notifications-android/issues).
- Have a feature request. Please open an [issue](https://github.com/pusher/push-notifications-android/issues).
- If you want to contribute, please submit a [pull request](https://github.com/pusher/push-notifications-android/pulls) (preferably with some tests).

## Credits

Push Notifications is owned and maintained by [Pusher](https://pusher.com).

## License

Push Notifications is released under the MIT license. See [LICENSE](https://github.com/pusher/push-notifications-android/blob/master/LICENSE) for details.
