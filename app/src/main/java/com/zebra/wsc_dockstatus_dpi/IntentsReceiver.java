package com.zebra.wsc_dockstatus_dpi;

import static android.content.Intent.ACTION_DOCK_EVENT;
import static android.content.Intent.EXTRA_DOCK_STATE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.symbol.emdk.EMDKBase;
import com.symbol.emdk.EMDKException;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileManager;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;

//adb shell am broadcast -a com.ndzl.DW -c android.intent.category.DEFAULT --es com.symbol.datawedge.data_string spriz  com.ndzl.targetelevator/.IntentsReceiver
public class IntentsReceiver extends BroadcastReceiver implements EMDKManager.EMDKListener, EMDKManager.StatusListener, ProfileManager.DataListener {
    private ProfileManager profileManager = null;
    private EMDKManager emdkManager = null;

    String profile_LOW_DPI = "LOW_DPI_ON_PRIMARY_DISPLAY";
    String profile_HIGH_DPI = "HIGH_DPI_ON_PRIMARY_DISPLAY";

    int dockState=-1;
    String dockLastStatus="N/A";
    static int dockedCount=0;
    static int undockedCount=0;

    @Override
    public void onReceive(final Context context, Intent intent) {

/*        if (intent != null && intent.getAction().equals("com.ndzl.DW")){
            String barcode_value = intent.getStringExtra("com.symbol.datawedge.data_string");
            String _tbw = "\n"+ DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis()))+" - com.ndzl.DW received via BROADCAST intent <"+barcode_value+">";
            try {

                //MainActivity.getInstanceActivity().testCall(_tbw);
                //MainActivity.getInstanceActivity().setProfile_LOW_DPI();
               //MainActivity.getInstanceActivity().setProfile_HIGH_DPI();

                //setProfile_HIGH_DPI();

            } catch (Exception e) {

            }

        }
        else if (intent != null && intent.getAction().equals(ACTION_DOCK_EVENT)){
            */


        //sticky broadcast received multiple times  https://developer.android.com/topic/security/risks/sticky-broadcast
            dockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE,Intent.EXTRA_DOCK_STATE_UNDOCKED);
            if (dockState == 1) {
                // we are now in 'dock'
                // place your code here
                dockedCount++;
                dockLastStatus = "DOCKED "+dockedCount;
            }else if(dockState == 0){
                // we are now in 'undock'
                // place your code here
                undockedCount++;
                dockLastStatus = "UNDOCKED "+undockedCount;
            }
       // }




            //TRYING TO GET RID OF THE STICKY INTENTS RE-SUBMISSION
        //==> NO, LOOPS FOREVER!
        //MainActivity.getInstanceActivity().receiverUnregistration();
        //MainActivity.getInstanceActivity().receiverRegistration();


        EMDKResults results = EMDKManager.getEMDKManager(context.getApplicationContext(), this);

        Log.i("Zebra WSC Docking Status", dockLastStatus);
        Toast.makeText(context, dockLastStatus, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {
        this.emdkManager = emdkManager;

        try {
            emdkManager.getInstanceAsync(EMDKManager.FEATURE_TYPE.PROFILE, this);
        } catch (EMDKException e) {
            e.printStackTrace();
        }
        catch(Exception ex){

        }
    }

    @Override
    public void onClosed() {

    }

    boolean isEMDKStatusOK = false;
    @Override
    public void onStatus(EMDKManager.StatusData statusData, EMDKBase emdkBase) {
        if(statusData.getResult() == EMDKResults.STATUS_CODE.SUCCESS) {
            if(statusData.getFeatureType() == EMDKManager.FEATURE_TYPE.PROFILE)
            {
                profileManager = (ProfileManager)emdkBase;
                profileManager.addDataListener(this);

                isEMDKStatusOK = true;

                if(dockState==1)
                    setProfile_LOW_DPI();
                else if(dockState==0)
                    setProfile_HIGH_DPI();

            }
        }
    }

    void setProfile_LOW_DPI(){
        if(isEMDKStatusOK)
            ApplyEMDKprofile(profile_LOW_DPI);
    }

    void setProfile_HIGH_DPI(){
        if(isEMDKStatusOK)
            ApplyEMDKprofile(profile_HIGH_DPI);
    }

    private void ApplyEMDKprofile( String profileToBeApplied){
        if (profileManager != null) {
            String[] modifyData = new String[1];

            final EMDKResults results = profileManager.processProfileAsync(profileToBeApplied,
                    ProfileManager.PROFILE_FLAG.SET, modifyData);

            String sty = results.statusCode.toString();
        }
    }

    @Override
    public void onData(ProfileManager.ResultData resultData) {
        EMDKResults result = resultData.getResult();
        if(result.statusCode == EMDKResults.STATUS_CODE.CHECK_XML) {
            String responseXML = result.getExtendedStatusMessage();
            Log.i("Zebra WSC Docking Status", "SCREEN DPI CHANGED");
        } else if(result.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            Log.i("Zebra WSC Docking Status", "ERROR IN PROFILE APPLICATION");
        }
        //finish();
        emdkManager.release(); // IS THIS THE RIGHT PLACE TO CALL IT?
    }

}