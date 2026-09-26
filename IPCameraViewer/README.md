# IPCameraViewer

Android IP camera / NVR viewer MVP.

## Current features
- 1×1, 2×2 and 3×3 live-view grids
- RTSP playback using AndroidX Media3
- RTSP-over-TCP for common LAN/NVR deployments
- Add/edit/delete cameras
- Username/password support
- Local camera configuration storage
- ONVIF WS-Discovery scan for Network Video Transmitters
- NVR channels can be added as ordinary RTSP URLs

## Build
Open in Android Studio and build `app`.

The repository is also prepared for GitHub Actions in `.github/workflows/android.yml`.

## Notes
The first version deliberately keeps ONVIF discovery separate from stream playback. ONVIF discovery finds devices/XAddrs; RTSP URLs remain configurable because NVR and camera vendors expose different stream paths.
