# IP Camera Viewer

An Android app for viewing and organizing IP camera streams on phones, tablets, and Android TV. It supports up to nine cameras in single, 2×2, and 3×3 layouts, with touch and D-pad navigation.

## Features

- RTSP and HTTP MJPEG streams, optional main/substream URLs, per-camera enablement and ordering.
- ONVIF WS-Discovery, device/profile inspection, stream import, and reported capability metadata.
- Manual NVR channel templates for vendor RTSP URL formats, with bulk channel creation.
- Per-camera audio selection; audio starts muted. Only the selected audio owner can play sound.
- Camera settings and credentials stored locally. Stream credentials are encrypted with AES/GCM using an Android Keystore key; camera database URLs are stored without user information.
- Focusable controls and remote-friendly layouts for Android TV.

Supported devices run Android 9 (API 28) or later. H.264 and H.265 playback depends on the camera stream and the hardware/software decoders available on the Android device.

## Build

Requirements: JDK 21 and Android SDK Platform 37 with Android build tools installed.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot'
$env:ANDROID_HOME = 'C:\Apps\AndroidSdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
./gradlew.bat testDebugUnitTest compileDebugAndroidTestKotlin assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Copy it to an Android device and install it with `adb install -r app-debug.apk`, or open it from a file manager after allowing installation from that source. A release APK can be built with `assembleRelease`; production distribution requires configuring a signing key outside the repository.

## Add cameras

- **ONVIF:** choose **Discover ONVIF** while the device is on the camera LAN. Select a discovered camera, enter its credentials locally, inspect media profiles, then import the desired profile. ONVIF discovery depends on multicast being allowed by the network.
- **RTSP:** choose **Add RTSP camera**, enter a name and `rtsp://` URL, and optionally provide a substream URL. Credentials can be entered in the URL or in the edit form; stored secrets are encrypted.
- **MJPEG:** choose **Add MJPEG** and enter an `http://` or `https://` JPEG/MJPEG endpoint and optional Basic Authentication credentials.
- **NVR:** choose **Add NVR/channels** and enter the host, credentials, channel range, and vendor URL template. Generic automatic NVR discovery is not possible without a known vendor protocol; available discovery adapters can be added independently.

Choose 1×1, 2×2, or 3×3 in the layout selector. Select a tile to make it the active camera; in 1×1 mode, choose a camera from the camera list. On Android TV, use the D-pad to move between focusable controls and Select/Enter to activate them. Touch users can tap tiles and buttons.

## Camera setup

Add an RTSP or HTTP MJPEG URL directly, or run ONVIF discovery on the same local network as the camera. NVR setup accepts an RTSP URL template containing `{channel}` and optionally `{host}` and `{port}`. For example, `rtsp://{host}:554/Streaming/Channels/{channel}01`.

NVR URL formats vary by vendor, so the app uses a configurable template instead of guessing a vendor protocol. ONVIF PTZ capability is recorded when advertised; a vendor-specific PTZ transport is needed to issue movement commands. Generic RTSP talkback is not implemented, so the app does not request microphone access or present a nonfunctional talk button. HTTP MJPEG audio is not supported.

## Security and network behavior

Credentials stay on-device and are encrypted in Android Keystore-backed storage. RTSP and MJPEG traffic uses the camera's configured transport, so credentials and video are only protected in transit when the camera endpoint itself supports a secure transport. ONVIF discovery and camera requests run on the local network.

Audio is available only when a stream provides an audio track and the camera profile reports audio support. Audio starts muted and only the selected camera can own audio output. Talkback requires a compatible camera-specific transport and is not exposed until one is implemented. See [ARCHITECTURE.md](ARCHITECTURE.md) for module responsibilities and data flow.
