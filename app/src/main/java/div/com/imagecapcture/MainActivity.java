package div.com.imagecapcture;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private static final String TAG = "MainActivity";

    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS : {
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
            super.onManagerConnected(status);
        }
    };

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Open CV not loaded");
        } else {
            Log.d(TAG, "Open CV Loaded");
        }
    }


    protected static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 0;
    private SurfaceView SurView;
    private SurfaceHolder camHolder;

    private boolean previewRunning;
    private Button button1;
    final Context context = this;
    public Camera camera = null;
    private ImageView camera_image;
    private Bitmap bmp, bmp1;
    private ByteArrayOutputStream bos;
    private BitmapFactory.Options options, o, o2;
    private FileInputStream fis;
    ByteArrayInputStream fis2;
    private FileOutputStream fos;
    private File dir_image2, dir_image;
    private RelativeLayout CamView;
    private RelativeLayout rectLayout;

    int left, top, width, height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CamView = (RelativeLayout) findViewById(R.id.camview);

        SurView = (SurfaceView)findViewById(R.id.sview);
        camHolder = SurView.getHolder();
        //camHolder.setFormat(PixelFormat.RGBA_8888);

        camHolder.addCallback(this);
        camHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        button1 = (Button)findViewById(R.id.button_1);

        rectLayout = (RelativeLayout) findViewById(R.id.rectLayoutId);
        ShapeDrawable rectShapeDrawable = new ShapeDrawable(); // pre defined class

        // get paint
        Paint paint = rectShapeDrawable.getPaint();

        // set border color, stroke and stroke width
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(15); // you can change the value of 5
        rectLayout.setBackgroundDrawable(rectShapeDrawable);

        left = rectLayout.getLeft();
        top = rectLayout.getTop() + 300;
        width = rectLayout.getWidth() + 800;
        height = rectLayout.getHeight() + 300;

        camera_image = (ImageView) findViewById(R.id.camera_image);

        button1.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                button1.setClickable(false);
                button1.setVisibility(View.INVISIBLE);  //<-----HIDE HERE
                //rectLayout.setVisibility(View.INVISIBLE);
                camera.takePicture(null, null, mPicture);
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try{
            camera=Camera.open();
            camera.setDisplayOrientation(90);
        }catch(Exception e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"Error", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(previewRunning){
            camera.stopPreview();
        }
        try{
            camera.setPreviewDisplay(camHolder);
            camera.startPreview();
            previewRunning=true;
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.release();
        camera=null;
    }

    public void TakeScreenshot(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int nu = preferences.getInt("image_num",0);
        nu++;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("image_num",nu);
        editor.commit();
        CamView.setDrawingCacheEnabled(true);
        CamView.buildDrawingCache(true);
        bmp = Bitmap.createBitmap(CamView.getDrawingCache());
        CamView.setDrawingCacheEnabled(false);
        bos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        byte[] bitmapdata = bos.toByteArray();
        fis2 = new ByteArrayInputStream(bitmapdata);

        String picId=String.valueOf(nu);
        String myfile="MyImage"+picId+".jpeg";

        dir_image = new  File(Environment.getExternalStorageDirectory()+"/My_Custom_Folder");
        dir_image.mkdirs();

        try {
            File tmpFile = new File(dir_image,myfile);
            fos = new FileOutputStream(tmpFile);

            byte[] buf = new byte[1024];
            int len;
            while ((len = fis2.read(buf)) > 0) {
                fos.write(buf, 0, len);
            }
            fis2.close();
            fos.close();

            Mat mat = new Mat();
            Utils.bitmapToMat(bmp, mat);
            //double edge = detectEdges(mat);

            int bitsPerPixel = ImageFormat.getBitsPerPixel(camera.getParameters().getPreviewFormat());
            double totalSizeBitPerPixel = rectLayout.getWidth() * rectLayout.getHeight() * bitsPerPixel / 8;

            //double percent = ((edge *100)/totalSizeBitPerPixel);

            //Log.d("TIMER","Rect area : "+ totalSizeBitPerPixel +" Meena : "+edge + " Percentage : "+percent+"%");

//            Toast.makeText(getApplicationContext(),
//                    "The file is saved at :/My_Custom_Folder/"+"MyImage"+picId+".jpeg"+"Rect area : "+ totalSizeBitPerPixel +" Meena : "+edge+" Percentage : "+percent+"%",Toast.LENGTH_LONG).show();
            Toast.makeText(getApplicationContext(),
                    "The file is saved at :/My_Custom_Folder/"+"MyImage"+picId+".jpeg"+"Rect area : "+ totalSizeBitPerPixel,Toast.LENGTH_LONG).show();


            bmp1 = null;
            camera_image.setImageBitmap(bmp1);
            camera.startPreview();
            button1.setClickable(true);
            button1.setVisibility(View.VISIBLE);//<----UNHIDE HER
            rectLayout.setVisibility(View.VISIBLE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            final Uri contentUri = Uri.fromFile(dir_image);
            scanIntent.setData(contentUri);
            sendBroadcast(scanIntent);
        } else {
            final Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory()));
            sendBroadcast(intent);
        }
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            dir_image2 = new  File(Environment.getExternalStorageDirectory()+"/My_Custom_Folder");
            dir_image2.mkdirs();

            double randomNo = Math.random();

            int angleToRotate = getRoatationAngle(MainActivity.this, Camera.CameraInfo.CAMERA_FACING_FRONT);
            // Solve image inverting problem
            //angleToRotate = angleToRotate + 180;
            Bitmap orignalImage = BitmapFactory.decodeByteArray(data, 0, data.length);
            Bitmap bitmapImage = rotate(orignalImage, angleToRotate);

            File tmpFile = new File(dir_image2,"TempImage"+randomNo +".jpg");
            try {
                fos = new FileOutputStream(tmpFile);
                //fos.write(data);
                bitmapImage.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                fos.close();
            } catch (FileNotFoundException e) {
                Toast.makeText(getApplicationContext(),"Error",Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(),"Error",Toast.LENGTH_LONG).show();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                final Uri contentUri = Uri.fromFile(dir_image2);
                scanIntent.setData(contentUri);
                sendBroadcast(scanIntent);
            } else {
                final Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory()));
                sendBroadcast(intent);
            }

            cropImage(bitmapImage);
        }
    };

    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.postRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    /**
     * Get Rotation Angle
     *
     * @param mContext
     * @param cameraId
     *            probably front cam
     * @return angel to rotate
     */
    public static int getRoatationAngle(Activity mContext, int cameraId) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = mContext.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    public void cropImage(Bitmap srcBmp) {
        camera_image.setImageBitmap(srcBmp);

        left = rectLayout.getLeft() + 100;
        top = rectLayout.getTop() + 370;
        width = rectLayout.getWidth() + 700;
        height = rectLayout.getHeight() + 300;

        Bitmap dstBmp = Bitmap.createBitmap(srcBmp,left,top,width,height);

        double picId= Math.random();
        String myfile="MyImage"+picId+".jpeg";

        dir_image = new  File(Environment.getExternalStorageDirectory()+"/Crop_Img");
        dir_image.mkdirs();

        try {
            File tmpFile = new File(dir_image,myfile);
            fos = new FileOutputStream(tmpFile);
            dstBmp.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.close();

            bmp1 = null;
            //camera_image.setImageBitmap(bmp1);
            camera.startPreview();
            button1.setClickable(true);
            button1.setVisibility(View.VISIBLE);//<----UNHIDE HER
            rectLayout.setVisibility(View.VISIBLE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            final Uri contentUri = Uri.fromFile(dir_image);
            scanIntent.setData(contentUri);
            sendBroadcast(scanIntent);
        } else {
            final Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory()));
            sendBroadcast(intent);
        }

        Mat mat = new Mat();
        Utils.bitmapToMat(dstBmp, mat);
        double edge = detectEdges(mat);

        int bitsPerPixel = ImageFormat.getBitsPerPixel(camera.getParameters().getPreviewFormat());
        double totalSizeBitPerPixel = width * rectLayout.getHeight() * bitsPerPixel / 8;

        double percent = ((edge *100)/totalSizeBitPerPixel);

        Log.d("TIMER","Rect area : "+ totalSizeBitPerPixel +" Meena : "+edge + " Percentage : "+percent+"%");

        Toast.makeText(getApplicationContext(),
                    "The file is saved at :/My_Custom_Folder/"+"MyImage"+picId+".jpeg"+"Rect area : "+ totalSizeBitPerPixel +" Meena : "+edge+" Percentage : "+percent+"%",Toast.LENGTH_LONG).show();
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Open CV not loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, baseLoaderCallback);
        } else {
            Log.d(TAG, "Open CV Loaded");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public double detectEdges(Mat rgbImage) {
        double maxContoursArea = 0;
        //read the RGB image
        //Mat rgbImage = Imgcodecs.imread(path);
        //mat gray image holder
        Mat imageGray = new Mat();
        //mat canny image
        Mat imageCny = new Mat();

        Mat imagedst = new Mat();

        Mat imageBlurr= new Mat();

        Imgproc.cvtColor(rgbImage, imageGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(imageGray, imageBlurr, new org.opencv.core.Size(5,5), 0);

        Imgproc.bilateralFilter(imageBlurr, imagedst, 10, 50, 0);
        Imgproc.Canny(imagedst, imageCny, 10, 100, 3, true);

        Imgproc.dilate(imageCny, imageCny, new Mat(), new Point(-1, 1), 50);
        Imgproc.erode(imageCny, imageCny, new Mat(), new Point(-1, 1), 50);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy= new Mat();
        Imgproc.findContours(imageCny, contours,hierarchy , Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        Mat mat= new Mat();
        Imgproc.cvtColor(imageCny, mat, Imgproc.COLOR_GRAY2BGR);
        List<MatOfPoint> matOfPoints= new ArrayList<MatOfPoint>();

        int maxValIdx = 0;

        if(contours.size() > 0) {
            for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
                //System.out.println("coming here");
                double contoursArea = Imgproc.contourArea(contours.get(idx));
                System.out.println(contoursArea);
                if (maxContoursArea < contoursArea) {
                    maxContoursArea = contoursArea;
                    maxValIdx = idx;
                }
            }
            MatOfPoint contour = contours.get(maxValIdx);
            matOfPoints.add(contour);

            System.out.println(matOfPoints.size());
            // Mat mat= new Mat();
            Imgproc.polylines(mat, matOfPoints, true, new Scalar(255,255,0));
            Imgproc.fillPoly(mat, matOfPoints, new Scalar(0,0,255));
            Mat improvedMat= new Mat();
            Imgproc.cvtColor(mat, improvedMat, Imgproc.COLOR_BGR2GRAY);
            if (maxContoursArea > 20000) {
                //   mainActivity.redirectToImgView(improvedMat);
            }
            Imgcodecs.imwrite("D:\\output.png", improvedMat);


            bmp = Bitmap.createBitmap(improvedMat.cols(), improvedMat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(improvedMat, bmp);

            bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            byte[] bitmapdata = bos.toByteArray();
            fis2 = new ByteArrayInputStream(bitmapdata);

            double v = Math.random();
            String myfile="MyImage_"+v+".jpeg";

            dir_image = new  File(Environment.getExternalStorageDirectory()+"/Meena");
            dir_image.mkdirs();

            try {
                File tmpFile = new File(dir_image,myfile);
                fos = new FileOutputStream(tmpFile);

                byte[] buf = new byte[1024];
                int len;
                while ((len = fis2.read(buf)) > 0) {
                    fos.write(buf, 0, len);
                }
                fis2.close();
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                final Uri contentUri = Uri.fromFile(dir_image);
                scanIntent.setData(contentUri);
                sendBroadcast(scanIntent);
            } else {
                final Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory()));
                sendBroadcast(intent);
            }

        }
        return maxContoursArea;
    }
}
