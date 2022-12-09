package com.schneewittchen.rosandroid.widgets.location;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.schneewittchen.rosandroid.R;
import com.schneewittchen.rosandroid.ui.views.widgets.PublisherWidgetView;

import android.location.Location;
import android.util.Log;
import android.view.WindowManager;


/**
 * TODO: Description
 *
 * @author Gennaro Raiola
 * @version 0.0.1
 * @created on 19.11.22
 */
public class LocationView extends PublisherWidgetView {

    public static final String TAG = LocationView.class.getSimpleName();

    Context context;
    Paint buttonPaint;
    TextPaint textPaint;
    StaticLayout staticLayout;

    double gpsLatitude;
    double gpsLongitude;
    double gpsAltitude;
    double networkLatitude;
    double networkLongitude;
    double networkAltitude;
    long gpsTime = 0;
    long networkTime = 0;

    BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getBundleExtra("Location");
            Location location = (Location) bundle.getParcelable("Location");
            if (location != null) {
                publishCoordinates(location);
            }
        }
    };

    public LocationView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public LocationView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    private void init() {

        ((Activity) getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        buttonPaint = new Paint();
        buttonPaint.setColor(getResources().getColor(R.color.colorPrimary));
        buttonPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        textPaint = new TextPaint();
        textPaint.setColor(Color.BLACK);
        textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        textPaint.setTextSize(26 * getResources().getDisplayMetrics().density);


        IntentFilter filter = new IntentFilter();
        filter.addAction("LOCATION_UPDATE");
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(context).registerReceiver(locationReceiver, filter);

        context.startService(new Intent(context, LocationService.class));
    }

    private void changeState(boolean pressed) {
        LocationEntity entity = (LocationEntity) widgetEntity;
        entity.buttonPressed = pressed;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.editMode) {
            return super.onTouchEvent(event);
        }

        LocationEntity entity = (LocationEntity) widgetEntity;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if(entity.buttonPressed) {
                    changeState(false);
                }
                else {
                    changeState(true);
                }
                break;
            default:
                return false;
        }

        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        float textLayoutWidth = width;

        LocationEntity entity = (LocationEntity) widgetEntity;

        if (entity.rotation == 90 || entity.rotation == 270) {
            textLayoutWidth = height;
        }

        if(!entity.buttonPressed)
            buttonPaint.setColor(getResources().getColor(R.color.colorPrimary));
        else
            buttonPaint.setColor(getResources().getColor(R.color.color_attention));

        canvas.drawRect(new Rect(0, 0, (int) width, (int) height), buttonPaint);

        staticLayout = new StaticLayout(entity.text,
                textPaint,
                (int) textLayoutWidth,
                Layout.Alignment.ALIGN_CENTER,
                1.0f,
                0,
                false);
        canvas.save();
        canvas.rotate(entity.rotation, width / 2, height / 2);
        canvas.translate(((width / 2) - staticLayout.getWidth() / 2), height / 2 - staticLayout.getHeight() / 2);
        staticLayout.draw(canvas);
        canvas.restore();
    }

    public void publishCoordinates(Location location) {

        LocationEntity entity = (LocationEntity) widgetEntity;

        if(entity.buttonPressed) {

            double latitude;
            double longitude;
            double altitude;
            String type;

            if(location.getProvider().equals(LocationManager.GPS_PROVIDER))
            {
                gpsLatitude = location.getLatitude();
                gpsLongitude = location.getLongitude();
                gpsAltitude = location.getAltitude();
                gpsTime = location.getTime();
            }
            else if(location.getProvider().equals(LocationManager.NETWORK_PROVIDER))
            {
                networkLatitude = location.getLatitude();
                networkLongitude = location.getLongitude();
                networkAltitude = location.getAltitude();
                networkTime = location.getTime();
            }
            if( 0 < gpsTime - networkTime) {
                latitude = gpsLatitude;
                longitude = gpsLongitude;
                altitude = gpsAltitude;
                type = "GPS";
            }
            else
            {
                latitude = networkLatitude;
                longitude = networkLongitude;
                altitude = networkAltitude;
                type = "NETWORK";
            }
            Log.d(TAG, type + " Longitude: " + longitude + " Latitude: " + latitude + " Altitude " + altitude);
            this.publishViewData(new LocationData(latitude, longitude, altitude, type));
        }
    }
}
