package org.mkonchady.renuka;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.widget.TextView;

public class PhotoView extends View {

    private static final int INVALID_POINTER_ID = -1;

    public Bitmap bitmap;                               // bitmap to hold the photo
    private int widthBitmap;
    private int heightBitmap;
    int widthCanvas;
    int heightCanvas;

    private Matrix canvasForwardTransform = new Matrix();     // transform from map xy to canvas xy
    private Matrix canvasReverseTransform = new Matrix();     // transform from canvas xy to map xy

    // scale and position of canvas on map to handle drag
    private float mxScaleFactor = 1.0f;
    private float myScaleFactor = 1.0f;
    private float mPosX;       private float mPosY;
    final DisplayMetrics displayMetrics;
    final float dpHeight, dpWidth;
    private Paint backgroundPaint = new Paint();

    private float mLastTouchX; private float mLastTouchY;
    private int mActivePointerId = INVALID_POINTER_ID;
    private ScaleGestureDetector mScaleDetector;

    // views to show the answers and the status message
    TextView statusView;
    TextView answerView1;
    TextView answerView2;
    TextView answerView3;
    TextView answerView4;

    File[] imageFiles;                                      // file names of all the photos
    ArrayList<Integer> seenImageIds = new ArrayList<>();   // ids of photos that have been seen
    Random random = new Random();
    String chosenAnswer, correctAnswer;
    boolean firstTime = true;

    final Pattern getYear = Pattern.compile("^y(....).*$");
    final int NUM_ANSWERS = 4;
    final int START_YEAR = 1990;
    final int END_YEAR;
    int NUM_IMAGES;

    private MainActivity mainActivity;
    final String TAG = "PhotoView";
    private Context context;

    public PhotoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(Color.WHITE);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mainActivity = (MainActivity) context;
        mainActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // build the bitmap from the map name and scale to the screen dimensions
        END_YEAR = Calendar.getInstance().get(Calendar.YEAR);
        displayMetrics = context.getResources().getDisplayMetrics();

        // convert screen pixels to density independent pixels
        dpHeight = displayMetrics.heightPixels / displayMetrics.density;
        dpWidth = displayMetrics.widthPixels / displayMetrics.density;

        boolean readPermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M; // < 23;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)
            readPermissionGranted = true;

        if (!readPermissionGranted) {
            Log.d(TAG, "Getting Read Permission");
            Intent intent = new Intent(context, PermissionActivity.class);
            intent.putExtra("permission", Manifest.permission.READ_EXTERNAL_STORAGE);
            mainActivity.startActivityForResult(intent, 100);
        }

    }


    @Override
    protected void onDraw(Canvas canvas) {
        //float save_mPosX;  float save_mPosY;
        super.onDraw(canvas);

        if (firstTime) {
            loadImages();
            getViews();
            setQuestion();
            statusView.setText("Select one answer");
            firstTime = false;
        }

        // build the forward / reverse transforms
        forwardTransform(); reverseTransform();
        canvas.drawRect(0.0f, 0.0f, (float) getWidth(), (float) getHeight(), backgroundPaint);
        canvas.drawBitmap(bitmap, canvasForwardTransform, backgroundPaint);
        invalidate();

    }

    // transform from bitmap to canvas
    private void forwardTransform() {
        canvasForwardTransform.reset();
        canvasForwardTransform.postTranslate(-widthBitmap / 2.0f, -heightBitmap / 2.0f);
        canvasForwardTransform.postScale(mxScaleFactor, myScaleFactor);
        canvasForwardTransform.postTranslate(mPosX, mPosY);
    }

    // transform from canvas to bitmap
    private void reverseTransform() {
        canvasReverseTransform.reset();
        canvasReverseTransform.postTranslate(-mPosX, -mPosY);
        canvasReverseTransform.postScale(1.0f / mxScaleFactor, 1.0f / myScaleFactor);
        canvasReverseTransform.postTranslate(widthBitmap / 2.0f, heightBitmap / 2.0f);
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Let the ScaleGestureDetector inspect all events.
        mScaleDetector.onTouchEvent(ev);
        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            // possible click on a city
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                mLastTouchX = x;
                mLastTouchY = y;
                mActivePointerId = ev.getPointerId(0);
                invalidate();
                break;
            }
            // a possible drag
            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                final float x = ev.getX(pointerIndex);
                final float y = ev.getY(pointerIndex);

                // checkBounds(x, y, 0.0f);
                // Only move if the ScaleGestureDetector isn't processing a gesture.
                if (!mScaleDetector.isInProgress()) {
                    final float dx = x - mLastTouchX;
                    final float dy = y - mLastTouchY;
                    mPosX += dx;
                    mPosY += dy;
                    invalidate();
                }
                mLastTouchX = x;
                mLastTouchY = y;
                break;
            }

            case MotionEvent.ACTION_UP: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastTouchX = ev.getX(newPointerIndex);
                    mLastTouchY = ev.getY(newPointerIndex);
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                }
                break;
            }
        }
        return true;
    }

    private void loadImages() {

        // load the image name array
        //name is the file name without the extension, id is the resource ID
/*
        for(int id = (R.drawable.aaaa); id <= (R.drawable.zzzz); id++) {
            String name = getResources().getResourceEntryName(id);
            if (name.startsWith("y"))
                imageNames.put(id, name);
        }
*/
        // All photos are stored in DCIM folder
        String path = Environment.getExternalStorageDirectory().toString()+"/DCIM/Renuka";
        File directory = new File(path);
        imageFiles = directory.listFiles();

        // load the image ids array
        //int i = 0;
        //imageIds = new int[imageNames.size()];
        //for (int id: imageNames.keySet())
        //    imageIds[i++] = id;

        //NUM_IMAGES = imageIds.length;
        NUM_IMAGES = imageFiles.length;
        Log.d(TAG, "No. of Images: " + NUM_IMAGES);
    }

    private void getViews() {
        statusView = mainActivity.statusView;
        answerView1 = mainActivity.answerView1;
        answerView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chosenAnswer = answerView1.getText().toString();
                if (chosenAnswer.equals(correctAnswer)) {
                    answerView1.setBackgroundResource(R.color.DarkGreen);
                    statusView.setText("Correct!! Try this one");
                    setQuestion();
                } else {
                    answerView1.setBackgroundResource(R.color.darkred);
                    statusView.setText("Try again");
                }
            }
        });
        answerView2 = mainActivity.answerView2;
        answerView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chosenAnswer = answerView2.getText().toString();
                if (chosenAnswer.equals(correctAnswer)) {
                    answerView2.setBackgroundResource(R.color.DarkGreen);
                    statusView.setText("Correct!! Try this one");
                    setQuestion();
                } else {
                    answerView2.setBackgroundResource(R.color.darkred);
                    statusView.setText("Try again");
                }

            }
        });

        answerView3 = mainActivity.answerView3;
        answerView3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chosenAnswer = answerView3.getText().toString();
                if (chosenAnswer.equals(correctAnswer)) {
                    answerView3.setBackgroundResource(R.color.DarkGreen);
                    statusView.setText("Correct!! Try this one");
                    setQuestion();
                } else {
                    answerView3.setBackgroundResource(R.color.darkred);
                    statusView.setText("Try again");
                }

            }
        });
        answerView4 = mainActivity.answerView4;
        answerView4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chosenAnswer = answerView4.getText().toString();
                if (chosenAnswer.equals(correctAnswer)) {
                    answerView4.setBackgroundResource(R.color.DarkGreen);
                    statusView.setText("Correct!! Try this one");
                    setQuestion();
                } else {
                    answerView4.setBackgroundResource(R.color.darkred);
                    statusView.setText("Try again");
                }

            }
        });

    }

    private void setQuestion() {

        // reset colors of answer views
        answerView1.setBackgroundResource(R.color.DarkBlue);
        answerView2.setBackgroundResource(R.color.DarkCyan);
        answerView3.setBackgroundResource(R.color.DarkGoldenrod);
        answerView4.setBackgroundResource(R.color.DarkMagenta);

        // find a random image
        int tries = 0; int imageNum = 0;
        while (tries < 100) {
            imageNum = random.nextInt(NUM_IMAGES);
            if (!seenImageIds.contains(imageNum))
                break;
            tries++;
        }
        seenImageIds.add(imageNum);

        // set the correct answer
        //bitmap = BitmapFactory.decodeResource(getResources(), imageIds[imageNum]);
       // String imageName = imageNames.get(imageIds[imageNum]);
        String imageName = imageFiles[imageNum].getName();
        Matcher m = getYear.matcher(imageName);
        if (m.matches()) correctAnswer = m.group(1);

        // set the wrong answers
        int[] answers = new int[NUM_ANSWERS];
        int correct = Integer.parseInt(correctAnswer);
        answers[0] = correct;
        answers[1] = correct - 1;
        answers[2] = correct + 1;
        do {
            answers[3] = random.nextInt(END_YEAR - START_YEAR) + START_YEAR;
        } while (answers[3] == answers[0] || answers[3] == answers[1] || answers[3] == answers[2]);

        // shuffle answers
        for (int i = 0; i < NUM_ANSWERS; i++) {
            // choose index uniformly in [i, n-1]
            int r = i + (int) (Math.random() * (NUM_ANSWERS - i));
            int swap = answers[r];
            answers[r] = answers[i];
            answers[i] = swap;
        }

        answerView1.setText(answers[0] + "");
        answerView2.setText(answers[1] + "");
        answerView3.setText(answers[2] + "");
        answerView4.setText(answers[3] + "");

        // extract the scaled down version of the bitmap from mapName
       // Bitmap sampleBitmap = decodeSampledBitmapFromResource(getResources(), imageIds[imageNum], (int) (dpWidth), (int) (dpHeight) );
        Bitmap sampleBitmap =  BitmapFactory.decodeFile(imageFiles[imageNum].getAbsolutePath());
        float bitmapHeight = sampleBitmap.getHeight();
        float bitmapWidth = sampleBitmap.getWidth();
        float screenHeight = displayMetrics.heightPixels;
        float screenWidth = displayMetrics.widthPixels;
        float scaleWidth = screenWidth / bitmapWidth;
        float scaleHeight = screenHeight / bitmapHeight;
        float scale = (scaleHeight >= scaleWidth) ? scaleWidth : scaleHeight;

        // scale the bitmap based on the screen dimensions
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale, 0, 0);
        bitmap = Bitmap.createBitmap(sampleBitmap, 0, 0, (int) bitmapWidth, (int) bitmapHeight, matrix, false);
        mxScaleFactor = 1.0f; myScaleFactor = 1.0f;

        widthBitmap = bitmap.getWidth();
        heightBitmap = bitmap.getHeight();
        widthCanvas = getWidth();         // width of canvas
        heightCanvas = getHeight();       // height of canvas
        mPosX = widthBitmap / 2 ; mPosY = heightBitmap / 2 ;
    }

    // from developer.android.com
    // return a sampled version of the bitmap to save memory
    public Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            // Don't let the object get too small or too large.
            mxScaleFactor *= detector.getScaleFactor();
            mxScaleFactor = Math.max(0.1f, Math.min(mxScaleFactor, 5.0f));
            myScaleFactor *= detector.getScaleFactor();
            myScaleFactor = Math.max(0.1f, Math.min(myScaleFactor, 5.0f));
            invalidate();
            return true;
        }
    }

}