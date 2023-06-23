package com.example.llamadasos;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

public class SegundoPlano extends Service {

    private static final String TAG = "SegundoPlano";
    private LocationManager locationManager;
    private LocationListener locationListener;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;

    private double latitude = 0.0;
    private double longitude = 0.0;
    private String telefono;

    @Override
    public void onCreate() {
        super.onCreate();

        // Inicializar variables y obtener preferencias
        initVariablesAndPreferences();

        // Configuración para obtener las coordenadas del teléfono
        configureLocationUpdates();

        // Configuración para manejar llamadas telefónicas
        configurePhoneStateListener();
    }

    private void initVariablesAndPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(SegundoPlano.this);
        telefono = preferences.getString("telefono", "");
        Toast.makeText(this, "Servicio iniciado", Toast.LENGTH_SHORT).show();
    }

    private void configureLocationUpdates() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListener);
        }
    }

    private void configurePhoneStateListener() {
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            boolean primeraLlamada = true;

            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                        handleIncomingCall();
                        primeraLlamada = false;
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (!primeraLlamada) {
                            handleCallEnded();
                            primeraLlamada = true;
                        }
                        break;
                }
            }
        };
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void handleIncomingCall() {
        Toast.makeText(SegundoPlano.this, "Llamada entrante", Toast.LENGTH_SHORT).show();
        sendMessageWithCoordinates(latitude, longitude);
        setVolumeToHalf();
    }

    private void handleCallEnded() {
        Toast.makeText(SegundoPlano.this, "Llamada finalizada", Toast.LENGTH_SHORT).show();
        restoreVolumeToMax();
        makePhoneCall();
    }

    private void sendMessageWithCoordinates(double latitude, double longitude) {
        SmsManager smsManager = SmsManager.getDefault();
        String phoneNumber = telefono;
        String message = "Coordenadas: " + latitude + ", " + longitude + "\n" +
                "https://maps.google.com/?q=" + latitude + "," + longitude;
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
    }

    private void setVolumeToHalf() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
        int targetVolume = maxVolume / 2;
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, targetVolume, 0);
    }

    private void restoreVolumeToMax() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_SAME, 0);
    }

    private void makePhoneCall() {
        String phoneNumber = telefono;
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        locationManager.removeUpdates(locationListener);
    }
}
