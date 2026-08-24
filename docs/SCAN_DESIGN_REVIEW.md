# Scan design review — future scope

The attached PDF contains a detailed 24-screen mobile design covering onboarding, authentication, camera permission, capture, batch scanning, review, manual crop, enhancement, document naming, library/search, export/share, privacy, backup, and premium plans.

## Strong parts to retain

- Clear offline/local language instead of vague privacy badges
- 48dp minimum controls, larger primary actions, and a large shutter target
- End-to-end scan flow from permission primer through save/export
- Accessible alternatives for page reordering and clear progress/success copy
- Explicit warning before a document leaves the protected local area
- Search scope explained as names/tags when page-text recognition is unavailable
- Thoughtful low-storage, no-results, denied-permission, and subscription-ending states

## Decisions required before implementation

### 1. Account contradiction

The design itself flags that one source promises scanning without an account while another requires login before the first scan. For EveryCue's offline-first first release, Scan should not require an account. Accounts should appear only when a user explicitly enables future encrypted backup/sync.

### 2. Brand and design-system conflict

The PDF uses Toolly naming, Source Serif typography, cyan interaction ink, and magenta premium/destructive accents. The shipped source described in the PDF uses a different blue, sans-serif, and Material system. EveryCue needs one shared design system; Scan should be reskinned before coding rather than introducing a second product identity inside the app.

### 3. Scope is much larger than a first scanner release

Authentication, OTP, encrypted backup, trusted devices, account deletion, premium plans, purchase restore, OCR search, PDF tools, and cross-platform parity substantially increase backend, security, billing, policy, and QA work.

## Recommended future EveryCue Scan MVP

```text
Permission explanation
→ Scan/import
→ Multi-page review
→ Crop/rotate/filter
→ Name and category
→ Local library
→ PDF/JPEG export and share
```

Defer account creation, cloud backup, trusted devices, OCR full-text search, premium billing, unlimited-page entitlements, merge/split/compress tools, and iOS parity until the local Android scanner is stable.

## Scanner-engine implication

Google Play services' document scanner starts its own scanner UI flow and can return JPEG/PDF results with page limits, gallery import, crop/rotate/reorder, filters, and cleaning modes. It is well suited to a fast local MVP, but the PDF's fully custom viewfinder chrome cannot simply be skinned onto that UI. A later decision is required between:

- accepting the Google-provided scanner experience for speed and reliability, or
- building a custom CameraX/vision pipeline to match the PDF more closely.

Scan remains documentation-only in the current EveryCue scope.

