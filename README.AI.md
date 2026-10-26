# README

Source: [Claude](https://claude.ai/chat/7988ad8e-dfd9-4d24-ac2f-783dd78cca20)

```sh
# List the templates:
android create --list
# Template name               Template description            Tags
# empty-activity (default)    Empty Activity                  compose,activity,agp-9
# empty-activity-lightbuild   Empty Activity (Experimental)   compose,activity,lightbuild


# Create the project with the template, name and output folder:
#   empty-activity: pick one from the --list output (likely something like an empty Compose activity).
#   --name: the display name of the app.
#   --output (or -o): the folder to create, here pomodoro-app.
#   --application-id and --namespace are optional, but setting them avoids com.example defaults.
#   --min-sdk is also optional if you want to override the template’s default.
android create empty-activity \
  --name="Pomodoro App" \
  --output=pomodoro-app \
  --application-id=com.sahilrajput.pomodoro \
  --namespace=com.sahilrajput.pomodoro
# Output:
#   INFO: Processing template 'empty-activity'
#   INFO: Installing Android SDK package 'platforms/android-37' to '/Users/apple/Library/Android/sdk'
#   Package platforms/android-37 not found.
#   INFO: Successfully created project 'Empty Activity' at 'pomodoro-app'
```
