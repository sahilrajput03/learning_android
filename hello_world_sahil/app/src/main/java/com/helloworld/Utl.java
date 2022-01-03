package com.helloworld;

import java.lang.Math;
import android.widget.Toast;
// NotificationCompat, src: https://stackoverflow.com/a/59507099/10012446
import androidx.core.app.NotificationCompat;
import android.app.PendingIntent; // src: https://stackoverflow.com/a/27396662/10012446
import android.content.Intent; // src: https://stackoverflow.com/a/26246956/10012446
import android.app.NotificationChannel; // src: https://stackoverflow.com/q/46155562/10012446
import android.app.NotificationManager;
import android.app.Notification;
import android.content.Context;
import android.os.Build;


public class Utl {
      // Generated via github copilot:
  public static void showToast(Context context, String message) {
    // ? Sending a toast message
    // Toast.makeText(getApplicationContext(), "Toast: Activity Code just executed.", Toast.LENGTH_LONG).show(); // Toast.LENGTH_SHORT and Toast.LENGTH_LONG,  // We can't change duration at all, // we can only pass text to this function. source: https://stackoverflow.com/a/2221285/10012446
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
  }
  
  public static void showNotification(Context context, String channelId, int notificationId, String category_text, String title_text, String description_text) {
    // Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    // ? Sending a statusbar notification: docs:https://developer.android.com/training/notify-user/build-notification#add_the_support_library.
    // src: https://stackoverflow.com/a/16448278/10012446
    NotificationManager mNotificationManager;

    NotificationCompat.Builder b = new NotificationCompat.Builder(context, "notify_001");
    Intent i = new Intent(context, MainActivity.class);
    PendingIntent pi = PendingIntent.getActivity(context, 0, i, 0);

    NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
    bigText.bigText(description_text);
    bigText.setBigContentTitle(title_text);
    bigText.setSummaryText(category_text);

    b.setContentIntent(pi);
    b.setSmallIcon(R.mipmap.ic_launcher_round);
    b.setContentTitle("Your Title");
    b.setContentText("Your text");
    b.setPriority(Notification.PRIORITY_MAX);
    b.setStyle(bigText);

    mNotificationManager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

    // === Removed some obsoletes
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
    {
        // String channelId = "Your_channel_id";
        NotificationChannel channel = new NotificationChannel(
                                            channelId,
                                            "Channel human readable title",
                                            NotificationManager.IMPORTANCE_HIGH);
      mNotificationManager.createNotificationChannel(channel);
      b.setChannelId(channelId);
    }

    mNotificationManager.notify(notificationId, b.build());
  }

}