package watkins.lisa.com.treadmillocr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final String TAG = MainActivity.class.getSimpleName();

    private Button takePhotoButton;
    private ImageView imageView;
    private TextView analyzedText;
    private Button retryButton;

    private Uri currentPhotoUri;
    private String currentPhotoFilePath;
    private Bitmap rotatedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        takePhotoButton = findViewById(R.id.take_photo);
        imageView = findViewById(R.id.image);
        analyzedText = findViewById(R.id.analyzed_text);
        retryButton = findViewById(R.id.retry);

        takePhotoButton.setOnClickListener(new TakePhotoOnClickListener());
        retryButton.setOnClickListener(new RetryOnClickListener());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            try {
                Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), currentPhotoUri);

                rotatedBitmap = ensureImageIsInPortraitOrientation(imageBitmap);

                takePhotoButton.setVisibility(View.GONE);
                imageView.setVisibility(View.VISIBLE);
                analyzedText.setVisibility(View.VISIBLE);
                retryButton.setVisibility(View.VISIBLE);
                // imageView.setImageBitmap(rotatedBitmap);
                photoAnalysis(rotatedBitmap);
            } catch (Exception e) {
                Log.d(TAG, e.getLocalizedMessage());
                takePhotoButton.setText("Retry");
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.d(TAG, ex.getLocalizedMessage());
            }

            if (photoFile != null) {
                currentPhotoUri = FileProvider.getUriForFile(this,
                        "watkins.lisa.com.treadmillocr",
                        photoFile);
                currentPhotoFilePath = photoFile.getAbsolutePath();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void photoAnalysis(Bitmap bitmap) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

        FirebaseVisionTextDetector detector = FirebaseVision.getInstance()
                .getVisionTextDetector();

        detector.detectInImage(image)
                .addOnSuccessListener(new MyTextDetectorOnSuccessListener())
                .addOnFailureListener(new MyTextDetectorOnFailureListener());
    }

    private Bitmap ensureImageIsInPortraitOrientation(Bitmap bitmap) {
        try {
            ExifInterface ei = new ExifInterface(currentPhotoFilePath);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            Bitmap rotatedBitmap;
            switch(orientation) {

                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotatedBitmap = rotateImage(bitmap, 90);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotatedBitmap = rotateImage(bitmap, 180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotatedBitmap = rotateImage(bitmap, 270);
                    break;

                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    rotatedBitmap = bitmap;
            }

            return rotatedBitmap;
        } catch (Exception e) {
            Log.d(TAG, e.getLocalizedMessage());
            return null;
        }
    }

    private Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    private class MyTextDetectorOnSuccessListener implements OnSuccessListener<FirebaseVisionText> {
        @Override
        public void onSuccess(FirebaseVisionText firebaseVisionText) {
            // StringBuilder allText = new StringBuilder();

            // make canvas
            Canvas canvas = new Canvas(rotatedBitmap);
            imageView.draw(canvas);

            // create transparent paint
            Paint paint = new Paint();
            paint.setColor(Color.TRANSPARENT);
            paint.setStyle(Paint.Style.FILL);

            for (FirebaseVisionText.Block block: firebaseVisionText.getBlocks()) {
                // String text = block.getText();

                for (FirebaseVisionText.Line line: block.getLines()) {
                    // ...
                    for (FirebaseVisionText.Element element: line.getElements()) {
                        Rect rect = element.getBoundingBox();

                        canvas.drawRect(rect, paint);

                        // create border
                        paint.setStrokeWidth(10);
                        paint.setColor(Color.BLACK);
                        paint.setStyle(Paint.Style.STROKE);
                        canvas.drawRect(rect, paint);
                    }
                }

                // allText.append(text + "\n");
            }
            imageView.setImageBitmap(rotatedBitmap);
            // analyzedText.setText(allText.toString());
        }
    }

    private class MyTextDetectorOnFailureListener implements OnFailureListener {
        @Override
        public void onFailure(@NonNull Exception e) {
            Log.d(TAG, e.getLocalizedMessage());
        }
    }

    private class TakePhotoOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            dispatchTakePictureIntent();
        }
    }

    private class RetryOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            imageView.setVisibility(View.GONE);
            analyzedText.setVisibility(View.GONE);
            retryButton.setVisibility(View.GONE);
            takePhotoButton.setVisibility(View.VISIBLE);
            rotatedBitmap = null;
            currentPhotoUri = null;
        }
    }
}