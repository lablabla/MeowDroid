package com.lablabla.meow;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "MainActivity";

    private GoogleApiClient mGoogleApiClient;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private FirebaseUser mFirebaseUser;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;


    private boolean mIsTokenReady;
    private boolean mIsFriendsFetched;

    private LinearLayout mProgressBarLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mIsTokenReady = false;
        mIsFriendsFetched = false;
        configureUI();

        mProgressBarLayout.setVisibility(View.VISIBLE);
        mAuth = FirebaseAuth.getInstance();
        configureDatabase();
        configureCloudMessaging();

        mProgressBarLayout.setVisibility(View.INVISIBLE);
    }

    private void configureCloudMessaging() {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId  = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
        }

        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
                Log.d(TAG, "Key: " + key + " Value: " + value);
            }
        }

        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        String token = task.getResult().getToken();
                        mFirebaseUser = mAuth.getCurrentUser();
                        User user = new User(token, mFirebaseUser.getDisplayName());
                        //saveUserToken(mFirebaseUser.getUid(), user);
                        fetchFriends();
                        // Log and toast
                        String msg = getString(R.string.msg_token_fmt, token);
                        Log.d(TAG, msg);
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void configureDatabase() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void configureUI() {
        Button signoutButton = findViewById(R.id.sign_out_button);
        signoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuth.signOut();

                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();
                // Build a GoogleSignInClient with the options specified by gso.
                GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(MainActivity.this, gso);
                mGoogleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        Intent intent = new Intent(MainActivity.this, SplashActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
            }
        });
        mProgressBarLayout = findViewById(R.id.llProgressBar);
        recyclerView = findViewById(R.id.my_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

    }

    private void saveUserToken(String uid, User user)
    {
        //mDatabase.child("users").child(uid).setValue(user);
    }

    private void fetchFriends()
    {
        if (mFirebaseUser == null)
        {
            Log.e(TAG, "ERROR!: Tried fetching friend for invalid user");
            return;
        }
        HashMap<String, String> names = new HashMap();
        names.put("John", "John");
        names.put("Tim", "Tim");
        names.put("Sam", "Sam");
        names.put("Ben", "Ben");

        final Object lockObject = new Object();
        //mDatabase.child("users").child(mFirebaseUser.getUid()).child("friends").setValue(names);
        mDatabase.child("users").child(mFirebaseUser.getUid()).child("friends")
                .addValueEventListener(new ValueEventListener() {
            private boolean mIsInitial = false;
            private int mNumberOfFriends;
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!mIsInitial)
                {
                    mIsInitial = true;

                    final List<User> list = new ArrayList<>();
                    Map<String, String> friendsUids = (Map<String, String>) dataSnapshot.getValue();
                    if (friendsUids == null)
                    {
                        return;
                    }
                    mNumberOfFriends = friendsUids.size();

                    for (String friendUid : friendsUids.values())
                    {
                        mDatabase.child("users").child(friendUid).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                User user = dataSnapshot.getValue(User.class);
                                synchronized (lockObject)
                                {
                                    list.add(user);
                                    mNumberOfFriends--;
                                    if (mNumberOfFriends == 0)
                                    {

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {

                                                mAdapter = new MeowAdapter(list);
                                                recyclerView.setAdapter(mAdapter);
                                            }
                                        });
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void taskFinished()
    {
        if (mIsTokenReady && mIsFriendsFetched)
        {
            mProgressBarLayout.setVisibility(View.INVISIBLE);
        }
    }
}
