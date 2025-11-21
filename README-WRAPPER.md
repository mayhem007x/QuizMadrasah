# Gradle wrapper setup

This project is configured to use the Gradle wrapper with Gradle 8.4.

Files added:
- `gradlew` (Unix shell script)
- `gradlew.bat` (Windows launcher)
- `gradle/wrapper/gradle-wrapper.properties` (points to Gradle 8.4)

Notes:
- The Gradle wrapper JAR (`gradle/wrapper/gradle-wrapper.jar`) is not included here. To generate it you can either:
  - Open this project in Android Studio: Android Studio will download the Gradle distribution and create the wrapper files if needed.
  - Or, if you have a system Gradle installed, run this from the project root in PowerShell:

```powershell
gradle wrapper --gradle-version 8.4
```

After the wrapper is present you can build using the wrapper (Windows PowerShell):

```powershell
.\gradlew assembleDebug
```

If you run into issues, paste the output of `.\gradlew -v` or `gradle -v` and I will help adjust versions.
