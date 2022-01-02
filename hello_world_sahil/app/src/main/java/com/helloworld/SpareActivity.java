package com.helloworld;

import android.app.Activity;
import android.os.Bundle;

public class SpareActivity extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_spare);
    // LEARN: Sahil: Observation: If my one activity get not responding, I can't use other activity via the other icon I have in apps or via switching to the other activity with the use of ```adb shell am start -n com.helloworld/com.helloworld.MainActivity```.

    // URL url = new URL("https://i.picsum.photos/id/202/536/354.jpg?hmac=yO3GuY1TtL9JGeHctCd_n_LRx7XrmqDHOMESFbbN5lM");
    // Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
    // imageView.setImageBitmap(bmp);
  }
}