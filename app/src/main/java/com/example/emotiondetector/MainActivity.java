package com.example.emotiondetector;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
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


import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.types.TUint8;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PIC_REQUEST = 2;
    public ImageView mImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImage = findViewById(R.id.imageView);
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
        Bitmap testImage = toGrayscale(Bitmap.createScaledBitmap(mImageBitmap,48,48,false));
        TensorImage tensorImage = new TensorImage(DataType.UINT8);
        tensorImage.load(testImage);
        Log.d("LOG",tensorImage.toString());

        SavedModelBundle model = SavedModelBundle.load("model.h5");
        Tensor input = (Tensor) tensorImage;
        Tensor output = model.session().runner().feed("INPUT_TENSOR", input).fetch("OUTPUT_TENSOR", 7).run().get(0);
        Log.d("Tensor",output.toString());

//        String simpleMlp = new ClassPathResource("model.h5").getFile().getPath();
//        MultiLayerNetwork model = KerasModelImport.importKerasSequentialModelAndWeights(simpleMlp);
//        Toast.makeText(getApplicationContext(),"DONE",Toast.LENGTH_SHORT).show();
    }

    public Bitmap toGrayscale(Bitmap bmpOriginal)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();
        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }
}