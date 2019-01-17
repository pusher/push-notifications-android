# How to add a local copy of this project as a dependency
When testing changes you've made to this library, it can be useful to use
your local copy in another project. Gradle makes this process very complicated.
The steps for how to do this are as follows:

## Step 1 - Create a new android application / project
Either create or load the target project in Android Studio.

## Step 2 - Add the push-notifications-android project
In your app project's `settings.gradle` add the following lines:
```
include ':pushnotifications'
project(':pushnotifications').projectDir = new File(settingsDir, '<PATH_TO_THIS_PROJECT>/push-notifications-android')

include ':pushnotifications-lib'
project(':pushnotifications-lib').projectDir = new File(settingsDir, '<PATH_TO_THIS_PROJECT>/push-notifications-android/pushnotifications')

include ':pushnotifications-lint'
project(':pushnotifications-lint').projectDir = new File(settingsDir, '<PATH_TO_THIS_PROJECT>/push-notifications-android/pushnotifications-lint')
```

## Step 3 - Add the necessary build config
This project has various build config variables that will be required to build
it from source. Add the following to your project-level `build.gradle`:
```
buildscript {
    ext.versions = [
            kotlin: '1.2.51',

            androidPlugin: '3.0.0',
            androidTools: '27.0.2',
            androidLintTools: '26.0.0',
    ]
    ext.deps = [
            androidPlugin: "com.android.tools.build:gradle:${versions.androidPlugin}",

            lint: [
                    'core': "com.android.tools.lint:lint:${versions.androidLintTools}",
                    'api': "com.android.tools.lint:lint-api:${versions.androidLintTools}",
                    'checks': "com.android.tools.lint:lint-checks:${versions.androidLintTools}",
                    'tests': "com.android.tools.lint:lint-tests:${versions.androidLintTools}",
            ],
    ]
 // Rest of your buildscript...
}
```

| If these values don't work, you can copy the latest ones from the `build.gradle` for this project.

## Step 4 - Add the pushnotifications-lib project to you apps build.gradle
Add the following line to your application's `build.gradle` where you would
normally add the actual Beams SDK:
```
dependencies {
    implementation 'com.google.firebase:firebase-core:16.0.1'
    implementation 'com.google.firebase:firebase-messaging:17.1.0'
    compile project(':pushnotifications-lib') // Add this line
    // Rest of your dependencies...
}
```
