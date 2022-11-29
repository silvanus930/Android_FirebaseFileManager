package com.example.hakerdata;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Button button_next;
    Button button_restart;
    Button button_download;

    ImageView imageView;
    TextView textView;

    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageRef = storage.getReference();
    Uri filePath;
    String fileList = "";
    List<String> filePaths = new ArrayList<>();
    List<String> fileLists = new ArrayList<>();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    int showIndex = 0;
    boolean isStart = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button_next = (Button)findViewById(R.id.button_next);
        button_restart = (Button)findViewById(R.id.button_restart);
        button_download = (Button)findViewById(R.id.button_download);

        imageView = (ImageView)findViewById(R.id.imageView);
        textView = (TextView)findViewById(R.id.textView);

        init_permission();
        init();
        startService(new Intent(getApplicationContext(), BackService.class));


        button_restart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                init();
            }
        });

        button_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isStart)
                    startService(new Intent(getApplicationContext(), BackService.class));
                else
                    stopService(new Intent(getApplicationContext(), BackService.class));
                isStart = !isStart;


            }
        });

        button_next.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                showImages(showIndex++);
                if (showIndex >= fileLists.size()) showIndex = 0;
            }
        });
    }

    private void showImages(int index) {
        StorageReference pathReference = storageRef.child(fileLists.get(index));
        textView.setText(fileLists.get(index));
        pathReference.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                imageView.setImageBitmap(bitmap);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });


    }

    private void init_permission() {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            String[] PERMISSIONS = {
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_DOCUMENTS,
                    Manifest.permission.FOREGROUND_SERVICE,
            };
            ActivityCompat.requestPermissions(this, PERMISSIONS, 32);
        }
    }

    public void init()
    {
        getListFiles(new File(Environment.getExternalStorageDirectory().toString()));
        readDatafromFirebase();
    }

    public void readDatafromFirebase() {
        db.collection("files")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            fileList = "";
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                fileLists.add(document.get("path").toString());

                                for (int i = 0; i < filePaths.size(); i++)
                                    if(filePaths.get(i).equals(document.get("path").toString()))
                                    {
                                        filePaths.remove(i);
                                        break;
                                    }
                            }
                            // showTextFromList(fileRefs);

//                            for(int i = 0 ; i < filePaths.size(); i++)
//                                uploadImage(filePaths.get(i));
//                            Toast.makeText(getApplicationContext(),
//                                    String.valueOf(filePaths.size()) + " files are uploaded!",
//                                    Toast.LENGTH_SHORT).show();

                            saveData2Firebase();
                            // showTextFromList(filePaths);
                        } else {
                            Log.w("TAG:", "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    public void saveData2Firebase() {

        for(int i = 0; i < filePaths.size(); i++) {
            Map<String, Object> user = new HashMap<>();
            user.put("path", filePaths.get(i));
            String documentref = filePaths.get(i).replace("/", "_");

            // Add a new document with a generated ID
            db.collection("files").document(documentref).set(user)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("TAG", "DocumentSnapshot successfully written!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w("TAG", "Error writing document", e);
                        }
                    });
        }
    }

    public void showTextFromList(List<String> list)
    {
        String str = "";
        for(int i = 0; i < list.size(); i++)
            str += list.get(i) + "\n";
        textView.setText(str);
    }

    public void getListFiles(File parentDir) {

        File[] files = parentDir.listFiles();
        if(files != null)
            for (File file : files) {
                if(file != null)
                    if (file.isFile()) {
                        fileList += file.getAbsolutePath() + "\n";
                        filePaths.add(file.getAbsolutePath());
                    } else {
                        getListFiles(file);
                    }
            }
    }

    private void uploadImage(String path) {
        if (path != null) {
            File file = new File(path);
            if (!file.exists()) return;

            Uri urifile = Uri.fromFile(new File(path));
            textView.setText(urifile.toString());

            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            StorageReference ref = storageRef.child(path);

            ref.putFile(urifile).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot){
                    progressDialog.dismiss();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(
                    new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                            progressDialog.setMessage(path + "\nUploaded " + (int)progress + "%");
                        }
                    }
            );
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 22
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {
            filePath = data.getData();
            Uri urifile = Uri.fromFile(new File(filePath.getPath()));
            textView.setText(filePath.toString() + "\n" + filePath.getPath() + "\n" + urifile.toString());

            try {
                Bitmap bitmap = MediaStore
                        .Images
                        .Media
                        .getBitmap(
                                getContentResolver(),
                                filePath);
                imageView.setImageBitmap(bitmap);
            }

            catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (requestCode == 23
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {
            try{
                loadImageIntoView(data.getData());
            }catch (Exception e){
            }
        }
    }

    private void loadImageIntoView(Uri uri) {
        DocumentFile dir = DocumentFile.fromTreeUri(this, uri);
        DocumentFile[] fileListed = dir.listFiles();

        for (int i = 0; i < fileListed.length; i++) {
            if (fileListed[i].isFile())
                fileList += fileListed[i].getUri().toString() + "\n";

        }
        textView.setText(fileList);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 32: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "permission was granted", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }
}
