package com.helloworld;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;

public class MainActivity extends Activity {
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Context ctx = getApplicationContext(); // Other way of getting context is via: ```MainActivity.this```
    
    Utl.showToast(ctx, "Main Activity's onCreate method executed!");
    
    Utl.showNotification(ctx, "channel 1", 1, "Daily Reminders", "Life just rock and rolls", "Longboards just rocks down hill !");
    Utl.showNotification(ctx, "channel 2", 2, "Daily Reminders 2", "Life just rock and rolls 2", "Longboards just rocks down hill 2");
    
    // todo: learn how to show a simply image:
    // URL url = new URL("https://i.picsum.photos/id/202/536/354.jpg?hmac=yO3GuY1TtL9JGeHctCd_n_LRx7XrmqDHOMESFbbN5lM");
    // Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
    // imageView.setImageBitmap(bmp);

    // todo: learn how to fetch some json data and show raw json text in a textview:

    // todo: learn prompting for permissions.
    // src1: https://developer.android.com/training/permissions/requesting
    // src2: androidOfficial@youtube: https://www.youtube.com/watch?v=x38dYUm7tCY
    // src3: https://www.geeksforgeeks.org/android-how-to-request-permissions-in-android-application/
  }
}