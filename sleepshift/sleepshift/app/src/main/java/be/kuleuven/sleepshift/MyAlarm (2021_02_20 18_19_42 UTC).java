package be.kuleuven.sleepshift;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


public class MyAlarm extends BroadcastReceiver {

    //the method will be fired when the alarm is triggerred
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("MyAlarmBel", "Alarm just fired");
        MediaPlayer mediaPlayer= MediaPlayer.create(context, Settings.System.DEFAULT_RINGTONE_URI);
        mediaPlayer.start();

        NotificationCompat.Builder builder= new NotificationCompat.Builder(context, "melding")
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_light)
                .setContentTitle("Alarm Sleepshift")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);



        NotificationManagerCompat notificationManager= NotificationManagerCompat.from(context);

        //elk id van van elke melding moet anders zijn
        notificationManager.notify(200, builder.build());
    }

}
