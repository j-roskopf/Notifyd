package com.joe.notifyd;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;

import com.joe.notifyd.RealmObjects.Notification;
import com.joe.notifyd.Util.Constants;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;
import com.pes.androidmaterialcolorpickerdialog.ColorPicker;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

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
    private final String STROKE_WIDTH_FIRST_SELECTED_KEY = "firstTimeSelected";
    private final String STROKE_WIDTH_KEY = "strokeWidth";

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

    //if returned to main activity from editing a notification, pass back the image so we can save it in the db
    private byte[] image;

    Context context;

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

        context = this;
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
        widthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchStrokeWidthPicker();
            }
        });

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

/*
        //max zoom
        SubActionButton.Builder maxZoomBuilder = new SubActionButton.Builder(this);
        maxZoomBuilder.setBackgroundDrawable(ContextCompat.getDrawable(this,R.drawable.sub_action_item_background));
        ImageView maxZoomIcon = new ImageView(this);
        maxZoomIcon.setImageDrawable( ContextCompat.getDrawable(this,R.drawable.ic_zoom_in_white_24dp));
        SubActionButton maxZoomButton = maxZoomBuilder.setContentView(maxZoomIcon).setLayoutParams(params).build();
        maxZoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reconfigureZoom(.5);
            }
        });

        //min zoom
        SubActionButton.Builder minZoomBuilder = new SubActionButton.Builder(this);
        minZoomBuilder.setBackgroundDrawable(ContextCompat.getDrawable(this,R.drawable.sub_action_item_background));
        ImageView minZoomIcon = new ImageView(this);
        minZoomIcon.setImageDrawable( ContextCompat.getDrawable(this,R.drawable.ic_zoom_out_white_24dp));
        SubActionButton minZoomButton = minZoomBuilder.setContentView(minZoomIcon).setLayoutParams(params).build();
        minZoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reconfigureZoom(-.5);
            }
        });
*/




        actionMenu = new FloatingActionMenu.Builder(this)
                .addSubActionView(widthButton)
                .addSubActionView(colorButton)
                //.addSubActionView(maxZoomButton)
                //.addSubActionView(minZoomButton)
                .attachTo(floatingActionButton)
                .setRadius((int)getResources().getDimension(R.dimen.radial_menu_radius))
                .build();

        actionMenu.setStateChangeListener(new FloatingActionMenu.MenuStateChangeListener() {
            @Override
            public void onMenuOpened(FloatingActionMenu floatingActionMenu) {

                RotateAnimation rotate = new RotateAnimation(0f, 405,
                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

                // prevents View from restoring to original direction.
                rotate.setFillAfter(true);

                rotate.setInterpolator(new OvershootInterpolator());

                rotate.setDuration(400);

                floatingActionButton.startAnimation(rotate);
            }

            @Override
            public void onMenuClosed(FloatingActionMenu floatingActionMenu) {

                RotateAnimation rotate = new RotateAnimation(45f, -90f,
                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

                // prevents View from restoring to original direction.
                rotate.setFillAfter(true);

                rotate.setInterpolator(new OvershootInterpolator());

                rotate.setDuration(400);

                floatingActionButton.startAnimation(rotate);

            }
        });


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

        try{
            strokeWidth = Float.parseFloat(sharedPreferences.getString(STROKE_WIDTH_KEY,"20px").replace("px","").trim());
        }catch (Exception e){
            strokeWidth = 20.0f;
        }
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

            if(image != null){
                Log.d("D","drawingDebug image.length = " + image.length);

                Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);

                drawingView.setBackground(new BitmapDrawable(getResources(), bitmap));
            }
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
     * displays stroke width picker
     */
    private void launchStrokeWidthPicker(){

        //close fab menu
        actionMenu.close(true);

        LayoutInflater layoutInflater = LayoutInflater.from(this);

        View promptsView = layoutInflater.inflate(R.layout.stroke_width_spinner, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setView(promptsView);

        alertDialogBuilder.setTitle("Choose stroke width");
        alertDialogBuilder.setIcon(R.drawable.ic_format_paint_black_18dp);

        final AlertDialog alertDialog = alertDialogBuilder.create();

        Spinner spinner = (Spinner) promptsView
                .findViewById(R.id.stroke_width_spinner);

        final List<String> spinnerArray =  new ArrayList<String>();
        spinnerArray.add("10 px");
        spinnerArray.add("20 px");
        spinnerArray.add("30 px");
        spinnerArray.add("40 px");
        spinnerArray.add("50 px");
        spinnerArray.add("60 px");

        //set default value for stroke
        try{
            Log.d("D","strokeWidthDebug with strokeWidht in pref = " + sharedPreferences.getString(STROKE_WIDTH_KEY,"20 px"));
            strokeWidth = Float.parseFloat(sharedPreferences.getString(STROKE_WIDTH_KEY,"20 px").replace("px","").trim());
        }catch (Exception e){
            Log.d("D","strokeWidthDebug with e = " + e);
            strokeWidth = 20.0f;
        }

        Log.d("D","strokeWidthDebug with strokeWidth = " + strokeWidth);


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, spinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if(isFloatingEqual(strokeWidth,10f)){
            spinner.setSelection(0);
        }else if(isFloatingEqual(strokeWidth,20f)){
            spinner.setSelection(1);
        }else if(isFloatingEqual(strokeWidth,30f)){
            spinner.setSelection(2);
        }else if(isFloatingEqual(strokeWidth,40f)){
            spinner.setSelection(3);
        }else if(isFloatingEqual(strokeWidth,50f)){
            spinner.setSelection(4);
        }else if(isFloatingEqual(strokeWidth,60f)){
            spinner.setSelection(5);
        }else{
            spinner.setSelection(0);
        }

        // show it
        alertDialog.show();
        alertDialog.setCanceledOnTouchOutside(true);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            //ignore the first time onItemSelected is fired as it's a rogue
            boolean firstTimeSelected = false;

            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(!firstTimeSelected){
                    firstTimeSelected = true;
                }else{
                    sharedPreferences.edit().putString(STROKE_WIDTH_KEY,spinnerArray.get(position)).apply();
                    reconfigureStrokeWidth(spinnerArray.get(position));

                    strokeWidth = Float.parseFloat(spinnerArray.get(position).replace("px","").trim());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }

        });
    }

    /**
     * Compare to floats for (almost) equality. Will check whether they are
     * at most 5 ULP apart.
     */
    public static boolean isFloatingEqual(float v1, float v2) {
        if (v1 == v2)
            return true;
        float absoluteDifference = Math.abs(v1 - v2);
        float maxUlp = Math.max(Math.ulp(v1), Math.ulp(v2));
        return absoluteDifference < 5 * maxUlp;
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

        Log.d("D","reconfigureColor with strokeWidth + color " + strokeWidth + " " + colorToUse);

        config.setStrokeWidth(strokeWidth);
        config.setMinZoom(minZoom);
        config.setMaxZoom(maxZoom);

        config.setShowCanvasBounds(true); // If the view is bigger than canvas, with this the user will see the bounds (Recommended)

        config.setCanvasHeight(getCanvasHeight());
        config.setCanvasWidth(getCanvasWidth());
        drawingView.setConfig(config);

    }

    /**
     * Changes zoom of drawing view by amount passed
     * @param amountToChange - relative amount to increase / decrease
     */
    private void reconfigureZoom(int amountToChange){
        DrawableViewConfig config = new DrawableViewConfig();

        config.setStrokeColor(color);

        config.setStrokeWidth(strokeWidth);


        if(amountToChange < 0){
            //passed in amount to decrease
            minZoom = minZoom + amountToChange;
        }else{
            maxZoom = maxZoom +  amountToChange;
        }

        if(maxZoom > 5){
            maxZoom = 3;
        }


        config.setMinZoom(minZoom);
        config.setMaxZoom(maxZoom);

        config.setShowCanvasBounds(true); // If the view is bigger than canvas, with this the user will see the bounds (Recommended)

        config.setCanvasHeight(getCanvasHeight());
        config.setCanvasWidth(getCanvasWidth());
        drawingView.setConfig(config);

    }



    /**
     * sets the stroke width
     * @param strokeWidthToUse - width to set (in the form of 10 px for example, so make sure to strip and convert the string to a float)
     */
    private void reconfigureStrokeWidth(String strokeWidthToUse){
        DrawableViewConfig config = new DrawableViewConfig();

        config.setStrokeColor(color);

        try{
            config.setStrokeWidth(Float.parseFloat(strokeWidthToUse.replace("px","").trim()));
        }catch (Exception e){
            config.setStrokeWidth(20.f);
        }

        config.setMinZoom(minZoom);
        config.setMaxZoom(maxZoom);

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
                .setCancelText("Cancel")
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.dismissWithAnimation();
                    }
                })
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.dismissWithAnimation();
                        drawingView.clear();
                        Drawable transparentDrawable = new ColorDrawable(Color.TRANSPARENT);
                        drawingView.setBackground(transparentDrawable);
                        image = null;
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra(Constants.ID_ACTIVITY_RESULT_GOOD_KEY,currentId);
                        if(image != null && image.length > 0){
                            returnIntent.putExtra(Constants.ID_IMAGE,image);
                        }
                        setResult(Activity.RESULT_OK,returnIntent);

                        deleteImageInDb();

                        finish();
                    }
                })
                .show();
    }

    /**
     * deletes the current notifications image given the associated ID
     */
    private void deleteImageInDb() {
        if(currentId != -1){
            RealmResults<Notification> results = realm.where(Notification.class).equalTo("ID",currentId).findAll();

            if(results.size() > 0){
                if(realm != null){
                    Notification toEdit = realm.where(Notification.class)
                            .equalTo("ID", currentId).findFirst();
                    realm.beginTransaction();
                    toEdit.setImage(new byte[0]);
                    realm.commitTransaction();
                }

            }else{
                if(realm != null){
                    realm.beginTransaction();

                    // Create an object
                    Notification notification = realm.createObject(Notification.class);
                    notification.setImage(new byte[0]);
                    realm.commitTransaction();
                }
            }
        }
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
                            if(image != null && image.length > 0){
                                returnIntent.putExtra(Constants.ID_IMAGE,image);
                            }
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

        image = byteArray;

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
