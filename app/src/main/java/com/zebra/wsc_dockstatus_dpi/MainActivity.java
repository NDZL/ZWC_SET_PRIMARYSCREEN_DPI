package com.zebra.wsc_dockstatus_dpi;

import static android.content.Intent.ACTION_DOCK_EVENT;
import static android.content.Intent.EXTRA_DOCK_STATE;
import static android.view.ViewConfiguration.getLongPressTimeout;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.symbol.emdk.EMDKBase;
import com.symbol.emdk.EMDKException;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileManager;

import java.lang.ref.WeakReference;


/*
*   MONITORING THE WORKSTATION CONNECT DOCKING STATE AND APPLYING THE DIFFERENT DPI RESOLUTIONS DEPENDING ON THE DOCK STATUS
*  VERSION #1: THE APP NEEDS TO STAY OPEN, THOUGH MINIMIZED, IN ORDER TO RECEIVE THE DOCKING STATE CHANGES
*  (ATTEMPTS TO USE A RECEIVER DECLARED IN THE MANIFEST FILE FAILED BECAUSE OF THE BACKGROUND EXECUTION RESTRICTIONS)
*   FUTURE VERSIONS MIGHT STARTUP AT BOOT, INSTALL A FOREGROUND SERVICE AND KEEP A BROADCAST RECEIVER ALIVE - REFER TO TARGETELEVATOR SOLUTION
* */

public class MainActivity extends AppCompatActivity  implements EMDKManager.EMDKListener, EMDKManager.StatusListener, ProfileManager.DataListener{

    private ProfileManager profileManager = null;
    private EMDKManager emdkManager = null;

    String profile_LOW_DPI = "LOW_DPI_ON_PRIMARY_DISPLAY";
    String profile_HIGH_DPI = "HIGH_DPI_ON_PRIMARY_DISPLAY";
    public static WeakReference<MainActivity> weakActivity;
    // etc..
    public static MainActivity getInstanceActivity() {
        return weakActivity.get();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("a13emdk", "getLongPressTimeout()="+getLongPressTimeout());

        weakActivity = new WeakReference<>(MainActivity.this);


        receiverRegistration();

        //TESTING WINDOW MANAGER
        //com.android.settingslib.display.DisplayDensityUtils densityUtils = new com.android.settingslib.display.DisplayDensityUtils(this);


        //EMDK REGISTRATION
        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), MainActivity.this);
    }

    BroadcastReceiver _br = null;
    public void receiverRegistration(){
        IntentFilter filter = new IntentFilter(EXTRA_DOCK_STATE);

        //filter.addAction(EXTRA_DOCK_STATE); //STICKY BROADCAST  https://developer.android.com/topic/security/risks/sticky-broadcast
        filter.addAction(ACTION_DOCK_EVENT);

        _br = new IntentsReceiver();

        registerReceiver(_br, filter);
    }

    public void receiverUnregistration(){
        unregisterReceiver(_br);
    }

    public void showToast(String s){
        Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {
        this.emdkManager = emdkManager;

        try {
            emdkManager.getInstanceAsync(EMDKManager.FEATURE_TYPE.PROFILE, MainActivity.this);
        } catch (EMDKException e) {
            e.printStackTrace();
        }
        catch(Exception ex){

        }
    }


    void testCall(String s){
        int x=0;
        String loc=s;
        s+="";
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        //Clean up the objects created by EMDK manager
        emdkManager.release();
    }




    @Override
    public void onClosed() {

    }

    boolean isEMDKStatusOK = false;

    //ON a TC21 A13: VERY SLOW TO CALL onStatus!
    @Override
    public void onStatus(EMDKManager.StatusData statusData, EMDKBase emdkBase) {
        if(statusData.getResult() == EMDKResults.STATUS_CODE.SUCCESS) {
            if(statusData.getFeatureType() == EMDKManager.FEATURE_TYPE.PROFILE)
            {
                profileManager = (ProfileManager)emdkBase;
                profileManager.addDataListener(this);

                isEMDKStatusOK = true;

                ApplyEMDKprofile(  "SOMESETTING" );
                //finish();
                //System.exit(0);
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
            Toast.makeText(MainActivity.this, "RESPONSE="+responseXML, Toast.LENGTH_LONG).show();
        } else if(result.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            Toast.makeText(MainActivity.this, "ERROR IN PROFILE APPLICATION", Toast.LENGTH_LONG).show();
        }
        //finish();
    }


    //A13 WITH MXMF.APK INSTALLED: <?xml version="1.0" encoding="UTF-8"?><wap-provisioningdoc><characteristic type="status"><parm name="code" value="6"/><parm name="description" value="Review the XML for details"/><characteristic type="extended_status"><parm name="code" value="19"/><parm name="description" value="Application is not allowed to submit xml."/></characteristic></characteristic><characteristic-error type="Permission Error" desc="MxFrameworkService:[com.ndzl.a13emdk] is NOT allowed to access MX Framework!"/></wap-provisioningdoc>
}