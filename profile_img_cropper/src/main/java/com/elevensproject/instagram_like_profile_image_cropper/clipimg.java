package com.elevensproject.instagram_like_profile_image_cropper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;

/**
 * Created by Jesse on 2015-12-01.
 */
public class clipimg extends View {
    private int screen_width;
    private Paint paint=null;
    public clipimg(Context context) {
        super(context);
        Display display = ((profile_img_cropper) getContext()).getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        screen_width = width;
    }

    public clipimg(Context context, AttributeSet attrs) {
        super(context, attrs);
        Display display = ((profile_img_cropper) getContext()).getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        screen_width = width;
    }

    public clipimg(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Display display = ((profile_img_cropper) getContext()).getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        screen_width = width;
    }


    private final String graycolor= "#aa000000";
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float radius = screen_width/2;
        Paint eraser = new Paint();
        eraser.setColor(0XFFFFFFFF);
        eraser.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

     //   canvas.drawColor(Color.parseColor(graycolor));
        Bitmap b = Bitmap.createBitmap(screen_width, screen_width, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        b.eraseColor(Color.TRANSPARENT);
        c.drawColor(Color.parseColor(graycolor));
        c.drawCircle(radius,radius,radius,eraser);
        canvas.drawBitmap(b,0,0,null);
    }
}
