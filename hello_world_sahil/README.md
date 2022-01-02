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
