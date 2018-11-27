# Moby tracking SDK [![Build Status](https://travis-ci.org/O2MC/moby-tracking-sdk.svg?branch=master)](https://travis-ci.org/O2MC/moby-tracking-sdk) [ ![Download](https://api.bintray.com/packages/edwinvrooij/moby-tracking-sdk-test/io.o2mc.sdk/images/download.svg) ](https://bintray.com/edwinvrooij/moby-tracking-sdk-test/io.o2mc.sdk/_latestVersion)

O2MC's mobile tracking SDK for collecting and measuring analytical events.

## Getting started

These instructions will get you up and running on your local machine. There is an Android and iOS app available for local testing.

### Prerequisites
The following tools are required depending on which mobile OS you are targeting.

**Android**

* Android SDK platform-tools 26

**iOS**

* XCode 9+
* iOS 9+



## Installation

### Android
> Hello, World!

For a Hello-world example, please refer to [this](docs/ANDROID.md) page.

Refer to the [API documentation](docs#api) for more details on how to use the SDK.

## Data specification
The data is sent as JSON data. The format contains two main properties, the `device` and the `events` property. Both properties are guaranteed to be included.

> Example
```json
{
    "device": {
        "appId": "io.o2mc.app",
        "locale": "en_US",
        "name": "Android SDK built for x86",
        "os": "android",
        "osVersion": "9"
    },
    "events": [
        {
            "name": "MainActivity created",                                                     
            "timestamp": "2018-07-25T11:19:17+02:00",
        },
        {
            "name": "Clicked button: 'Create Track Event'",                                                     
            "timestamp": "2018-07-25T11:19:17+02:00",
            "value": "Name"
        }
    ],
    "number": 1,
    "retries": 0,
    "sessionId": "6458def9-30b8-4591-9eb3-9e9881b8dc3f",
    "timestamp": "2018-07-25T11:19:18+02:00"
}
```

Refer to the [specification documentation](docs/DATA_SPECIFICATION.md) for more details.

## Contributing

Any kind of contribution is welcome.
For bug reports, feature requests, please open an issue without any hesitation.
For code contributions, it's strongly suggested to open an issue for discussion first. For more details, please refer to [contributing documentation](docs/CONTRIBUTING.md).

## License

[MIT license](LICENSE).

Copyright (c) Insite Innovations and Properties B.V.
