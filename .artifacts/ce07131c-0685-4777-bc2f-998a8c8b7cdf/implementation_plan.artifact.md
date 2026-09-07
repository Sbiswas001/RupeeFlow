# Polish and Simplify Settings for v1.0.0

This plan simplifies the Settings screen by removing unimplemented "Coming soon" features, focusing on providing a clean experience for the v1.0.0 release.

## User Review Required

> [!IMPORTANT]
> - The **Advanced** section (containing OCR Settings) will be removed completely.
> - **Clear Cache** and **Optimize Database** will be removed from the Storage section.
> - **Database Size** and **Cache Size** will be changed to non-clickable informational rows.

## Proposed Changes

### [feature/settings](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/feature/settings)

#### [MODIFY] [SettingsScreen.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/feature/settings/SettingsScreen.kt)
- Update **Storage Section**:
    - Remove "Clear Cache" and its divider.
    - Remove "Optimize Database" and its divider.
    - Set `onClick = {}` and ensure `Database Size` and `Cache Size` are not perceived as clickable (they already have `showChevron = false`).
- Remove **Advanced Section** (OCR Settings).
- Ensure **Reset App Data** remains at the bottom with its destructive styling and confirmation dialog.

#### [MODIFY] [PreferenceRow]
- Update `PreferenceRow` to make it non-clickable if `onClick` is an empty lambda or provide a `clickable` parameter to disable the ripple/interaction when intended as informational only.

## Verification Plan

### Manual Verification
1. Open Settings -> Verify **Storage** section only contains:
    - Backup & Restore (Clickable, has chevron)
    - Database Size (Non-clickable, no chevron)
    - Cache Size (Non-clickable, no chevron)
    - Reset App Data (Clickable, Red text, no chevron)
2. Verify **Advanced** section is gone.
3. Verify **Reset App Data** still shows the confirmation dialog.
4. Verify **Backup & Restore** still navigates correctly.
