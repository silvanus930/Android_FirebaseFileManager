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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Button upLoadbutton;
    Button choosebutton;
    Button openButton;
    Button folderButton;
    Button upLoadFile;
    ImageView imageView;
    TextView textView;
    EditText text_folder;
    EditText text_file;
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageRef = storage.getReference();
    Uri filePath;
    String fileList = "";
    List<String> filePaths = new ArrayList<>();
    List<String> fileRefs = new ArrayList<>();
    StorageReference mountainsRef = storageRef.child("mountains.jpg");
    StorageReference mountainImagesRef = storageRef.child("images/mountains.jpg");
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        upLoadbutton = (Button)findViewById(R.id.button_Upload);
        choosebutton = (Button)findViewById(R.id.buttonChoose);
        openButton = (Button)findViewById(R.id.buttonOpen);

        folderButton = (Button)findViewById(R.id.root_folder);
        upLoadFile = (Button)findViewById(R.id.upload_file);

        text_folder = (EditText)findViewById(R.id.text_folder);
        text_file = (EditText)findViewById(R.id.text_file);

        imageView = (ImageView)findViewById(R.id.imageView);
        textView = (TextView)findViewById(R.id.textView);

        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            String[] PERMISSIONS = {
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_DOCUMENTS,
            };
            ActivityCompat.requestPermissions(this, PERMISSIONS, 32);
        }

        getListFiles(new File(Environment.getExternalStorageDirectory().toString()));


        // getFileList(Environment.getExternalStorageDirectory().toString());

        // getFileList("/sdcard/");
        textView.setText(fileList);

        folderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fileList = "";
                getListFiles(new File(text_folder.getText().toString()));
            }
        });

        upLoadFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadImage(text_file.getText().toString());
            }
        });

        openButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, 23);
            }
        });

        upLoadbutton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.R)
            @Override
            public void onClick(View view) {
                for(int i = 0 ; i < filePaths.size(); i++)
                    uploadImage(filePaths.get(i));
                saveData2Firebase();
                readDatafromFirebase();
            }
        });

        choosebutton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.R)
            @Override
            public void onClick(View view) {
                SelectImage();
            }
        });
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
                                Log.d("TAG:", document.getId() + " => " + document.getData());
                                fileList += document.getId() + " => " + document.getData() + "\n";
                            }
                            textView.setText(fileList);
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
            user.put("ref", fileRefs.get(i));
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

    public void upLoadfromView()    {
        // Get the data from an ImageView as bytes
        imageView.setDrawingCacheEnabled(true);
        imageView.buildDrawingCache();
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_background);
        // Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = mountainsRef.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Toast.makeText(getApplicationContext(), "upLoad faild!", Toast.LENGTH_SHORT).show();

            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(getApplicationContext(), "success", Toast.LENGTH_SHORT).show();
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...
            }
        });
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

    public void getFileList(String path) {
        File directory = new File(path);
        if (directory != null) {
            File[] files = directory.listFiles();
            if (directory.canRead() && files != null) {
                for (File file : files) {
                    if (file != null) getFileList(file.getAbsolutePath());
                    fileList += file.getAbsolutePath() + "\n";
                }
            } else
                fileList += directory.getAbsolutePath() + "\n";
        }
        textView.setText(fileList);

    }
    private void SelectImage() {
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image from here..."), 22);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    public void upLoadfromStream() {
        InputStream stream = null;
        try {
            // File file = new File(Environment.getExternalStorageDirectory().toString() + "/123.png");
            File file = new File("sdcard/123.png");

            if (!file.exists())
            {
                Toast.makeText(getApplicationContext(), "File is not exist!", Toast.LENGTH_SHORT).show();
                Log.d("----", file.getPath());
                return;
            }
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        UploadTask uploadTask = mountainsRef.putStream(stream);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(getApplicationContext(), "File uploading failed!", Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

            }
        });
    }

    public void uploadImage() {
        if (filePath != null) {
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();
            // StorageReference ref = storageRef.child("images/" + UUID.randomUUID().toString());
            StorageReference ref = storageRef.child(filePath.getPath());
            textView.setText(filePath.toString());
            // filePath = Uri.fromFile(new File(filePath.getPath()));
            // filePath = Uri.parse("content://com.android.exteranalstorage.documents" + filePath.getPath());
            textView.setText(filePath.toString());


            ref.putFile(filePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot){
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Image Uploaded!!", Toast.LENGTH_SHORT).show();
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
                            progressDialog.setMessage("Uploaded " + (int)progress + "%");
                        }
                    }
            );
        }
    }

    private void uploadImage(String path) {
        if (path != null) {
             File file = new File(path);
            if (!file.exists())
            {
                Toast.makeText(this, "File not exits:" + path, Toast.LENGTH_SHORT).show();
                return;
            }

            Uri urifile = Uri.fromFile(new File(path));
            textView.setText(urifile.toString());

            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();
            // StorageReference ref = storageRef.child("images/" + UUID.randomUUID().toString());
            StorageReference ref = storageRef.child(path);
            fileRefs.add(ref.toString());

            ref.putFile(urifile).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot){
                    progressDialog.dismiss();
                    // Toast.makeText(MainActivity.this, "Image Uploaded!!", Toast.LENGTH_SHORT).show();
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

            // Get the Uri of data
            filePath = data.getData();

            Uri urifile = Uri.fromFile(new File(filePath.getPath()));
            textView.setText(filePath.toString() + "\n" + filePath.getPath() + "\n" + urifile.toString());
            text_file.setText(textView.getText());

            try {
                // Setting image on image view using Bitmap
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
                    // permission was granted, Do the you need to do.
                    Toast.makeText(getApplicationContext(), "permission was granted", Toast.LENGTH_SHORT).show();

                } else {

                    // permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }
}
