# Push Notifications Android

[![Build Status](https://app.bitrise.io/app/45610b9746e396f5/status.svg?token=OsxReMr5vbhXk7Y0wRuynQ&branch=master)](https://www.bitrise.io/app/45610b9746e396f5)
[![Twitter](https://img.shields.io/badge/twitter-@Pusher-blue.svg?style=flat)](http://twitter.com/Pusher)

This is the Android SDK for the [Pusher Beams](https://pusher.com/beams) service.

The SDK is written in Kotlin, but aimed to be as Java-friendly as possible

## Installation

### Update your project level gradle config

Add the Google Services classpath to the dependencies section of your project-level `build.gradle`:

```
buildscript {
    ...

    dependencies {
        // Add this line
        classpath 'com.google.gms:google-services:4.2.0'
    }
}
```

### Update your app level gradle config

If using firebase-messaging version below 22.0.0:
```
dependencies {
    ...

    // Add these lines
    implementation 'com.google.firebase:firebase-core:16.0.9'
    implementation 'com.google.firebase:firebase-messaging:18.0.0'
    implementation 'com.pusher:push-notifications-android:1.9.2'
}

// Add this line to the end of the file
apply plugin: 'com.google.gms.google-services'
```

If using firebase-messaging 22.0.0 and above:
```
dependencies {
    ...

    // Add these lines
    implementation 'com.google.firebase:firebase-messaging:22.0.0'
    implementation 'com.google.firebase:firebase-installations:17.1.0'
    implementation 'com.pusher:push-notifications-android:1.9.2'
}

// Add this line to the end of the file
apply plugin: 'com.google.gms.google-services'
```

## Release Process

Available for Pusher folks only: Search on the manual for Beams SDK Release Process

## Documentation

You can find our up-to-date documentation in [here](https://docs.pusher.com/beams/).

## Communication

- Found a bug? Please open an [issue](https://github.com/pusher/push-notifications-android/issues).
- Have a feature request. Please open an [issue](https://github.com/pusher/push-notifications-android/issues).
- If you want to contribute, please submit a [pull request](https://github.com/pusher/push-notifications-android/pulls) (preferably with some tests).

## Credits

Pusher Beams is owned and maintained by [Pusher](https://pusher.com).

## License

Pusher Beams is released under the MIT license. See [LICENSE](https://github.com/pusher/push-notifications-android/blob/master/LICENSE) for details.
