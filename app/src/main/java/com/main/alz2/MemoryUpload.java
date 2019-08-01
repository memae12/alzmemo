package com.main.alz2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.main.alz2.lib.ALZUrl;
import com.main.alz2.lib.CallBack;
import com.main.alz2.lib.HTTPAsyncTask;
import com.main.alz2.lib.ImageLoadTask;
import com.main.alz2.pojo.Relation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

public class MemoryUpload extends AppCompatActivity {
    final Context mContext = this;
    SharedPreferences alzpref;
    Bundle passedData;

    Bitmap pickedImage;

    private static final int SELECT_PICTURE = 1;
    //edited by marly
    private static final int TAKE_PICTURE = 2;
    private static final int DEL_PICTURE = 3;

    // LogCat tag
    private static final String TAG = MainActivity.class.getSimpleName();
    /*************/

    private String selectedImagePath;
    private ImageView imageView;
    private int relationId;
    private String imageUrl;
    private String imageNumber;
    private String imageName;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_upload);
        imageView = (ImageView)findViewById(R.id.imageView);
        setTitle("Update Memory");
        alzpref = this.getSharedPreferences(ALZUrl.ALZ_CONFIG, MODE_PRIVATE);
        passedData = this.getIntent().getExtras();


                ((Button) findViewById(R.id.browseBtn))
                .setOnClickListener(new View.OnClickListener() {

                    public void onClick(View arg0) {
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(intent,
                                "Select Picture"), SELECT_PICTURE);
                    }
                });

        //edited by marly lol
        ((Button) findViewById(R.id.cameraBtn))
                .setOnClickListener(new View.OnClickListener() {

                    public void onClick(View arg0) {
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        // start the image capture Intent
                        startActivityForResult(intent, TAKE_PICTURE);
                    }
                });

        ((Button) findViewById(R.id.saveBtn))
                .setOnClickListener(new View.OnClickListener() {

                    public void onClick(View arg0) {
                        Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
                        Save saveFile = new Save();
                        saveFile.SaveImage(mContext,bitmap);

                    }
                });

        relationId = Integer.parseInt(passedData.get("relationid").toString());
        imageNumber = passedData.getString("imageNumber");
        imageName = passedData.getString("imageName");

        imageUrl = ALZUrl.ALZ_HTTP;
        imageUrl += alzpref.getString(ALZUrl.ALZ_SERVER, "localhost");
        imageUrl += ALZUrl.DEFAULT_FILE_UPLOADS;
        imageUrl += relationId + "/" + imageName;



        if(!(imageName+"").equals("null")) {
            new ImageLoadTask(imageUrl, imageView).execute();
            Log.d(TAG,"image!:"+imageUrl);
        }
    }


    private boolean isDeviceSupportCamera() {
        if (getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 0) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }


        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
                .getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = pixels;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }


    public static Bitmap scaleBitmap(Bitmap bitmap, int wantedWidth, int wantedHeight) {
        float originalWidth = bitmap.getWidth();
        float originalHeight = bitmap.getHeight();
        Bitmap output = Bitmap.createBitmap(wantedWidth, wantedHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Matrix m = new Matrix();


        float scalex = wantedWidth/originalWidth;
        float scaley = wantedHeight/originalHeight;
        float xTranslation = 0.0f, yTranslation = (wantedHeight - originalHeight * scaley)/2.0f;

        m.postRotate(-90);

        m.postTranslate(xTranslation, yTranslation);
        m.preScale(scalex, scaley);

        // m.setScale((float) wantedWidth / bitmap.getWidth(), (float) wantedHeight / bitmap.getHeight());
        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        canvas.drawBitmap(bitmap, m, paint);
        output = Bitmap.createBitmap(output,0,0,output.getWidth(),output.getHeight(),m,true);

        return output;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE || requestCode == TAKE_PICTURE) {
                Uri selectedImageUri = data.getData();
                if (Build.VERSION.SDK_INT < 19) {
                    selectedImagePath = getPath(selectedImageUri);
                    Bitmap bitmap = BitmapFactory.decodeFile(selectedImagePath);
                    //pickedImage = scaleBitmap(bitmap,500,500);
                    pickedImage = getResizedBitmap(pickedImage, 500);
                    imageView.setImageBitmap(pickedImage);

                }
                else {
                    ParcelFileDescriptor parcelFileDescriptor;
                    try {
                        parcelFileDescriptor = getContentResolver().openFileDescriptor(selectedImageUri, "r");
                        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                        pickedImage = getResizedBitmap(image,500);
                        //pickedImage = scaleBitmap(image, 500,500);
                        //pickedImage = getRoundedCornerBitmap(pickedImage,500);
                        parcelFileDescriptor.close();
                        imageView.setImageBitmap(pickedImage);


                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            //edited by marly loll
            /*else if (requestCode == DEL_PICTURE)
            {
                Toast.makeText(mContext, "del Successful!", Toast.LENGTH_SHORT).show();
                Intent returnIntent = new Intent(mContext,MenuActivity.class);
                startActivity(returnIntent);

            }
            /**********************/
        }
    }

    /**
     * helper to retrieve the path of an image URI
     */
    public String getPath(Uri uri) {
        if( uri == null ) {
            return null;
        }
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if( cursor != null ){
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        return uri.getPath();
    }

    public void uploadImage(View view){

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pickedImage.compress(Bitmap.CompressFormat.JPEG, 50, baos); //bitmap is required image which have to send  in Bitmap form
        byte[] byteArray = baos.toByteArray();

        String encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT);

        taskUploadImage(encodedImage);
    }

    void taskUploadImage( String encodedImage )
    {
        final CallBack callback = new CallBack()
        {
            @Override
            public void onProgress() {}

            @Override
            public void onResult(String result) {
                JSONArray content = null;
                try
                {
                    JSONObject jsonRootObject = new JSONObject(result);
                    if (!jsonRootObject.getString("message").equals("success"))
                    {
                        Toast.makeText(mContext, "Error in upload", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(mContext, "Upload Successful!", Toast.LENGTH_SHORT).show();
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("result",result);
                    setResult(Activity.RESULT_OK,returnIntent);
                    finish();
                }catch(JSONException e){}
            }
            @Override
            public void onCancel() {}
        };
        String url = ALZUrl.ALZ_HTTP;
        url += alzpref.getString(ALZUrl.ALZ_SERVER,"localhost");
        url += ALZUrl.DEFAULT_URL_UPLOADER_PROC;
        JSONObject getData = new JSONObject();
        HashMap<Object, Object> data = new HashMap<>();
        data.put("encodedImage", encodedImage);
        data.put("relationId", relationId);
        data.put("imageNumber", imageNumber);
        HTTPAsyncTask asyncTask = new HTTPAsyncTask(mContext,callback, data, null, "POST");
        asyncTask.execute(url);
    }
}