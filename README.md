# LauncherAndroidTV
An overlay-type launcher for AndroidTV. The idea is to make as lightweight fully featured Launcher as possible with the ability to not interrupt the user's experience.

To launch the Launcher, either its main activity may be started OR you can send a global broadcast to show the overlay without pausing playback:
```am broadcast -a android.intent.action.MAIN -c android.intent.category.HOME -f 0x01000000```
This is facilitated on my TV with my [RespectLauncherAndroidTV](https://github.com/lonelytransistor/RespectLauncherAndroidTV) project, but can be done with anything that hooks to a button and can send broadcasts.
