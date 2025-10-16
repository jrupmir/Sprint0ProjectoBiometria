Test runner instructions

Web (Next.js) tests

Prereqs:
- Node 18+ and npm

From project `src/web` folder:

```powershell
cd src/web
npm install
npm test
```

This runs Jest tests added under `src/web/__tests__`.

Android unit tests

Prereqs:
- Java JDK and Android SDK configured
- Gradle wrapper available (it is) and JAVA_HOME set

From project `src/android` folder:

```powershell
cd src/android
./gradlew.bat test
```

This will run the JVM unit tests under `app/src/test/java/...` (does not require an emulator).

Arduino checks

A simple script checks `.ino` files exist and (optionally) helps you run `arduino-cli` to compile.

```powershell
python arduino\run_arduino_tests.py
```

Notes

- Android instrumentation/UI tests require emulator/device and are not added here.
- The web tests use jest + testing-library and are quick to run.
- If you want CI integration (Github Actions) I can add workflows to run these tests on push/PR.
