package com.elevensproject.instagram_like_profile_image_cropper;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
public class profile_img_cropper extends AppCompatActivity {




    private int RESULT_LOAD_IMG = 1;
    //motion actions:
    private int mode=0;
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private static final int FIXZ = 3;
    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();
    private PointF start = new PointF();
    private PointF mid  = new PointF();
    private PointF newmid  = new PointF();
    float imageTX;
    float imageTY;
    ImageView baseimg;
    Button select_btn;
    Button ok_btn;
    Bitmap bm;
    int screen_width;
    float scale=0;
    float newscale;
    float MAX_SCALE_FACT = 3.5f;
    float ZOOM_DURATION = 200;
    static final Interpolator myInterpolator = new AccelerateDecelerateInterpolator();

    private float oldDist = 1f;
    float[] f = new float[9];
    float dw,dh;
    float oldx,oldy;
    boolean first_time= false;
    Translate_animation animation_T;
    Scale_animation animation_S;
    int imageWidth;
    int imageHeight;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_img_cropper);
        Display display = getWindowManager().getDefaultDisplay();
        screen_width = display.getWidth();

        select_btn =(Button)findViewById(R.id.select_image_pick_new_image);
        ok_btn = (Button)findViewById(R.id.select_image_confirm);

        select_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
            }
        });
        ok_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bitmap result = get_current_img(baseimg);
                Intent intent = new Intent();
                intent.putExtra("bitmap",result);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });

        baseimg = (ImageView)findViewById(R.id.img);
        baseimg.setMaxHeight(display.getWidth());
//        baseimg.setOnTouchListener(img_touch);
        baseimg.getLayoutParams().height = display.getWidth();
        baseimg.requestLayout();
        // intial_view();
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
    }

    public void intial_view(){
        matrix = new Matrix();
//        bm = BitmapFactory.decodeResource(this.getResources(),
//                R.drawable.test);
        bm = ((BitmapDrawable)baseimg.getDrawable()).getBitmap();
        imageWidth = bm.getWidth();
        imageHeight = bm.getHeight();
        scale = (screen_width * 1.0f) / imageWidth;
        if (imageWidth > imageHeight) {
            scale = (screen_width * 1.0f) / imageHeight;
        }
        imageTX = (screen_width-imageWidth * scale )/2;
        imageTY = (screen_width - imageHeight * scale)/2;
        matrix.postScale(scale, scale);
        matrix.postTranslate(imageTX,imageTY);

        baseimg.setImageMatrix(matrix);
        baseimg.setImageBitmap(bm);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            // When an Image is picked
            if (requestCode == RESULT_LOAD_IMG && resultCode == Activity.RESULT_OK
                    && null != data) {
                // Get the Image from data
                Uri selectedImage = data.getData();

                String[] filePathColumn = { MediaStore.Images.Media.DATA };

                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();
                File f = new File(picturePath);
                if(f.exists()){
                    Picasso.with(this)
                            .load(selectedImage)
                            .into(baseimg, new Callback() {
                                @Override
                                public void onSuccess() {
                                    intial_view();
                                    baseimg.setOnTouchListener(img_touch);
                                }

                                @Override
                                public void onError() {

                                }
                            });
                }
                // Set the Image in ImageView after decoding the String
//

            } else {
                Toast.makeText(this, "You haven't picked Image",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG)
                    .show();
        }

    }


    View.OnTouchListener img_touch = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // handle touch events here
            ImageView view = (ImageView) v;
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:        // one finger down
                    savedMatrix.set(matrix);
                    start.set(event.getX(), event.getY());
                    mode = DRAG;
                    view.removeCallbacks(animation_T);
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:   // two finger down
                    oldDist = spacing(event);
                    if (oldDist > 10f) {
                        savedMatrix.set(matrix);
                        midPoint(mid, event);
                        start.set(mid.x,mid.y);
                        mode = ZOOM;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if(mode == DRAG) {
                        matrix.getValues(f);
                        dw = -(f[Matrix.MSCALE_X] * imageWidth - screen_width);
                        dh = -(f[Matrix.MSCALE_Y] * imageHeight - screen_width);

                        float destX = f[Matrix.MTRANS_X], destY = f[Matrix.MTRANS_Y];
                        boolean fixdrag = false;
                        if (f[Matrix.MTRANS_X] > 0) {
                            destX = 0;
                            fixdrag = true;
                        }
                        if (f[Matrix.MTRANS_Y] > 0) {
                            destY = 0;
                            fixdrag = true;
                        }

                        if (f[Matrix.MTRANS_X] < dw) {
                            destX = dw;
                            fixdrag = true;
                        }

                        if (f[Matrix.MTRANS_Y] < dh) {
                            destY = dh;
                            fixdrag = true;
                        }

                        if (fixdrag) {
                            animation_T = new Translate_animation(destX, destY, f[Matrix.MTRANS_X], f[Matrix.MTRANS_Y], view,200);
                            view.post(animation_T);
                        }
                    }
                    if(mode == FIXZ) {
                        float destS = 1;
                        boolean fixscale = false;
                        matrix.getValues(f);
                        float scaleX = f[Matrix.MSCALE_X];
                        if (scaleX < scale) {   //check if it is less than minimum scale that leave no black space in the frame
                            fixscale = true;
                            destS = scale;
                        }
                        if (scaleX > MAX_SCALE_FACT * scale) {  //check if it is too big
                            fixscale = true;
                            destS = MAX_SCALE_FACT * scale;
                        }

                        if (fixscale) {
                            //start fix error zoom animation
                            animation_S = new Scale_animation(destS, destS, f[Matrix.MSCALE_X], f[Matrix.MSCALE_Y], mid.x, mid.y, view);
                            view.post(animation_S);

                        }else{
                            matrix.getValues(f);
                            dw = -(f[Matrix.MSCALE_X] * imageWidth - screen_width);
                            dh = -(f[Matrix.MSCALE_Y] * imageHeight - screen_width);
                            float destX = f[Matrix.MTRANS_X], destY = f[Matrix.MTRANS_Y];
                            boolean fixdrag = false;
                            if (f[Matrix.MTRANS_X] > 0) {
                                destX = 0;
                                fixdrag = true;
                            }
                            if (f[Matrix.MTRANS_Y] > 0) {
                                destY = 0;
                                fixdrag = true;
                            }
                            if (f[Matrix.MTRANS_X] < dw) {
                                destX = dw;
                                fixdrag = true;
                            }
                            if (f[Matrix.MTRANS_Y] < dh) {
                                destY = dh;
                                fixdrag = true;
                            }
                            if (fixdrag) {
                                animation_T = new Translate_animation(destX, destY, f[Matrix.MTRANS_X], f[Matrix.MTRANS_Y], view,200);
                                view.post(animation_T);
                            }
                        }
                        //  mode = DRAG;
                    }
                    mode = NONE;
                    break;
                case MotionEvent.ACTION_POINTER_UP:   //any finger up
                    first_time = true;
                    mode = FIXZ;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                        matrix.set(savedMatrix);
                        matrix.postTranslate(event.getX() - start.x, event.getY()- start.y);
                    } else if (mode == ZOOM) {
                        float newDist = spacing(event);
                        matrix.set(savedMatrix);
                        if (newDist > 10f) {
                            newscale = (newDist / oldDist) ;
                            matrix.postScale(newscale, newscale, mid.x, mid.y);
                        }
                        midPoint(newmid, event);
                        matrix.postTranslate(newmid.x - start.x, newmid.y- start.y);
                    }else if(mode == FIXZ){
                        if(first_time) {
                            oldx = event.getX() - newmid.x;
                            oldy = event.getY() - newmid.y;
                            first_time = false;
                        }
                        matrix.set(savedMatrix);
                        matrix.postScale(newscale, newscale, mid.x, mid.y);
                        matrix.postTranslate(event.getX()-oldx - start.x, event.getY()-oldy- start.y);
                    }
                    break;
            }
            view = (ImageView) v;
            view.setImageMatrix(matrix);
            //   view.invalidate();
            return true;

        }
    };



    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void check_position(Matrix in_matrix,ImageView view){
        in_matrix.getValues(f);
        dw = -(f[Matrix.MSCALE_X] * imageWidth - screen_width);
        dh = -(f[Matrix.MSCALE_Y] * imageHeight - screen_width);

        float destX = f[Matrix.MTRANS_X];
        float destY = f[Matrix.MTRANS_Y];
        boolean fixdrag = false;
        if (f[Matrix.MTRANS_X] > 0) {
            destX = 0;
            fixdrag = true;
        }
        if (f[Matrix.MTRANS_Y] > 0) {
            destY = 0;
            fixdrag = true;
        }

        if (f[Matrix.MTRANS_X] < dw) {
            destX = dw;
            fixdrag = true;
        }

        if (f[Matrix.MTRANS_Y] < dh) {
            destY = dh;
            fixdrag = true;
        }
        if(fixdrag){
            in_matrix.postTranslate(destX - f[Matrix.MTRANS_X], destY - f[Matrix.MTRANS_Y]);
            view.setImageMatrix(in_matrix);
        }

    }

    private Bitmap get_current_img(ImageView v){
        v.setDrawingCacheEnabled(true);
        v.buildDrawingCache();
        Bitmap result_map = v.getDrawingCache();
        result_map = Bitmap.createScaledBitmap(result_map, 200, 200, true);
        v.destroyDrawingCache();
        return result_map;

    }

    private class Translate_animation implements Runnable{
        float dx,dy;
        final float endx,endy;
        final float startx,starty;
        final long mStartTime;
        final ImageView imgv;
        final int animation_speed;
        public Translate_animation(final float inx,final float iny,final float start_x,final float start_y,ImageView imgview,int speed){
            endx = inx;
            endy = iny;
            imgv = imgview;
            startx = start_x;
            starty = start_y;
            animation_speed = speed;
            mStartTime = System.currentTimeMillis();
        }
        @Override
        public void run() {
            if(imgv == null)return;

            matrix.getValues(f);
            float t = interpolate(animation_speed);

            dx = (endx-startx)*t+startx-f[Matrix.MTRANS_X];
            dy = (endy-starty)*t+starty-f[Matrix.MTRANS_Y];
            Log.d("dx:"," dx: "+dx +" old x: "+f[Matrix.MTRANS_X] +" destination x: "+endx);
            matrix.postTranslate(dx, dy);

            imgv.setImageMatrix(matrix);
            if (t < 1.0f) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    imgv.postOnAnimation(this);
                } else {
                    imgv.postDelayed(this, 1000 / 60);
                }
            }
        }

        private float interpolate(int animation_speed) {

            long currentT = System.currentTimeMillis();
            float t = 1f*(currentT-mStartTime)/animation_speed;
            Log.d("t:"," t is "+t);
            t = Math.min(1.0f, t);

            t = myInterpolator.getInterpolation(t);
            return t;
        }
    }

    private class Scale_animation implements Runnable{
        float ds;
        final float endsx,endsy;
        final float startsx,startsy;
        final long mStartTime;
        final float cx,cy;
        final ImageView imgv;
        final Matrix endmatrix=new Matrix();
        public Scale_animation(final float insx,final float insy,
                               final float start_sx,final float start_sy,
                               final float centerx,final float centery,
                               ImageView imgview){
            endsx = insx;
            endsy = insy;
            imgv = imgview;
            startsx = start_sx;
            startsy = start_sy;
            cx = centerx;
            cy = centery;
            mStartTime = System.currentTimeMillis();
            Log.d("sdc","start x distance and scale : "+f[Matrix.MTRANS_X]+" "+f[Matrix.MSCALE_X] );
            Log.d("sdc","center x and y : "+centerx+" "+centery );

        }
        @Override
        public void run() {

            if(imgv == null)return;

            matrix.getValues(f);
            float t = interpolate();

//            Log.d("dsx:", " dsx: " + ds + " old sx: " + f[Matrix.MTRANS_X] + " destination x: " + endsx);
            Log.d("sdc","scale distance change : "+f[Matrix.MTRANS_X] );

            ds = (t*(endsx-startsx)+startsx)/f[Matrix.MSCALE_X];

            matrix.postScale(ds, ds, cx, cy);

            imgv.setImageMatrix(matrix);
            if (t < 1.0f) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    imgv.postOnAnimation(this);
                } else {
                    imgv.postDelayed(this, 1000 / 60);
                }
            }else{
                check_position(matrix,imgv);
            }
        }

        private float interpolate() {

            long currentT = System.currentTimeMillis();
            float t = 1f*(currentT-mStartTime)/ZOOM_DURATION;
            Log.d("t:"," t is "+t);
            t = Math.min(1.0f, t);

            t = myInterpolator.getInterpolation(t);
            return t;
        }
    }

}
