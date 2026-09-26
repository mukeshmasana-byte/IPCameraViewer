# Architecture

The app follows a modern Android architecture utilizing Jetpack Compose for the UI layer and Room for local persistence.

## Modules
- **UI Layer (`ui.components` & `MainActivity.kt`)**: Displays up to 9 camera feeds in dynamic grids (1x1, 2x2, 3x3) using Jetpack Compose `LazyVerticalGrid`. Separated into specific UI components (`VideoPlayer`, `CameraGrid`, `CameraDialog`, `TopBar`) for maintainability.
- **Data Layer (`data` & `repository`)**: Contains the `AppDatabase` (Room), `CameraDao`, and `CameraRepository` managing the SQLite database and exposing flows to the UI.
- **Security (`security`)**: A `CryptoManager` utilizing `AndroidKeyStore` securely manages the encryption and decryption of camera passwords before they are stored into Room.
- **Player (`VideoPlayer`)**: Uses AndroidX Media3 ExoPlayer optimized for RTSP over TCP for consistent NVR and standard camera playback.
- **Discovery (`OnvifDiscovery` & `nvr`)**: Scans the local network via UDP multicast to discover ONVIF `NetworkVideoTransmitter` profiles. The NVR module outlines an abstraction for retrieving available channels directly from local recorders.
- **PTZ and Talkback (`ptz` & `player`)**: Interfaces ready for extending interaction using Talkback backchannel and Pan-Tilt-Zoom capabilities natively.
