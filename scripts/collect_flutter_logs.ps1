# Diagnostics script: collect_flutter_logs.ps1
# Run this from PowerShell (not the IDE terminal) to gather Flutter + Gradle logs.
# Usage: Open PowerShell, then run:
#   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass; .\scripts\collect_flutter_logs.ps1

$root = 'C:\Users\AnujiWeragoda\Android_Project\Kotlin_Development_MVVM'
$flutter = 'C:\flutter\bin\flutter.bat'
$gradlew = Join-Path $root 'gradlew'

Write-Host "Working directory: $root"
Set-Location $root

# Ensure output files are removed
$outs = @('gradle-flutter-failure.txt','flutter-build-bundle-debug.txt','flutter-doctor.txt','flutter-pub-get.txt')
foreach ($f in $outs) { if (Test-Path $f) { Remove-Item $f -Force } }

# Run flutter pub get in the module
Write-Host "Running: flutter pub get (flutter_module)"
Set-Location (Join-Path $root 'flutter_module')
& $flutter pub get > (Join-Path $root 'flutter-pub-get.txt') 2>&1

# Run flutter doctor
Write-Host "Running: flutter doctor -v"
& $flutter doctor -v > (Join-Path $root 'flutter-doctor.txt') 2>&1

# Run flutter build bundle verbose
Write-Host "Running: flutter build bundle --debug -v"
& $flutter build bundle --debug -v --target=lib\main.dart > (Join-Path $root 'flutter-build-bundle-debug.txt') 2>&1

# Run Gradle task from repo root
Write-Host "Running: gradlew :flutter:compileFlutterBuildDebug --stacktrace --info"
Set-Location $root
& $gradlew :flutter:compileFlutterBuildDebug --stacktrace --info > (Join-Path $root 'gradle-flutter-failure.txt') 2>&1

# Compress logs
$zip = Join-Path $root 'flutter_build_logs.zip'
$filesToZip = @((Join-Path $root 'gradle-flutter-failure.txt'), (Join-Path $root 'flutter-build-bundle-debug.txt'), (Join-Path $root 'flutter-doctor.txt'), (Join-Path $root 'flutter-pub-get.txt'))
Write-Host "Compressing logs to $zip"
if (Test-Path $zip) { Remove-Item $zip -Force }
Compress-Archive -Path $filesToZip -DestinationPath $zip -Force

Write-Host "Done. Upload $zip or paste the tail of the files if you prefer."

