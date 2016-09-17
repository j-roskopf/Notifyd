package com.joe.notifyd.Util;

import android.content.Context;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * Created by Joe on 9/5/2016.
 */
public class Helper {

    Context context;

    public Helper(Context context){
        this.context = context;
    }

    public void displayErrorMessage(String message){
        new SweetAlertDialog(context, SweetAlertDialog.ERROR_TYPE)
                .setTitleText("Oops...")
                .setContentText(message)
                .show();
    }
}
