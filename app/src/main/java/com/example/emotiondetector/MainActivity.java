package com.example.emotiondetector;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.emotiondetector.ml.Model;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    static Model model;

    private static final int CAMERA_PIC_REQUEST = 2, GALLERY_PIC_REQUEST = 1;
    public ImageView mImage;
    public TextView resultView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImage = findViewById(R.id.imageView);
        resultView = findViewById(R.id.textView);
        try {
            model = Model.newInstance(this);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"Cannot Load Model",Toast.LENGTH_SHORT).show();
            resultView.setText("Error: Model can't be loaded");
        }
    }

    public void openGallery(View view) {
        Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, GALLERY_PIC_REQUEST);
    }

    public void openCamera(View view) {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case GALLERY_PIC_REQUEST: {
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = data.getData();
                    Bitmap mImageBitmap = null;
                    try {
                        mImageBitmap = Bitmap.createScaledBitmap(MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage),240,240,false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mImage.setImageBitmap(mImageBitmap);
                    detect(mImageBitmap);
                }
                break;
            }
            case CAMERA_PIC_REQUEST: {
                if (resultCode == RESULT_OK) {
                    Bitmap image = (Bitmap) data.getExtras().get("data");
                    mImage.setImageBitmap(image);
                }
                break;
            }
        }
    }

    public void detect(Bitmap mImageBitmap){
        float[] floatArray =  toGrayscale(Bitmap.createScaledBitmap(mImageBitmap,48,48,false));

        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 48, 48, 1}, DataType.FLOAT32);
        inputFeature0.loadArray(floatArray);

        Model.Outputs outputs = model.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
        float[] result = outputFeature0.getFloatArray();
        resultView.setText("We detected your emotion as '"+resultToEmotion(result)+"'");
    }

    private float[] toGrayscale(Bitmap bmpOriginal)
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