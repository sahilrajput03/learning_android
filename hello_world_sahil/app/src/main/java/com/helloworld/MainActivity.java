package com.helloworld;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;

public class MainActivity extends Activity {
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Context ctx = getApplicationContext();
    
    Utl.showToast(ctx, "I am superman!");
    
    Utl.showNotification(ctx, "Daily Reminders", "Life just rock and rolls", "Longboards just rocks down hill !");
    
    // todo: learn how to show a simply image:
    // URL url = new URL("https://i.picsum.photos/id/202/536/354.jpg?hmac=yO3GuY1TtL9JGeHctCd_n_LRx7XrmqDHOMESFbbN5lM");
    // Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
    // imageView.setImageBitmap(bmp);

    // todo: learn how to fetch some json data and show raw json text in a textview:
  }
}