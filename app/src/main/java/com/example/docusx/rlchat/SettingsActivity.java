package com.example.docusx.rlchat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class SettingsActivity extends AppCompatActivity {

    private DatabaseReference userDatabase;
    private FirebaseUser currentUser;

    private CircleImageView displayImage;
    private TextView displayName;
    private TextView mStatus;
    private Button changeImageButton;
    private Button changeStatusButton;

    static private final int GALLERY_PICK = 1;

    private StorageReference mStorageRef;

    View progressOverlay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mStorageRef = FirebaseStorage.getInstance().getReference();
        progressOverlay = findViewById(R.id.progress_overlay);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserUID = currentUser.getUid();

        userDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserUID);
        userDatabase.keepSynced(true);

        userDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String name = dataSnapshot.child("name").getValue().toString();
                final String image = dataSnapshot.child("image").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String thumb_img = dataSnapshot.child("thumb_image").getValue().toString();

                displayImage = (CircleImageView) findViewById(R.id.settings_profile_image);
                displayName = (TextView) findViewById(R.id.settings_disp_name);
                mStatus = (TextView) findViewById(R.id.settings_status);

                displayName.setText(name);
                mStatus.setText(status);

                if(!image.equals("default")) {
                    //Picasso.get().load(image).placeholder(R.drawable.defaultimg).into(displayImage);

                    Picasso.get().load(image).networkPolicy(NetworkPolicy.OFFLINE)
                            .placeholder(R.drawable.defaultimg).into(displayImage, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(Exception e) {
                            Picasso.get().load(image).placeholder(R.drawable.defaultimg).into(displayImage);
                        }
                    });
                }

                changeStatusButton = (Button) findViewById(R.id.settings_change_status_btn);
                changeStatusButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        String statusValue = mStatus.getText().toString();
                        Intent statusIntent = new Intent(SettingsActivity.this, StatusActivity.class);
                        statusIntent.putExtra("status_value", statusValue);
                        startActivity(statusIntent);
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        changeImageButton = (Button) findViewById(R.id.settings_change_img_btn);
        changeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), GALLERY_PICK); */

                            // OR

                // start picker to get image for cropping and then use the image in cropping activity
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(5, 3)
                        .setMinCropWindowSize(500, 500)
                        .start(SettingsActivity.this);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                progressOverlay.setVisibility(View.VISIBLE);
                progressOverlay.isShown();

                Uri resultUri = result.getUri();

                File thumb_filePath = new File(resultUri.getPath());

                String currentUserID = currentUser.getUid();

                Bitmap thumb_bitmap = null;
                try {
                    thumb_bitmap = new Compressor(this)
                            .setMaxWidth(200)
                            .setMaxHeight(200)
                            .setQuality(60)
                            .compressToBitmap(thumb_filePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    thumb_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    final byte[] thumb_byte = baos.toByteArray();



                StorageReference filePath = mStorageRef.child("profile_images").child(currentUserID + ".jpg");
                final StorageReference thumbFilePath = mStorageRef.child("profile_images").child("thumbs").child(currentUserID+ ".jpg");

                filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull final Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful()) {
                            final String downloadURL = task.getResult().getDownloadUrl().toString();

                            UploadTask uploadTask = thumbFilePath.putBytes(thumb_byte);
                            uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> thumb_task) {

                                    String thumb_downloadURL = thumb_task.getResult().getDownloadUrl().toString();

                                    if(thumb_task.isSuccessful()) {

                                        Map update_hashMap = new HashMap<>();
                                        update_hashMap.put("image", downloadURL);
                                        update_hashMap.put("thumb_image", thumb_downloadURL);

                                        userDatabase.updateChildren(update_hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if(task.isSuccessful()) {
                                                    progressOverlay.setVisibility(View.INVISIBLE);
                                                    Toast.makeText(SettingsActivity.this, "image uploaded", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    }else {
                                        progressOverlay.setVisibility(View.INVISIBLE);
                                        Toast.makeText(SettingsActivity.this, "Error in uploading thumbline image.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });


                        }else {
                            progressOverlay.setVisibility(View.INVISIBLE);
                            Toast.makeText(SettingsActivity.this, "Error in uploading image.", Toast.LENGTH_SHORT).show();

                        }
                    }
                });

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

}

