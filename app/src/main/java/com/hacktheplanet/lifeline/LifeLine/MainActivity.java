package com.hacktheplanet.lifeline.LifeLine;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.telephony.SmsManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.SampleRate;

import java.util.Collection;
import java.util.UUID;

import com.microsoft.band.tiles.BandIcon;
import com.microsoft.band.tiles.BandTile;



public class MainActivity extends Activity {

    private BandClient client = null;
    private Button btnStart;
    private TextView txtStatus;
    private TextView accelTest;
    private AlertDialog instanceDialog = null;
    private EditText contact1Name, contact2Name, contact3Name,
            contact1Phone, contact2Phone, contact3Phone;
    private String gmail = null;
    private  BandIcon smallIcon;
    private BandIcon tileIcon;
    private UUID tileUuid;
    private BandTile tile;
    private CountDownTimer instanceTimer = null;
    private final static String TAG = "MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.hacktheplanet.lifeline.LifeLine.R.layout.activity_main);

        txtStatus = (TextView) findViewById(com.hacktheplanet.lifeline.LifeLine.R.id.testText);
        btnStart = (Button) findViewById(com.hacktheplanet.lifeline.LifeLine.R.id.btnStart);
        contact1Name = (EditText) findViewById(com.hacktheplanet.lifeline.LifeLine.R.id.contact1Name);
        contact2Name = (EditText) findViewById(com.hacktheplanet.lifeline.LifeLine.R.id.contact2Name);
        contact3Name = (EditText) findViewById(com.hacktheplanet.lifeline.LifeLine.R.id.contact3Name);
        contact1Phone = (EditText) findViewById(com.hacktheplanet.lifeline.LifeLine.R.id.contact1Phone);
        contact2Phone = (EditText) findViewById(com.hacktheplanet.lifeline.LifeLine.R.id.contact2Phone);
        contact3Phone = (EditText) findViewById(com.hacktheplanet.lifeline.LifeLine.R.id.contact3Phone);
        AccountManager manager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
        Account[] list = manager.getAccounts();

        // create the small and tile icons from writable bitmaps
        // small icons are 24x24 pixels
        Bitmap smallIconBitmap = Bitmap.createBitmap(24, 24, null);
        smallIcon = BandIcon.toBandIcon(smallIconBitmap);
        // tile icons are 46x46 pixels
        Bitmap tileIconBitmap = Bitmap.createBitmap(46, 46, null);
        tileIcon = BandIcon.toBandIcon(tileIconBitmap);
        // create a new UUID for the tile
        tileUuid = UUID.randomUUID();
        // create a new BandTile using the builder
        tile = new BandTile.Builder(tileUuid,"LifeLine", tileIcon).setTileSmallIcon(smallIcon).build();

        for(Account account: list)
        {
            if(account.type.equalsIgnoreCase("com.google"))
            {
                this.gmail = account.name;
                break;
            }
        }

        btnStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                txtStatus.setText("");
                new appTask().execute();
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        txtStatus.setText("");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (client != null) {
            try {
                client.getSensorManager().unregisterAccelerometerEventListeners();
            } catch (BandIOException e) {
                appendToUI(e.getMessage());
            }
        }
    }

    private class appTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    appendToUI("Band is connected.\n");
                    startAccelListener();
                    Collection<BandTile> tiles = client.getTileManager().getTiles().await();
                    int tileCapacity = client.getTileManager().getRemainingTileCapacity().await();
                    if(client.getTileManager().addTile(MainActivity.this, tile).await()) {
                        // do work if the tile was successfully created
                    }
                    if(client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
                        startHRListener();
                    } else {
                        // user has not consented yet, request it
                        client.getSensorManager().requestHeartRateConsent(MainActivity.this, mHeartRateConsentListener);
                    }
                    for(BandTile t : tiles) {
                        if(client.getTileManager().removeTile(t).await()){
                            // do work if the tile was successfully removed
                        }
                    }

                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage();
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }

    private void appendToUI(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus.setText(string);


            }
        });
    }

    private BandHeartRateEventListener heartRateListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null) {
                appendToUI(String.format(" HR = " + event.getHeartRate()));
            }
        }

    };

    private BandAccelerometerEventListener accelListener = new BandAccelerometerEventListener() {
        @Override
        public void onBandAccelerometerChanged(BandAccelerometerEvent event) {

            if (event.getAccelerationX() > 3 || event.getAccelerationX() < -3){
                txtStatus.post(new Runnable() {
                    @Override
                    public void run() {
                        if (instanceDialog == null) {
                            showDialog();
                        }
                    }
                });
                try {
                    client.getNotificationManager().showDialog(tileUuid,"Everything ok?",
                            "Press to dismiss").await();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                catch (BandException e){
                    e.printStackTrace();
                }

            }
        }

    };


    private void showDialog(){
        final AlertDialog alertDialog = new AlertDialog.Builder(this).setPositiveButton("I'm OK!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                instanceTimer.cancel();
                instanceDialog = null;
            }
        }).create();
        alertDialog.setTitle("Abnormal movement detected, Are You OK?");
        alertDialog.setMessage("00:10");
        instanceDialog = alertDialog;
        alertDialog.show();


        if (alertDialog.isShowing()) {
            instanceTimer = new CountDownTimer(10000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    alertDialog.setMessage("00:"+ (millisUntilFinished/1000));
                }

                @Override
                public void onFinish() {
                    alertDialog.dismiss();
                    txtStatus.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Warning not dismissed, notifying emergency Contacts", Toast.LENGTH_LONG).show();
                            if (contact1Phone.getText() != null) {
                               sendSMS("+1"+contact1Phone.getText().toString(), "Hello " + contact1Name.getText().toString()+ ", this is an automated message from Seize Control." +
                                       " The owner of this phone did not dismiss the warning and may be in trouble, please contact them.");
                            }
                            if (contact2Phone.getText() != null) {
                                sendSMS("+1"+contact2Phone.getText().toString(), "Hello " + contact2Name.getText().toString() + ", this is an automated message from Seize Control." +
                                        " The owner of this phone did not dismiss the warning and may be in trouble, please contact them.");
                            }
                            if (contact3Phone.getText() != null) {
                                sendSMS("+1"+contact3Phone.getText().toString(), "Hello " + contact3Name.getText().toString() + ", this is an automated message from Seize Control." +
                                        " The owner of this phone did not dismiss the warning and may be in trouble, please contact them.");
                            }
                        }
                    });
                }
            }.start();
        }

    }

    public class TileEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == "com.microsoft.band.action.ACTION_TILE_OPENED") {
                // handle tile opened event

            }
            else if (intent.getAction() == "com.microsoft.band.action.ACTION_TILE_BUTTON_PRESSED") {
                // handle button pressed event
                showDialog();
            }
            else if (intent.getAction() == "com.microsoft.band.action.ACTION_TILE_CLOSED") {
                // handle tile closed event
            }
        }
    }

    public void sendSMS(String phoneNo, String msg){
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
            Toast.makeText(getApplicationContext(), "Message Sent",
                    Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(),ex.getMessage().toString(),
                    Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        }
    }

    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                appendToUI("Band isn't paired with your phone.\n");
                return false;
            }
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        appendToUI("Band is connecting...\n");
        return ConnectionState.CONNECTED == client.connect().await();
    }

    private HeartRateConsentListener mHeartRateConsentListener = new HeartRateConsentListener() {
        @Override
        public void userAccepted(boolean b) {
            // handle user's heart rate consent decision
            if (b) {
                // Consent has been given, start HR sensor event listener
                startHRListener();
            } else {
                // Consent hasn't been given
                appendToUI(String.valueOf(b));
            }
        }
    };


    public void startHRListener() {
        try {
            // register HR sensor event listener
            client.getSensorManager().registerHeartRateEventListener(heartRateListener);
        } catch (BandIOException ex) {
            appendToUI(ex.getMessage());
        } catch (BandException e) {
            String exceptionMessage="";
            switch (e.getErrorType()) {
                case UNSUPPORTED_SDK_VERSION_ERROR:
                    exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.";
                    break;
                case SERVICE_ERROR:
                    exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.";
                    break;
                default:
                    exceptionMessage = "Unknown error occurred: " + e.getMessage();
                    break;
            }
            appendToUI(exceptionMessage);

        } catch (Exception e) {
            appendToUI(e.getMessage());
        }
    }

    public void startAccelListener(){
        try{
            client.getSensorManager().registerAccelerometerEventListener(accelListener, SampleRate.MS128);
        }
        catch (BandIOException ex){
            appendToUI(ex.getMessage());
        }
        catch(BandException ex){
            String exceptionMessage="";
            switch (ex.getErrorType()) {
                case UNSUPPORTED_SDK_VERSION_ERROR:
                    exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.";
                    break;
                case SERVICE_ERROR:
                    exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.";
                    break;
                default:
                    exceptionMessage = "Unknown error occurred: " + ex.getMessage();
                    break;
            }
            appendToUI(exceptionMessage);

        } catch (Exception ex) {
            appendToUI(ex.getMessage());
        }
    }
}