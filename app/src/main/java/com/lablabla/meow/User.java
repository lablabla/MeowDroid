package com.lablabla.meow;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {

    public String fcmToken;
    public String displayName;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String fcmToken, String displayName) {
        this.fcmToken = fcmToken;
        this.displayName = displayName;
    }
}
