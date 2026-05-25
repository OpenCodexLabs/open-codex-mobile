# Mobile GUI Agent Loop Workflow

This workflow turns a broad phone-side request into safe, auditable Android GUI operations through desktop Codex and ADB.

## Definition

```text
Phone-side agent request
-> desktop agent planning and execution
-> ADB controls Android GUI
-> screenshot/UI tree verification
-> result returned to the phone
```

## Default task policy

Allowed without extra confirmation:

- Open an app.
- Search or navigate within an app.
- Read visible non-sensitive results.
- Take screenshots for verification.
- Go back, close dialogs, return Home.

Must stop and ask the user:

- Payment, ordering, booking, calling, sending, posting, deleting.
- Granting permissions.
- Changing account, privacy, payment, or security settings.
- Any action where the UI text suggests commitment, confirmation, or irreversible side effects.

## Execution steps

1. Confirm device availability.

```sh
adb devices -l
```

2. Capture the current screen.

```sh
adb exec-out screencap -p > before.png
```

3. Dump the UI tree when text or bounds matter.

```sh
adb shell uiautomator dump /sdcard/window.xml
adb exec-out cat /sdcard/window.xml > window.xml
```

4. Prefer app intents for opening apps, then GUI actions for app-specific flows.

```sh
adb shell monkey -p your.map.package.name -c android.intent.category.LAUNCHER 1
```

5. Use screenshots after each major action.

```sh
adb exec-out screencap -p > step.png
```

6. Stop at the first irreversible boundary and summarize what remains for the user.

## Demo task template

```text
Use the connected Android phone through USB ADB.
Open the target map app.
Search for [destination].
Read the route or ride estimate from the app.
Do not place an order, pay, call, or change settings.
Return the result with screenshot evidence.
```

## Notes

- Coordinate taps are acceptable for experiments, but UI tree text should be used whenever possible.
- Chinese text input through plain `adb shell input text` is unreliable on many Android builds. Prefer app URI schemes, visible search suggestions, or a dedicated ADB keyboard for repeated workflows.
- If the app shows a pending payment or historical order, stop unless the user explicitly asks to handle it.
