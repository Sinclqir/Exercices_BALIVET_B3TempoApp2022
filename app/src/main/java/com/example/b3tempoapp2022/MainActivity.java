package com.example.b3tempoapp2022;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.b3tempoapp2022.databinding.ActivityMainBinding;

import java.net.HttpURLConnection;
import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    public static final String CHANNEL_ID = "tempo_alert_channel_id";
    private static final int ALARM_MANAGER_REQUEST_CODE = 2023;

    public static IEdfApi edfApi;
    ActivityMainBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG,"onCreate()");
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // init views
        binding.historyBt.setOnClickListener(this);

        Button historyBt2 = findViewById(R.id.history_bt2);
        historyBt2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, HistoryActivity2.class);
                startActivity(intent);
            }
        });


        // Create notification channel
        createNotificationChannel();

        // init tempo alarm
        initAlarmManager();

        // Init Retrofit client
        Retrofit retrofitClient = ApiClient.get();
        if (retrofitClient != null) {
            // Create EDF API Call interface
            edfApi = retrofitClient.create(IEdfApi.class);
        } else {
            Log.e(LOG_TAG, "unable to initialize Retrofit client");
            finish();
        }

    }

    @Override
    protected void onResume() {
        Log.d(LOG_TAG,"onResume()");
        super.onResume();
        updateNbTempoDaysLeft();
        updateTempoDaysColor();
    }

    private void updateNbTempoDaysLeft() {
        // Create call to getTempoDaysLeft
        Call<TempoDaysLeft> call = edfApi.getTempoDaysLeft(IEdfApi.EDF_TEMPO_API_ALERT_TYPE);

        call.enqueue(new Callback<TempoDaysLeft>() {
            @Override
            public void onResponse(@NonNull Call<TempoDaysLeft> call, @NonNull Response<TempoDaysLeft> response) {
                TempoDaysLeft tempoDaysLeft = response.body();
                if (response.code() == HttpURLConnection.HTTP_OK && tempoDaysLeft != null) {
                    Log.d(LOG_TAG, "nb red days = " + tempoDaysLeft.getParamNbJRouge());
                    Log.d(LOG_TAG, "nb white days = " + tempoDaysLeft.getParamNbJBlanc());
                    Log.d(LOG_TAG, "nb blue days = " + tempoDaysLeft.getParamNbJBleu());
                    binding.blueDaysTv.setText(String.valueOf(tempoDaysLeft.getParamNbJBleu()));
                    binding.whiteDaysTv.setText(String.valueOf(tempoDaysLeft.getParamNbJBlanc()));
                    binding.redDaysTv.setText(String.valueOf(tempoDaysLeft.getParamNbJRouge()));
                } else {
                    Log.w(LOG_TAG, "call to getTempoDaysLeft () failed with error code " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<TempoDaysLeft> call, @NonNull Throwable t) {
                Log.e(LOG_TAG, "call to getTempoDaysLeft () failed ");
            }
        });

    }

    private void updateTempoDaysColor() {
        // Call to getTempoDaysColor
        Call<TempoDaysColor> call;
        call = edfApi.getTempoDaysColor(Tools.getNowDate("yyyy-MM-dd"),IEdfApi.EDF_TEMPO_API_ALERT_TYPE);

        call.enqueue(new Callback<TempoDaysColor>() {
            @Override
            public void onResponse(@NonNull Call<TempoDaysColor> call, @NonNull Response<TempoDaysColor> response) {
                TempoDaysColor tempoDaysColor = response.body();
                if (response.code() == HttpURLConnection.HTTP_OK && tempoDaysColor != null) {
                    Log.d(LOG_TAG,"Today color = " + tempoDaysColor.getCouleurJourJ().toString());
                    Log.d(LOG_TAG,"Tomorrow color = " + tempoDaysColor.getCouleurJourJ1().toString());
                    binding.todayDcv.setDayCircleColor(tempoDaysColor.getCouleurJourJ());
                    binding.todayDcv.addColorString(getString(tempoDaysColor.getCouleurJourJ().getStringResId()));
                    binding.tomorrowDcv.setDayCircleColor(tempoDaysColor.getCouleurJourJ1());
                    binding.tomorrowDcv.addColorString(getString(tempoDaysColor.getCouleurJourJ1().getStringResId()));
                    //checkColor4Notif(tempoDaysColor.getCouleurJourJ1());
                } else {
                    Log.w(LOG_TAG, "call to getTempoDaysColor() failed with error code " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<TempoDaysColor> call, @NonNull Throwable t) {
                Log.e(LOG_TAG, "call to getTempoDaysColor() failed ");
            }
        });
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void checkColor4Notif(TempoColor color) {
        if (color == TempoColor.RED || color == TempoColor.WHITE) {
            // create notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher) // mandatory setting !
                    .setContentTitle(getString(R.string.tempo_notif_title))
                    .setContentText(getString(R.string.tempo_notif_text, getString(color.getStringResId())))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            // show notification
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(Tools.getNextNotifId(), builder.build());
        }

    }

    private void initAlarmManager() {

        // create a pending intent
        Intent intent = new Intent(this, TempoAlarmReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(
                this,
                ALARM_MANAGER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        // Set the alarm to start at approximately 11:15 a.m.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 11);
        calendar.set(Calendar.MINUTE, 15);

        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        // With setInexactRepeating(), you have to use one of the AlarmManager interval
        // constants--in this case, AlarmManager.INTERVAL_DAY.
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, alarmIntent);
    }

  /* Obsolete way to handle button click based on the button's 'onClick' XML attribute
    public void showHistory(View view) {
        Intent intent = new Intent();
        intent.setClass(this,HistoryActivity.class);
        startActivity(intent);
    } */

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        intent.setClass(this,HistoryActivity.class);
        startActivity(intent);
    }
}