# Floppa Walker — Meteor Client addon (Minecraft 1.21.11)

Turn on the **Floppa Walker** module (category **Floppa**) and a Floppa cube waddles
around your screen and lets out a noise at random intervals.

## Requirements
- JDK 21
- Minecraft 1.21.11 with Fabric Loader and **Meteor Client for 1.21.11**

## Build

**Easiest: let GitHub build it (nothing to install)**
1. Make a free GitHub account, create a new repository (any name).
2. Upload the *contents* of this folder to it (Add file > Upload files, drag everything in,
   including the `.github` folder). If `.github` won't upload, use Add file > Create new file,
   type `.github/workflows/build.yml` as the name and paste in that file's contents.
3. Open the **Actions** tab, wait for "Build jar" to turn green (a few minutes).
4. Click the run, scroll to **Artifacts**, download `floppa-walker-jar`, unzip it.
   The jar inside is the real mod.

**Or build locally** (the Gradle wrapper jar can't be bundled here, so do ONE of these once):

**A) Copy the wrapper from the official template (easiest)**
```
git clone --depth 1 https://github.com/MeteorDevelopment/meteor-addon-template tmp-template
cp tmp-template/gradlew tmp-template/gradlew.bat .
cp tmp-template/gradle/wrapper/gradle-wrapper.jar gradle/wrapper/
rm -rf tmp-template
```
**B) You already have Gradle 9.x installed:** run `gradle wrapper` in this folder.

Then:
```
./gradlew build          # Windows: gradlew.bat build
```
The jar is `build/libs/floppa-walker-1.0.0.jar`.

## Install
Put the jar and the Meteor Client 1.21.11 jar together in `.minecraft/mods`, launch with Fabric,
then open Meteor's GUI (Right Shift) -> **Floppa** tab -> **floppa-walker**.

## Settings
General: size, speed, waddle, pauses, bottom-only, hide-with-hud.
Sound: play-sound, sound-on-enable, min/max-interval (seconds), volume, pitch.

## Changing the image or sound
- Image: replace `src/main/resources/assets/floppa_walker/textures/gui/floppa.png`
  and update `TEX_W` / `TEX_H` in `FloppaWalker.java` to its exact pixel size.
- Sound: replace `.../sounds/floppa.ogg` (must be **Ogg Vorbis**, mono works best):
  `ffmpeg -i input.mp3 -ac 1 -c:a libvorbis -q:a 5 floppa.ogg`

## Troubleshooting
- **"Could not find/resolve ..." for a version** — all versions are in `gradle/libs.versions.toml`.
  Update `yarn-mappings`, `fabric-loader` and `loom` to the latest 1.21.11 values from
  https://fabricmc.net/develop, and make sure a `1.21.11-SNAPSHOT` Meteor build is still published.
- **"Cannot remap onEntityCollision ..." / every `net.minecraft` import fails** — known Meteor/Yarn
  issue for 1.21.11 (meteor-client issue #6551). Try a different `yarn-mappings` build in the catalog.
- **Compile error on `drawTexture(...)` or `getMatrices()`** — Minecraft's GUI drawing API changed
  a lot in 1.21.x; check the signature in your IDE (Ctrl+click `DrawContext`).
- **No sound** — confirm the file is real Ogg Vorbis, and that the Master and Players/Voice
  volume sliders in Minecraft's sound settings aren't at 0.
