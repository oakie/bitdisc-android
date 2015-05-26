# Bitdisc
Disc golf score card application for android. User authentication is handled by the
[android facebook sdk](https://developers.facebook.com/docs/android), text data is synced to a
[firebase cloud storage](https://www.firebase.com/) instance and images are uploaded to
[cloudinary](http://cloudinary.com/).

## Install
1. Clone repo:
```bash
        git clone git@github.com:oakie/bitdisc-android.git
```
2. Create a file in `app/src/main/res/values/secrets.xml` containing:
```mxml
        <?xml version="1.0" encoding="utf-8"?>
        <resources>
          <string name="firebase_url">YOUR_FIREBASE_URL</string>
          <string name="facebook_app_id">YOUR_FACEBOOK_APP_ID</string>
          <string name="cloudinary_url">YOUR_CLOUDINARY_URL</string>
        </resources>
```
3. Build and enjoy!
