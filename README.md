# HotlineRedial V0.2

Android default dialer experimental prototype for retrying busy or congested calls. Kotlin, minSdk 29, targetSdk 35.

## Build

GitHub Actions workflow `Build Android APK` builds a debug APK and uploads an artifact. This is an experimental build, not verified on REDMI Turbo 5 Max. Setting this as default dialer can affect incoming-call handling. Restore your original dialer after testing.

## Caution

Retry stops on an ACTIVE call and on unrecognized disconnect causes. Android background call restrictions and OEM policies can prevent calls even when the foreground service remains active. This app is not suitable as a replacement for a production dialer.
