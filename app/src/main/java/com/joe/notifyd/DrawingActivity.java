package com.joe.notifyd;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.joe.notifyd.RealmObjects.Notification;
import com.joe.notifyd.Util.Constants;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;
import com.pes.androidmaterialcolorpickerdialog.ColorPicker;

import java.io.ByteArrayOutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.pedant.SweetAlert.SweetAlertDialog;
import io.realm.Realm;
import io.realm.RealmResults;
import me.panavtec.drawableview.DrawableViewConfig;

public class DrawingActivity extends AppCompatActivity {

    /**
     * FINAL
     */

    private final String SELECTED_COLOR_KEY = "selectedColor";
    private final String SELECTED_COLOR_RED_KEY = "selectedRed";
    private final String SELECTED_COLOR_GREEN_KEY = "selectedGreen";
    private final String SELECTED_COLOR_BLUE_KEY = "selectedBlue";

    /**
     * UI
     */
    @BindView(R.id.drawingView)
    me.panavtec.drawableview.DrawableView drawingView;

    @BindView(R.id.floatingActionButton)
    com.github.clans.fab.FloatingActionButton floatingActionButton;

    FloatingActionMenu actionMenu;

    /**
     * NON UI
     */

    //all initialized when the drawing view is initialized
    private int color;
    private float maxZoom;
    private float minZoom;
    private float strokeWidth;

    private SharedPreferences sharedPreferences;

    //Used to get the width/height to set the canvas width/height
    private Display display;

    private Realm realm;

    private int currentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawing);

        ButterKnife.bind(this);

        initVars();

        setupDrawingView();

        setupFloatingActionButton();
    }

    /**
     * initializes variables
     */
    private void initVars(){
        sharedPreferences = this.getSharedPreferences(
                getResources().getString(R.string.app_name), Context.MODE_PRIVATE);

        display = getWindowManager().getDefaultDisplay();

        realm = Realm.getDefaultInstance();

        currentId = getIntent().getIntExtra(Constants.ID_KEY,-1);
    }

    private void setupFloatingActionButton(){

        int size = (int)getResources().getDimension(R.dimen.radial_menu_image);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                size,
                size );

        //stroke width
        SubActionButton.Builder widthBuilder = new SubActionButton.Builder(this);
        widthBuilder.setBackgroundDrawable(ContextCompat.getDrawable(this,R.drawable.sub_action_item_background));
        ImageView widthIcon = new ImageView(this);
        widthIcon.setImageDrawable( ContextCompat.getDrawable(this,R.drawable.ic_format_paint_white_24dp));
        SubActionButton widthButton = widthBuilder.setContentView(widthIcon).setLayoutParams(params).build();

        //color
        SubActionButton.Builder colorBuilder = new SubActionButton.Builder(this);
        colorBuilder.setBackgroundDrawable(ContextCompat.getDrawable(this,R.drawable.sub_action_item_background));
        ImageView colorIcon = new ImageView(this);
        colorIcon.setImageDrawable( ContextCompat.getDrawable(this,R.drawable.ic_color_lens_white_24dp));
        SubActionButton colorButton = colorBuilder.setContentView(colorIcon).setLayoutParams(params).build();
        colorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchColorPicker();
            }
        });

        //max zoom
        SubActionButton.Builder maxZoomBuilder = new SubActionButton.Builder(this);
        maxZoomBuilder.setBackgroundDrawable(ContextCompat.getDrawable(this,R.drawable.sub_action_item_background));
        ImageView maxZoomIcon = new ImageView(this);
        maxZoomIcon.setImageDrawable( ContextCompat.getDrawable(this,R.drawable.ic_zoom_in_white_24dp));
        SubActionButton maxZoomButton = maxZoomBuilder.setContentView(maxZoomIcon).setLayoutParams(params).build();

        //min zoom
        SubActionButton.Builder minZoomBuilder = new SubActionButton.Builder(this);
        minZoomBuilder.setBackgroundDrawable(ContextCompat.getDrawable(this,R.drawable.sub_action_item_background));
        ImageView minZoomIcon = new ImageView(this);
        minZoomIcon.setImageDrawable( ContextCompat.getDrawable(this,R.drawable.ic_zoom_out_white_24dp));
        SubActionButton minZoomButton = minZoomBuilder.setContentView(minZoomIcon).setLayoutParams(params).build();




        actionMenu = new FloatingActionMenu.Builder(this)
                .addSubActionView(widthButton)
                .addSubActionView(colorButton)
                .addSubActionView(maxZoomButton)
                .addSubActionView(minZoomButton)
                .attachTo(floatingActionButton)
                .setRadius((int)getResources().getDimension(R.dimen.radial_menu_radius))
                .build();


    }

    /**
     * sets the configuration options for the drawing view
     */
    private void setupDrawingView(){
        DrawableViewConfig config = new DrawableViewConfig();

        color = sharedPreferences.getInt(SELECTED_COLOR_KEY,-1);

        if(color == -1){
            //default black
            color = ContextCompat.getColor(this,R.color.md_black_1000);
        }

        config.setStrokeColor(color);

        config.setShowCanvasBounds(true); // If the view is bigger than canvas, with this the user will see the bounds (Recommended)

        strokeWidth = 20.f;
        config.setStrokeWidth(strokeWidth);

        minZoom = 1.0f;
        maxZoom = 3.0f;
        config.setMinZoom(minZoom);
        config.setMaxZoom(maxZoom);

        config.setCanvasHeight(getCanvasHeight());
        config.setCanvasWidth(getCanvasWidth());
        drawingView.setConfig(config);


        getSavedBitmap();
    }

    private void getSavedBitmap(){
        RealmResults<Notification> results = realm.where(Notification.class).equalTo("ID",currentId).findAll();

        Log.d("D","drawingDebug results.size = " + results.size());

        if(results.size() > 0 && realm != null){
            Notification toEdit = realm.where(Notification.class)
                    .equalTo("ID", currentId).findFirst();

            byte[] image = toEdit.getImage();

            Log.d("D","drawingDebug image.length = " + image.length);

            Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);

            drawingView.setBackground(new BitmapDrawable(getResources(), bitmap));

        }

    }

    /**
     * converts drawable to bitmap
     * @param drawable
     * @return
     */
    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;
        try{
            if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                if(bitmapDrawable.getBitmap() != null) {
                    return bitmapDrawable.getBitmap();
                }
            }

            if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
                bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
            } else {
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            }

            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }catch (Exception e){
            bitmap = null;
        }
        return bitmap;
    }

    /**
     * get device width to set canvas width
     * @return - device width
     */
    private int getCanvasWidth(){
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    /**
     * get Height to set canvas height
     * @return - device height
     */
    private int getCanvasHeight(){
        Point size = new Point();
        display.getSize(size);
        return size.y;
    }

    /**
     * displays color picker
     */
    private void launchColorPicker(){

        Log.d("D","launching color picker");

        final ColorPicker cp = new ColorPicker(this, sharedPreferences.getInt(SELECTED_COLOR_RED_KEY,0), sharedPreferences.getInt(SELECTED_COLOR_GREEN_KEY,0), sharedPreferences.getInt(SELECTED_COLOR_BLUE_KEY,0));
        /* Show color picker dialog */
        cp.show();

        /* On Click listener for the dialog, when the user select the color */
        Button okColor = (Button)cp.findViewById(R.id.okColorButton);
        okColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int selectedColorRGB = cp.getColor();
                int red = cp.getRed();
                int blue = cp.getBlue();
                int green = cp.getGreen();

                sharedPreferences.edit().putInt(SELECTED_COLOR_RED_KEY,red).apply();
                sharedPreferences.edit().putInt(SELECTED_COLOR_BLUE_KEY,blue).apply();
                sharedPreferences.edit().putInt(SELECTED_COLOR_GREEN_KEY,green).apply();
                sharedPreferences.edit().putInt(SELECTED_COLOR_KEY,selectedColorRGB).apply();

                reconfigureColor(selectedColorRGB);

                cp.dismiss();

                actionMenu.close(true);
            }
        });
    }

    /**
     * sets color of the drawing view
     * @param colorToUse - color to set
     */
    private void reconfigureColor(int colorToUse){
        DrawableViewConfig config = new DrawableViewConfig();

        color = colorToUse;
        config.setStrokeColor(color);

        config.setStrokeWidth(strokeWidth);
        config.setMinZoom(minZoom);
        config.setMaxZoom(maxZoom);

        config.setShowCanvasBounds(true); // If the view is bigger than canvas, with this the user will see the bounds (Recommended)

        config.setCanvasHeight(getCanvasHeight());
        config.setCanvasWidth(getCanvasWidth());
        drawingView.setConfig(config);

    }

    /**
     * displays the delete message
     */
    private void showDeleteMessage(){
        new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Are you sure?")
                .setContentText("Won't be able to recover this drawing!")
                .setConfirmText("Yes, delete it!")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.dismissWithAnimation();
                        drawingView.clear();
                    }
                })
                .show();
    }

    /**
     * displays generic error message
     * @param title
     */
    private void displayGenericErrorMessageWithTitle(String title){
        new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Warning")
                .setContentText(title)
                .setConfirmText("Okay")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.dismissWithAnimation();
                    }
                })
                .show();
    }

    /**
     * displays generic success message with no title
     */
    private void displayGenericSuccessMessage(final boolean finishActivity){
        new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("Success")
                .setConfirmText("Okay")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.dismissWithAnimation();
                        if(finishActivity){
                            Intent returnIntent = new Intent();
                            returnIntent.putExtra(Constants.ID_ACTIVITY_RESULT_GOOD_KEY,currentId);
                            setResult(Activity.RESULT_OK,returnIntent);
                            finish();
                        }
                    }
                })
                .show();
    }

    /**
     * merge 2 bitmaps
     * @param bmp1
     * @param bmp2
     * @return
     */
    public static Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, new Matrix(), null);
        canvas.drawBitmap(bmp2, 0, 0, null);
        return bmOverlay;
    }

    /**
     * saves drawing to database
     */
    private void save(){
        if(!getIntent().hasExtra(Constants.ID_KEY)){
            displayGenericErrorMessageWithTitle("An error has occurred while saving (1)");
            return;
        }

        Bitmap bitmap = drawingView.obtainBitmap();

        Bitmap background = drawableToBitmap(drawingView.getBackground());

        Bitmap toSave;
        if(background != null){
            Log.d("D","drawingDebug background is not null");
            toSave = overlay(bitmap,background);
        }else{
            Log.d("D","drawingDebug background is null");
            toSave = bitmap;
        }

        currentId = getIntent().getIntExtra(Constants.ID_KEY,-1);

        Log.d("D","drawingDebug in DrawingActivity with currentId = " + currentId);

        if(currentId == -1){
            displayGenericErrorMessageWithTitle("An error has occurred while saving (2)");
            return;
        }

        RealmResults<Notification> results = realm.where(Notification.class).equalTo("ID",currentId).findAll();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        toSave.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        Log.d("D","drawingDebug in DrawingActivity with byteArray.length = " + byteArray.length + " " + results.size());

        if(results.size() > 0 && realm != null){
            Notification toEdit = realm.where(Notification.class)
                    .equalTo("ID", currentId).findFirst();


            realm.beginTransaction();
            toEdit.setImage(byteArray);
            realm.commitTransaction();
        }else{
            realm.beginTransaction();

            // Create an object
            Notification notification = realm.createObject(Notification.class);
            notification.setID(currentId);
            notification.setImage(byteArray);
            realm.commitTransaction();
        }

        displayGenericSuccessMessage(true);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.drawing_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                showDeleteMessage();
                return true;

            case R.id.action_save:
                save();
                return true;

            case R.id.action_undo:
                drawingView.undo();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
    }
}
