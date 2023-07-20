package com.example.speechtotext3;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.speechtotext3.databinding.ActivitySpeechToTextBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class SpeechToText extends AppCompatActivity {
    private FirebaseStorage firebaseStorage;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth auth;

    private StorageReference storageReference;

    EditText editText;

    Uri editText2;

    ImageView imageView;
    TextView textView;
    private ActivitySpeechToTextBinding binding;

    public static Integer RecordAudioRequestCode=1;

    private SpeechRecognizer speechRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding= ActivitySpeechToTextBinding.inflate(getLayoutInflater());
        View view=binding.getRoot();
        setContentView(view);
        editText=findViewById(R.id.editText);
        textView=findViewById(R.id.textView);
        imageView=findViewById(R.id.image);

        firebaseStorage=FirebaseStorage.getInstance();
        auth=FirebaseAuth.getInstance();
        firebaseFirestore=FirebaseFirestore.getInstance();
        storageReference=firebaseStorage.getReference();

        if (ContextCompat.checkSelfPermission(SpeechToText.this, Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED){
            checkPermission();
        }
        speechRecognizer=SpeechRecognizer.createSpeechRecognizer(this);
        Intent speechIntent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,500000000);


        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int i) {

            }

            @Override
            public void onResults(Bundle bundle) {

                imageView.setImageResource(R.drawable.baseline_mic_24);
                ArrayList<String> arrayList=bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                editText.setText(arrayList.get(0));
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });
        imageView.setOnTouchListener((view1, motionEvent) -> {
            if (motionEvent.getAction()== MotionEvent.ACTION_UP){
                speechRecognizer.stopListening();
            }
            if (motionEvent.getAction()==MotionEvent.ACTION_DOWN){
                imageView.setImageResource(R.drawable.baseline_mic_24);
                speechRecognizer.startListening(speechIntent);
            }
            return false;
        });
    }

    public void yukle(View view){
        if (editText!=null){
            UUID uuid=UUID.randomUUID();
            String editTextName="editTexts/"+uuid+".txt";
            editText2= Uri.parse(editText.toString());

            storageReference.child(editTextName).putFile(editText2).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                StorageReference newReferance=firebaseStorage.getReference(editTextName);
                newReferance.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        String downloadUrl=uri.toString();
                        String editText2=binding.editText.getText().toString();
                        FirebaseUser user=auth.getCurrentUser();
                        String email=user.getEmail();
                        HashMap<String,Object> postData=new HashMap<>();
                        postData.put("useremail",email);
                        postData.put("downloadUrl",downloadUrl);
                        postData.put("editText",editText2);
                        firebaseFirestore.collection("Posts").add(postData).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                            Intent intent=new Intent(SpeechToText.this,MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                              Toast.makeText(SpeechToText.this,e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(SpeechToText.this,e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
                }
            });
        }
    }
    private void checkPermission() {
        ActivityCompat.requestPermissions(SpeechToText.this, new String[]{
                Manifest.permission.RECORD_AUDIO},RecordAudioRequestCode);

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        speechRecognizer.destroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==RecordAudioRequestCode && grantResults.length>0){
            if (grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Permission Granted",Toast.LENGTH_LONG).show();
            }
        }
    }
    }
