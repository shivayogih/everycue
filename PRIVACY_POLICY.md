# EveryCue Privacy Policy — development draft

Effective date: to be finalized before publication.

EveryCue is currently designed as an offline-first Android application. The development build does not require an account and does not include advertising, analytics, cloud synchronization, or a developer-operated backend.

Track and Renew records are stored in an app-private local Room database. Pack trips, checklist data, settings, and the optional local identity profile (name, phone, email, address, and pincode) are stored in app-private local Android DataStore. Sensitive organizer text and Pack/profile payloads are authenticated and encrypted at rest with AES-256-GCM using a non-exportable Android Keystore key; non-sensitive appearance and reminder preferences are not encrypted. Android operating-system backup and device transfer are disabled for EveryCue. The app sends profile details to another app only after the user taps Share and chooses a destination in Android's system chooser. Manual JSON exports contain readable profile and organizer data so the user can move or preserve it; the user controls the export destination and is responsible for protecting that file.

Production builds apply local device-integrity checks and do not open organizer/profile data when common emulator, root, test-key, or system-modification signals are detected. They also block screenshots and non-system overlays. Development builds permit emulators and rooted development devices. These controls reduce exposure but cannot guarantee protection against an attacker who fully controls the operating system.

Smart Add uses Google Play services code-scanner and text-recognition modules. Scanner/OCR processing runs on the device and EveryCue does not send recognized text, barcodes, or selected images to a developer-operated server. Google Play services may download the required on-device model/module. A label image is processed only after the user captures or selects it, OCR output remains an editable draft, and manual item entry remains available when scanning is unavailable. The final release Data safety review must verify these SDK behaviors against Google's then-current documentation.

The shipped privacy policy and Google Play Data safety answers must be reviewed against the final release bundle and every included SDK before publication. This draft is not a hosted production privacy policy.
