package com.example.emotiondetector;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.emotiondetector.ml.Model;
import com.google.android.gms.common.util.ArrayUtils;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.types.TUint8;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    static Model model;

    private static final int CAMERA_PIC_REQUEST = 2;
    public ImageView mImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImage = findViewById(R.id.imageView);
        try {
            model = Model.newInstance(this);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"Cannot Load Model",Toast.LENGTH_SHORT).show();
        }
    }

    public void openGallery(View view) {
        Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        final int ACTIVITY_SELECT_IMAGE = 1234;
        startActivityForResult(i, ACTIVITY_SELECT_IMAGE);
    }

    public void openCamera(View view) {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case 1234: {
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = data.getData();
                    Bitmap mImageBitmap = null;
                    try {
                        mImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    /* Now you have choosen image in Bitmap format in object "yourSelectedImage". You can use it in way you want! */
                    mImage.setImageBitmap(mImageBitmap);
                    try {
                        startProcessing(mImageBitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
            case 2: {
                if (resultCode == RESULT_OK) {
                    Bitmap image = (Bitmap) data.getExtras().get("data");
                    mImage.setImageBitmap(image);
                }
                break;
            }
        }
    }

    public void startProcessing(Bitmap mImageBitmap) throws IOException{
        float[] floatArray =  toGrayscale(Bitmap.createScaledBitmap(mImageBitmap,48,48,false));

        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 48, 48, 1}, DataType.FLOAT32);
        inputFeature0.loadArray(floatArray);

        Model.Outputs outputs = model.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
        float[] result = outputFeature0.getFloatArray();
        //TODO
        Toast.makeText(getApplicationContext(),resultToEmotion(result),Toast.LENGTH_SHORT).show();
    }

    public float[] toGrayscale(Bitmap bmpOriginal)
    {
        float[] array = new float[bmpOriginal.getWidth()*bmpOriginal.getHeight()];
        for (int x=0;x<bmpOriginal.getWidth();x++){
            for (int y=0;y<bmpOriginal.getHeight();y++){
                int pixel = bmpOriginal.getPixel(x,y);
                int red = Color.red(pixel);
                int blue = Color.blue(pixel);
                int green = Color.green(pixel);
                float bw = ((float)(red+blue+green))/765;
                array[x*bmpOriginal.getHeight()+y] = (bw);
            }
        }
        return array;
    }
    public String resultToEmotion(float[] result){
        float max= 0;int p=0;
        for(int i=0;i<7;i++){
            if(result[i]>max){
                p=i;
                max=result[p];
            }
        }
        switch(p){
            case 0: return("Angry");
            case 1: return("Disgust");
            case 2: return("Fear");
            case 3: return("Happy");
            case 4: return("Sad");
            case 5: return("Surprise");
            case 6: return("Neutral");
        }
        return "ERROR";
    }
}