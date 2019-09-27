package com.example.docusx.rlchat;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private TextView profileName, profileStatus, profileFriendsCount;
    private Button profileSendFriendRequestBtn, profileDeclineFriendReqBtn;

    private String current_state;

    private DatabaseReference databaseReference;
    private DatabaseReference friendRequestDatabase;
    private DatabaseReference friendDatabase;
    private DatabaseReference notificationDatabase;
    private FirebaseUser currentUser;

    View progressOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final String user_id = getIntent().getStringExtra("userID");

        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
        friendRequestDatabase = FirebaseDatabase.getInstance().getReference().child("Friend_req");
        friendDatabase = FirebaseDatabase.getInstance().getReference().child("Friends");
        notificationDatabase = FirebaseDatabase.getInstance().getReference().child("Notifications");
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        progressOverlay = findViewById(R.id.progress_overlay);
        profileName = (TextView) findViewById(R.id.profileDisplayName);
        profileStatus = (TextView) findViewById(R.id.profileUserStatus);
        profileFriendsCount = (TextView) findViewById(R.id.profileTotalFriends);

        profileImageView = (ImageView) findViewById(R.id.profileImageView);
        profileSendFriendRequestBtn = (Button) findViewById(R.id.profileSendFriendRequest);
        profileDeclineFriendReqBtn = (Button) findViewById(R.id.profileDeclineFriendRequestBtn);

        current_state = "not_friends";

        progressOverlay.setVisibility(View.VISIBLE);
        progressOverlay.isShown();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String displayName = dataSnapshot.child("name").getValue().toString();
                String userStatus = dataSnapshot.child("status").getValue().toString();
                String profileImage = dataSnapshot.child("image").getValue().toString();

                profileName.setText(displayName);
                profileStatus.setText(userStatus);

                Picasso.get().load(profileImage).placeholder(R.drawable.default_square_img).into(profileImageView);

                profileDeclineFriendReqBtn.setVisibility(View.INVISIBLE);
                profileDeclineFriendReqBtn.setEnabled(false);

                // Friends list and request feature

                friendRequestDatabase.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if(dataSnapshot.hasChild(user_id)) {
                            final String req_type = dataSnapshot.child(user_id).child("request_type").getValue().toString();

                            if(req_type.equals("received")) {
                                current_state = "req_received";
                                profileSendFriendRequestBtn.setText("Accept Friend Request");

                                profileDeclineFriendReqBtn.setVisibility(View.VISIBLE);
                                profileDeclineFriendReqBtn.setEnabled(true);

                                if(profileDeclineFriendReqBtn.isPressed()) {

                                    profileDeclineFriendReqBtn.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            friendRequestDatabase.child(currentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    friendRequestDatabase.child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            profileDeclineFriendReqBtn.setVisibility(View.INVISIBLE);
                                                            profileDeclineFriendReqBtn.setEnabled(false);

                                                            current_state = "not_friends";
                                                            profileSendFriendRequestBtn.setText("SEND FRIEND REQUEST");
                                                        }
                                                    });
                                                }
                                            });
                                        }
                                    });

                                }else {
                                    profileSendFriendRequestBtn.setText("Accept Friend Request");

                                    profileDeclineFriendReqBtn.setVisibility(View.VISIBLE);
                                    profileDeclineFriendReqBtn.setEnabled(true);
                                }

                            }else if(req_type.equals("sent")){
                                current_state = "req_sent";
                                profileSendFriendRequestBtn.setText("Cancel Friend Request");

                                profileDeclineFriendReqBtn.setVisibility(View.INVISIBLE);
                                profileDeclineFriendReqBtn.setEnabled(false);
                            }

                            progressOverlay.setVisibility(View.INVISIBLE);
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                        }else {

                            friendDatabase.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {

                                    if(dataSnapshot.hasChild(user_id)) {
                                        current_state = "friends";
                                        profileSendFriendRequestBtn.setText("Unfriend this person");

                                        profileDeclineFriendReqBtn.setVisibility(View.INVISIBLE);
                                        profileDeclineFriendReqBtn.setEnabled(false);
                                    }

                                    progressOverlay.setVisibility(View.INVISIBLE);
                                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                }


                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                    current_state = "friends";
                                    profileSendFriendRequestBtn.setText("Unfriend this person");

                                }
                            });

                        }

                        progressOverlay.setVisibility(View.INVISIBLE);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        profileSendFriendRequestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                profileSendFriendRequestBtn.setEnabled(false);

                if(current_state.equals("not_friends")) {
                    friendRequestDatabase.child(currentUser.getUid()).child(user_id).child("request_type")
                            .setValue("sent").addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()) {

                                friendRequestDatabase.child(user_id).child(currentUser.getUid()).child("request_type")
                                .setValue("received").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {

                                        HashMap<String, String> notificationData = new HashMap<>();
                                        notificationData.put("from", currentUser.getUid());
                                        notificationData.put("type", "request");

                                        notificationDatabase.child(user_id).push().setValue(notificationData).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {

                                                current_state = "req_sent";
                                                profileSendFriendRequestBtn.setText("Cancel Friend Request");

                                                profileDeclineFriendReqBtn.setVisibility(View.INVISIBLE);
                                                profileDeclineFriendReqBtn.setEnabled(false);

                                            }
                                        });

                                        Toast.makeText(ProfileActivity.this, "Request Sent", Toast.LENGTH_SHORT).show();
                                    }
                                });

                            }else {
                                Toast.makeText(ProfileActivity.this, "Failed Sending Request", Toast.LENGTH_SHORT).show();
                            }
                            profileSendFriendRequestBtn.setEnabled(true);
                        }
                    });
                }

                if(current_state.equals("req_sent")) {
                    friendRequestDatabase.child(currentUser.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            friendRequestDatabase.child(user_id).child(currentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    profileSendFriendRequestBtn.setEnabled(true);
                                    current_state = "not_friends";
                                    profileSendFriendRequestBtn.setText("SEND FRIEND REQUEST");

                                    profileDeclineFriendReqBtn.setVisibility(View.INVISIBLE);
                                    profileDeclineFriendReqBtn.setEnabled(false);
                                }
                            });
                        }
                    });
                }

                // to see friends
                if(current_state.equals("req_received")) {
                    final String currentDate = DateFormat.getDateTimeInstance().format(new Date());
                    friendDatabase.child(currentUser.getUid()).child(user_id).child("date").setValue(currentDate).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            friendDatabase.child(user_id).child(currentUser.getUid()).child("date").setValue(currentDate).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    friendRequestDatabase.child(currentUser.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            friendRequestDatabase.child(user_id).child(currentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    profileSendFriendRequestBtn.setEnabled(true);
                                                    current_state = "friends";
                                                    profileSendFriendRequestBtn.setText("Unfriend this person");

                                                    profileDeclineFriendReqBtn.setVisibility(View.INVISIBLE);
                                                    profileDeclineFriendReqBtn.setEnabled(false);
                                                }
                                            });
                                        }
                                    });
                                }
                            });
                        }
                    });
                }

                // to unfriend a friend
                if(current_state.equals("friends")) {
                    friendDatabase.child(currentUser.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            friendDatabase.child(user_id).child(currentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    profileSendFriendRequestBtn.setEnabled(true);
                                    current_state = "not_friends";
                                    profileSendFriendRequestBtn.setText("SEND A FRIEND REQUEST");
                                }
                            });
                        }
                    });
                }

            }
        });


    }
}
