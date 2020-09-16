package com.sinemorzambak.mywordlist;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;

public class MainActivity2 extends AppCompatActivity {

    Bitmap selectedImage;
    ImageView imageView;
    EditText wordText , meaningText,memText;
    Button button;
    SQLiteDatabase database;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        imageView = findViewById(R.id.imageView);
        wordText = findViewById(R.id.wordText);
        meaningText = findViewById(R.id.meaningText);

        memText = findViewById(R.id.memText);
        button = findViewById(R.id.button);

        database = this.openOrCreateDatabase("Words", MODE_PRIVATE,null);


        Intent intent = getIntent();
        String info = intent.getStringExtra("info");
        if (info.matches("new")){
            wordText.setText("");
            meaningText.setText("");

            memText.setText("");
            button.setVisibility(View.VISIBLE);

            Bitmap selectImage = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.src2);
            imageView.setImageBitmap(selectImage);

        } else{
            int wordId = intent.getIntExtra("wordId",1);
            button.setVisibility(View.INVISIBLE);

            try {
                Cursor cursor = database.rawQuery("SELECT * FROM words WHERE id = ?", new String[] {String.valueOf(wordId)});
                int wordIx = cursor.getColumnIndex("wordname");
                int meaningIx = cursor.getColumnIndex("meaning");

                int imageIx = cursor.getColumnIndex("image");
                int memIx = cursor.getColumnIndex("mem");

                while (cursor.moveToNext()){
                    wordText.setText(cursor.getString(wordIx));
                    meaningText.setText(cursor.getString(meaningIx));
                    memText.setText(cursor.getString(memIx));

                    byte[] bytes = cursor.getBlob(imageIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0,bytes.length);
                    imageView.setImageBitmap(bitmap);


                } cursor.close();

            }catch (Exception e) {

            }
        }
    }
    public void selectImage(View view){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intentToGallery,2);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1){
            if (grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentToGallery,2);
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == 2 && resultCode == RESULT_OK && data != null ){
            Uri imageData = data.getData();

            try {
                
                if (Build.VERSION.SDK_INT >= 28 ){
                    ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(),imageData);
                    selectedImage = ImageDecoder.decodeBitmap(source);
                    imageView.setImageBitmap(selectedImage);
                } else {
                    selectedImage= MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageData);
                    imageView.setImageBitmap(selectedImage);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void save(View view){
      String wordName = wordText.getText().toString();
      String meaning = meaningText.getText().toString();
      String mem = memText.getText().toString();
      Bitmap smallImage =makeSmallerImage(selectedImage,300);


      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
      byte[] byteArray = outputStream.toByteArray();

      try {
          database = this.openOrCreateDatabase("Words",MODE_PRIVATE,null);
          database.execSQL("CREATE TABLE IF NOT EXISTS words (id INTEGER PRIMARY KEY, wordname VARCHAR, meaning VARCHAR,sentence VARCHAR, mem VARCHAR, image BLOB)");
          String sqlString = "INSERT INTO words(wordname, meaning, sentence, mem, image) VALUES(?, ?, ?, ?, ?)";
          SQLiteStatement sqLiteStatement= database.compileStatement(sqlString);
          sqLiteStatement.bindString(1,wordName);
          sqLiteStatement.bindString(2, meaning);
          sqLiteStatement.bindString(4,mem);
          sqLiteStatement.bindBlob(5,byteArray);
          sqLiteStatement.execute();

      } catch (Exception e){

      }

      Intent intent = new Intent(MainActivity2.this, MainActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
      //finish();

    }
    public Bitmap makeSmallerImage(Bitmap image, int maximumSize){
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float)height;

        if (bitmapRatio>1){
           width = maximumSize;
           height = (int)(width / bitmapRatio);
        }else {
           height = maximumSize;
           width = (int)(height* bitmapRatio);
        }

        return Bitmap.createScaledBitmap(image, width, height,true);
    }


}