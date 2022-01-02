- Raw code for sending notifications:

Below code rocks but my showNotification() method in `MainActivity.java` file is just awesome!

```java
    // Sending a statusbar notification: docs:https://developer.android.com/training/notify-user/build-notification#add_the_support_library.
    // src: https://stackoverflow.com/a/16448278/10012446
NotificationManager mNotificationManager;

NotificationCompat.Builder b = new NotificationCompat.Builder(getApplicationContext(), "notify_001");
Intent i = new Intent(getApplicationContext(), MainActivity.class);
PendingIntent pi = PendingIntent.getActivity(MainActivity.this, 0, i, 0);

NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
bigText.bigText("My long notification text here goes here..");
bigText.setBigContentTitle("Today's Bible Verse");
bigText.setSummaryText("Text in detail");

b.setContentIntent(pi);
b.setSmallIcon(R.mipmap.ic_launcher_round);
b.setContentTitle("Your Title");
b.setContentText("Your text");
b.setPriority(Notification.PRIORITY_MAX);
b.setStyle(bigText);

mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

// === Removed some obsoletes
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
{
    String channelId = "Your_channel_id";
    NotificationChannel channel = new NotificationChannel(
                                        channelId,
                                        "Channel human readable title",
                                        NotificationManager.IMPORTANCE_HIGH);
  mNotificationManager.createNotificationChannel(channel);
  b.setChannelId(channelId);
}

mNotificationManager.notify(0, b.build());
```

- Folder structure for image sizes:

```
android/app/src/main/res/drawable-mdpi/ (24x24)
android/app/src/main/res/drawable-hdpi/ (36x36)
android/app/src/main/res/drawable-xhdpi/ (48x48)
android/app/src/main/res/drawable-xxhdpi/ (72x72)
android/app/src/main/res/drawable-xxxhdpi/ (96x96)
android/app/src/main/res/drawable-xxxhdpi/ (256x256) (Large Icon)
```

- Initial code of MainActivity.java

```java
package com.helloworld;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  }
}
```
