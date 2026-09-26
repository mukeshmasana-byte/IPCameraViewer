# Architecture

## Layers

- `ui/`: Compose dashboard, camera/NVR management, ONVIF discovery screens, and `CameraViewModel`.
- `player/`: Media3 RTSP playback, HTTP MJPEG frame decoding, lifecycle-bound playback, selected-camera audio coordination, and talkback boundary.
- `onvif/`: WS-Discovery, secure SOAP/XML parsing, capability/profile inspection, and stream URI requests.
- `nvr/`: NVR discovery adapter boundary and unsupported-vendor fallback.
- `data/` and `database/`: repository, Room entities/DAOs, schema migrations, and camera persistence.
- `security/`: Android Keystore AES/GCM secret storage.

## Camera data flow

The ViewModel validates URLs, creates source-specific camera models, and asks the repository to save them. Room stores non-secret metadata and sanitized endpoint origins. URL paths, query parameters, and credentials are held in encrypted preferences. Repository reads combine metadata with decrypted secrets. Camera deletion also removes the associated secret material; NVR removal cascades across its channels.

RTSP cameras are rendered through Media3. MJPEG cameras use a lifecycle-bound HTTP connection that decodes JPEG frames and limits each frame to 12 MiB. A single audio controller owns output for the selected camera; track availability and camera capability determine whether the audio control is available.

## Network and capability boundaries

ONVIF discovery uses multicast WS-Discovery and imports profiles only after a successful device request. XML parsing disables external entities and DTD processing. NVR vendor discovery is an adapter interface because discovery protocols and channel URL schemes are vendor-specific; manual templates provide a deterministic fallback. PTZ is exposed as capability metadata and an interface boundary but requires a matching command implementation. Talkback likewise remains an interface boundary until a compatible camera transport exists.

## Tests

JVM tests cover URL validation, credential URL handling, camera entity secret separation, ONVIF XML parsing, NVR adapter fallback, MJPEG framing, and audio ownership. Android instrumentation tests cover Room persistence/migrations and encrypted secret storage. Instrumentation tests require a device or emulator.
