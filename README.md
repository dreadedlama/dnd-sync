# DNDSync
This App was developed to enable synchronization between the smartphone and the **Galaxy Watch 4** of the
**Do Not Disturb** (*DND*) and **Bedtime mode** from [Digital Wellbeing](https://play.google.com/store/apps/details?id=com.google.android.apps.wellbeing&hl=en_US).
*DND* synchronization is only supported officially if using a Samsung phone with this smartwatch, Bedtime mode synchronization
is only newly available for the newest Pixel Watch 2:
* With this repo you get ***both***!

**Functionalities:**
* *1-way sync* or a *2-way sync* of **DND**, depending on the preferences
* When *DND* is activated on the phone, depending on the preferences, **Bedtime mode** can be activated to the watch!
  * (Useful for **Xiaomi phones** and **Samsung phones**, which don't have the *Digital Wellbeing app*)
* Automatically toggle **Bedtime** mode for the watch whenever it is activated on the phone
  * At night, when I charge my phone, bedtime mode on the phone is enabled and I wanted to sync and enable same mode on the watch
* Automatically toggle **Power Saver** mode in combo with bedtime mode on the watch, whenever bedtime mode is synced from the phone

Most of the credit goes to [@rhaeus](https://github.com/rhaeus) for the original development of this app.

I ([@dreadedlama](https://github.com/dreadedlama)) initially forked the project and worked on improving the Bedtime Mode implementation. [@Silleellie](https://github.com/Silleellie) later built on those changes in his own fork and added further improvements. [@Turtlepaw](https://github.com/Turtlepaw) subsequently took the project further with his own fork, including a Material 3 design to it.
**A big thank you to [@Silleellie](https://github.com/Silleellie) and [@Turtlepaw](https://github.com/Turtlepaw) for building on the project and contributing these improvements.**

I have since incorporated changes from both [@Silleellie](https://github.com/Silleellie)'s and [@Turtlepaw](https://github.com/Turtlepaw)'s forks into this fork. This project therefore combines improvements from both branches along with my own changes.

## Setup

***Manual installation is required. The use of ADB is required. (*Don't worry, it's very easy!*)***

* Download the latest `.apk` files from the ['Releases' section](https://github.com/dreadedlama/dnd-sync/releases) (`mobile-release.apk` and `wear-release.apk`)
* Be sure to enable notifications for Bedtime mode of the Digital Wellbeing app on your phone (*They are by default*)
  * This app knows that bedtime mode is activated when its notification pops up (since there's no public API for the *Digital Wellbeing* app)
* If you don't have ADB, you can download a lightweight version from the [github release page](https://github.com/K3V1991/ADB-and-FastbootPlusPlus/releases) of *ADB and Fastboot++*
  * In the following instructions, version **1.0.8** is used

### Phone

<p float="left">
  <img src="/images/mobile.png" width="300" />
</p>

1. Install the app `mobile-release.apk` on the phone via *adb*
  * Enable `USB Debugging` in the *Developer Options* of your phone and the connect it to the PC
  * Run `adb install mobile-release.ap`
2. Disconnect the phone from the PC
  * Disable `USB Debugging` from the *Developer Options* of your phone
3. Open the app and grant the permission for *DND Access* and *Bedtime Access* by clicking on the menu entry *DND-Bedtime Permission*. This will open the permission screen.
  * This Permission is required so that the app can *read/write* DND state and *read* Bedtime mode. Without this permission, the sync will not work.
4. Go back on the app and check that `DND-Bedtime Permission` now says **DND-Bedtime access granted** (*you may need to tap on the menu entry for it to update*)


### Watch

<p float="left">
  <img src="/images/wear_1.png" width="200" />
  <img src="/images/wear_2.png" width="200" />
</p>

Setting up the watch is a bit more *tricky* since the watch OS lacks the permission screen for DND access,
but the permission needed can be **easily set via ADB**!

Note: This is only tested on my Galaxy Watch 4 and it might not work on other devices!

1. Connect the watch to your computer via **adb** (watch and computer have to be in the *same network!*)
  * Enable Developer Options: Go to `Settings -> About watch -> Software -> tap the Software version 5 times -> developer mode is on (you can disable it in the same way)`
  * Enable `ADB debugging` and `Debug over WIFI` (in `Settings -> Developer Options`)
  * Click on `Pair new device`
  * Note the watch IP address and port, something like `192.168.0.100:5555`
  * Note also the pair key, something like `123456`
  * Pair the watch with `adb pair 192.168.0.100:5555 123456` (***insert your value!***)
  * Check that now your PC is listed under `Paired devices` and there's a text under it saying `Currently connected`
    * If not, perform `adb connect 192.168.0.100:6666` with the IP address and port listed in the `Debug over WIFI` screen
2. Install the app `wear-release.apk` on the watch
  * Run `adb install wear-release.apk`
3. Grant permission for **DND access** (*This allows the app to listen to DND changes and to change the DND setting*)
  * Run `adb shell cmd notification allow_listener in.dreadedlama.dndsync/in.dreadedlama.dndsync.DNDNotificationService`
4. Grant permission for **Secure Setting access** (*This allows the app to change BedTime mode setting on the watch*)
  * Run `adb shell pm grant in.dreadedlama.dndsync android.permission.WRITE_SECURE_SETTINGS`
  * **SAMSUNG WATCH ONLY** - If you want to show the Bedtime screen overlay on your Samsung watch, grant the app permission to show overlay windows:
      `adb shell appops set in.dreadedlama.dndsync SYSTEM_ALERT_WINDOW allow`
5. Open the app on the watch, scroll to the permission section and check if both `DND Permission`, `Secure Settings Permission` and
   `System Alert Windows Permission` say ***Granted*** (*you may need to tap on the menu entries for them to update*)
6. ***IMPORTANT: Disable `ADB debugging` and `Debug over WIFI`, because these options drain the battery!***



## Preferences options

### Phone preferences

* With the ***Sync DND state to watch*** switch you can enable and disable the sync for *DND* mode.
  If enabled, a *DND* change on the phone will lead to *DND* change on the watch.
* With the ***Enable Bedtime mode on DND sync*** switch, you can choose to activate the bedtime mode on the watch
  whenever DND is activated on the phone. Useful for all those phones missing the *Digital wellbeing* app
* With the ***Sync Bedtime mode to watch*** switch you can enable and disable the sync for bedtime mode.
  If enabled, when *Bedtime mode* is *enabled/disabled/paused* on the phone, it will be *enabled/disabled/paused* on the watch
* If you enable the setting ***Enable Power Saver mode with Bedtime***, the watch will turn on *power save* mode whenever the *Bedtime Mode* is synced from the phone,
  either due to *Sync Bedtime mode to watch* or to *Enable Bedtime mode on DND sync*

### Watch preferences

* If you enable the setting ***Sync DND state to phone***, a DND change on the watch will lead to a DND change on the phone
* If you enable the setting ***Vibrate on sync***, the watch will vibrate whenever it receives a sync request from the phone

# Note
If you are unable to "Allow Notification Access" to the mobile app and it is faded, go the apps section in Settings, open DNDSync app, click 3 dots on top right and grant "Allow Restricted Settings" access.
Now you'll be able to grant the Notification access to mobile app.