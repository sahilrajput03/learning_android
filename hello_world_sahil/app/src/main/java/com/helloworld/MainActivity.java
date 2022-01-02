package com.helloworld;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

// NotificationCompat, src: https://stackoverflow.com/a/59507099/10012446
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class MainActivity extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    
    // Sending a toast message
    Toast.makeText(getApplicationContext(), "Toast: Activity Code just executed.", Toast.LENGTH_LONG).show(); // Toast.LENGTH_SHORT and Toast.LENGTH_LONG,  // We can't change duration at all, source: https://stackoverflow.com/a/2221285/10012446

    // Sending a statusbar notification: docs:https://developer.android.com/training/notify-user/build-notification#add_the_support_library
    NotificationCompat.Builder b = new NotificationCompat.Builder(this, "my_channel_id1") // docs: https://developer.android.com/reference/androidx/core/app/NotificationCompat.Builder
        .setSmallIcon(R.drawable.ic_launcher_background) // this is any desirable icon.
        .setContentTitle("textTitle")
        .setContentText("textContent")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT);
    NotificationManagerCompat nm = NotificationManagerCompat.from(this);
    
    int notificationId = 101; // notificationId is a unique int for each notification that you must define
    nm.notify(notificationId, b.build());

    // URL url = new URL("https://i.picsum.photos/id/202/536/354.jpg?hmac=yO3GuY1TtL9JGeHctCd_n_LRx7XrmqDHOMESFbbN5lM");
    // Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
    // imageView.setImageBitmap(bmp);
  }
}