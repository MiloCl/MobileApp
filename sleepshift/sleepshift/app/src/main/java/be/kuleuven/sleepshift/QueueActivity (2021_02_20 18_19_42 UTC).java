


package be.kuleuven.sleepshift;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

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
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Calendar;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import static java.lang.Math.abs;

public class QueueActivity extends AppCompatActivity {

    private TextView  txtStep, lblQty;
    private LocalTime time1, time3;
    private int steps, dagen, slaaplengte;
    private ArrayList<PendingIntent> intentArray;
    private AlarmManager am;
    private int pendingIntentID=100;
    private LocalTime t1;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);
        t1= LocalTime.parse("12:59:59");

        txtStep= findViewById(R.id.txtSteps);
        lblQty = (TextView) findViewById(R.id.lblQty);

        //get the times from main activity
        Bundle extras = getIntent().getExtras();
        assert extras != null;
        time1 =(LocalTime) extras.get("tijd1");
        LocalTime time2 = (LocalTime) extras.get("tijd2");
        time3 = (LocalTime) extras.get("tijd3");

        getSlaapLengte(time1, time2);


        findViewById(R.id.btnAlarm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createNotification();
                setAlarms();
            }
        });

        findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelAlarms();
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void getSlaapLengte(LocalTime time1, LocalTime time2) {
            long verschil= time1.until(time2, ChronoUnit.MINUTES);
            slaaplengte= (int) (1440+ verschil);
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotification(){
        CharSequence name = "melding";
        String description = "Channel for melding";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel("melding", name, importance);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setAlarms() {
        Toast.makeText(this, "Alarms are set", Toast.LENGTH_SHORT).show();
        Calendar calendar = Calendar.getInstance();
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH),time1.getHour(), time1.getMinute(), 0);

        boolean eerste= true;
        for( int f=0;f<dagen;f++) {
            if(time3.isBefore(t1) && time1.isAfter(t1)){
                if(eerste){calendar.add(Calendar.DATE, -1);eerste= false;}
                //time 1 is na 12:00, time 3 is voor 12:00
                situation1(calendar);
            }

            if(time1.isBefore(t1) && time3.isAfter(t1) ){
                //time 1 is voor 12:00, time 3 is na 12:00
                situation2(calendar);
            }
            if((time1.isAfter(t1) && time3.isAfter(t1)) || (time1.isBefore(t1) && time3.isBefore(t1))){
                //time 1 en time 3 zijn beiden voor na 00:00
                situation3(calendar, t1);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void situation3(Calendar calendar, LocalTime t1) {
        if (time1.isBefore(t1) && time3.isBefore(t1)){calendar.add(Calendar.DATE, 1);}
        calendar.add(Calendar.MINUTE, steps);
        setWekker(calendar);
        if (time1.isBefore(t1) && time3.isBefore(t1)){calendar.add(Calendar.DATE, -1);}
        calendar.add(Calendar.MINUTE, slaaplengte);
        setWekker(calendar);
        calendar.add(Calendar.MINUTE, -1*slaaplengte);
        calendar.add(Calendar.DATE, 1);
        ;
    }

    private void situation2(Calendar calendar) {
        calendar.add(Calendar.DATE, 1);
        calendar.add(Calendar.MINUTE, steps);
        setWekker(calendar);
        calendar.add(Calendar.MINUTE, slaaplengte);
        calendar.add(Calendar.DATE, -1);
        setWekker(calendar);
        calendar.add(Calendar.DATE, 1);
        calendar.add(Calendar.MINUTE, -1*slaaplengte);
        ;
    }

    private void situation1(Calendar calendar) {
        calendar.add(Calendar.DATE, 1);
        calendar.add(Calendar.MINUTE, steps);
        setWekker(calendar);
        calendar.add(Calendar.MINUTE, slaaplengte);
        setWekker(calendar);
        calendar.add(Calendar.MINUTE, -1*slaaplengte);
    }


    //Vanaf hier wordt het alarm naar de MyAlarm klasse gestuurd
    public void setWekker(Calendar cal){
        am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        intentArray = new ArrayList<>();
        Intent intent = new Intent(this, MyAlarm.class);
        PendingIntent pi = PendingIntent.getBroadcast(QueueActivity.this, pendingIntentID, intent, 0);
        am.set(AlarmManager.RTC, cal.getTimeInMillis() , pi);
        Log.d("AlarmIsSet", "Alarm is set on" +  cal.getTime());
        intentArray.add(pi);
        pendingIntentID++;
    }

    private void cancelAlarms(){
        Toast.makeText(this, "Alarms are cancelled", Toast.LENGTH_SHORT).show();

         am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, MyAlarm.class);


        if(intentArray.size()>0){
            for(int i=0; i<intentArray.size(); i++){
                PendingIntent pendingIntent = PendingIntent.getBroadcast(QueueActivity.this, 100+i, intent, 0);

                am.cancel(pendingIntent);
            }
            intentArray.clear();
        }
    }



    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onBtnPlus_CLicked(View caller){
        int quantity = Integer.parseInt(lblQty.getText().toString()) ;
        if(quantity <7 ){quantity=quantity+1;
            lblQty.setText( Integer.toString(quantity));
            calculateSteps();
            makeShedule(0, quantity);}
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onBtnMinus_CLicked(View caller){
        int quantity = Integer.parseInt(lblQty.getText().toString()) ;
        if(quantity > 1){quantity=quantity-1;
            lblQty.setText( Integer.toString(quantity));
            calculateSteps();
            makeShedule(1, quantity);}
    }

    //berekent hoe groot de stappen moeten zijn om het slaaprimte per dag aan te passen
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void calculateSteps() {
        int verschil = abs(time1.getMinute() + 60 * (time1.getHour()) - time3.getMinute() - 60 * (time3.getHour()));
        if (verschil > 1080) {
            verschil = 1440 - abs(verschil);
        }
        dagen = Integer.parseInt(lblQty.getText().toString());
        steps = verschil / dagen;
        txtStep.setText("" + steps + " minutes/day");
    }



    @RequiresApi(api = Build.VERSION_CODES.O)
    public void makeShedule(int x, int dag){

        //Hier wordt een arraylist gemaakt met de 7 textviews zodat ze in de loop in 1 keer gevuld kunnen worden met tekst
        TextView txt1= findViewById(R.id.txt1);TextView txt2= findViewById(R.id.txt2);TextView txt3= findViewById(R.id.txt3);
        TextView txt4= findViewById(R.id.txt4);TextView txt5= findViewById(R.id.txt5);TextView txt6= findViewById(R.id.txt6);
        TextView txt7= findViewById(R.id.txt7);
        ArrayList<TextView> vakjes= new ArrayList<TextView>();
        vakjes.add(txt1);vakjes.add(txt2);vakjes.add(txt3);vakjes.add(txt4);
        vakjes.add(txt5);vakjes.add(txt6);vakjes.add(txt7);

        checkForNegativeStep();

        //Dit is het deel van de functie die effectief de waarden in de textviews steekt
        LocalDate date= LocalDate.now();
        LocalTime tijd = null;
        LocalDate datum = null;
        boolean eerste= true;
        for(int i=0; i < dagen; i++){
            tijd= time1.plusMinutes(steps*(i+1));
            datum= date.plusDays(i);
            LocalTime ochtend= tijd.plusMinutes(slaaplengte);

            vakjes.get(i).setText("" + datum.getDayOfWeek() + "\n"+ "\n" + tijd+ "-" + ochtend);

        }
        if(x==1){ vakjes.get(dag).setText("");}
    }

    //checks if negative step is necessary
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void checkForNegativeStep() {
        if( time1.isBefore(t1) && time3.isAfter(t1)) {steps= steps*-1;}
        if(time1.isAfter(time3) && time1.isAfter(t1) && time3.isAfter(t1) ){steps=steps*-1;}
        if(time1.isAfter(time3) && time1.isBefore(t1) && time3.isBefore(t1) ){steps=steps*-1;}
    }

}