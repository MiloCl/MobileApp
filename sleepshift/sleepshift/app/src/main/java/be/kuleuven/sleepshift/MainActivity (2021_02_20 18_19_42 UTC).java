package be.kuleuven.sleepshift;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import static java.lang.Math.abs;


public class MainActivity extends AppCompatActivity implements TimePickerDialog.OnTimeSetListener {

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    private int time1h, time1m, time2h, time2m, time3h, time3m;
    private CheckBox permission;
    private double lat, longi;
    private Button knop, btncalculate;
    private Spinner spinner;
    private Spinner spinner2;
    private String country;
    private boolean checked=false;
    private TextView tijdverschil;
    private boolean timeSet=false;
    private boolean jetLagMode=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        btncalculate= findViewById(R.id.btnCalculate);
        spinner = findViewById(R.id.spDest);
        spinner2 = findViewById(R.id.spCurrentCountry);
        spinner.setEnabled(false);
        spinner2.setEnabled(false);
        permission= findViewById(R.id.cbPermission);
        tijdverschil = findViewById(R.id.tijdVerschil);
        tijdverschil.setVisibility(TextView.INVISIBLE);
        btncalculate.setClickable(false);
        permission.setEnabled(false);

        //Enable jetlag modus
        Switch sw = findViewById(R.id.switch1);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) { //als schuiver aan
                    disableNewSched();
                    spinner.setEnabled(true);
                    spinner2.setEnabled(true);
                    permission.setEnabled(true);
                    checked= true;
                    jetLagMode=true;
                    tijdverschil.setVisibility(TextView.VISIBLE);
                    //Bepaalt gedrag SLEEPSHIFT knop --> indien tijdverschil groter dan 0 voldaan
                    if(calculateTimeDif()==0){
                        btncalculate.setClickable(false);
                        btncalculate.setBackground(getResources().getDrawable(R.drawable.seek));}
                } else { //als schuiver af
                    enableNewSched();
                    jetLagMode=false;
                    permission.setChecked(false);
                    spinner.setEnabled(false);
                    spinner2.setEnabled(false);
                    checked= false;
                    permission.setEnabled(false);
                    tijdverschil.setVisibility(TextView.INVISIBLE);
                    //Bepaalt gedrag SLEEPSHIFT knop --> indien tijd oorspronkelijk ingesteld voldaan
                    if(timeSet){
                        btncalculate.setClickable(true);
                        btncalculate.setBackground(getResources().getDrawable(R.drawable.seek2));}
                    else{
                    btncalculate.setClickable(false);
                    btncalculate.setBackground(getResources().getDrawable(R.drawable.seek));}
                }}});

        //wanneer locatie aangevinkt of afgevinkt wordt
        permission.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    getLocationFromGPS();
                    spinner2.setEnabled(false);
                    updateTimediff();
                    if(calculateTimeDif()==0 && jetLagMode){
                        btncalculate.setClickable(false);
                        btncalculate.setBackground(getResources().getDrawable(R.drawable.seek));}
                } else {
                    spinner2.setEnabled(true);
                   updateTimediff();
                }}});


        //Timepicker om slaapritmes in te stellen
        final Button button = findViewById(R.id.btnCurrentSchedule);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment timePicker = new TimePickerFragment();
                timePicker.show(getSupportFragmentManager(), "time picker");
                timeSet=true;
                knop=button;
            }});

        final Button button2 = findViewById(R.id.btnCurrentSchedule2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment timePicker2 = new TimePickerFragment();
                timePicker2.show(getSupportFragmentManager(), "time picker");
                knop = button2;

                //Bepaalt gedrag SLEEPSHIFT knop --> indien uur ingesteld kan men naar queue
                if(!jetLagMode){
                    btncalculate.setClickable(true);
                    btncalculate.setBackground(getResources().getDrawable(R.drawable.seek2));}
            }
        });



        final Button button3 = findViewById(R.id.btnNewSchedule);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!jetLagMode){
                DialogFragment timePicker3 = new TimePickerFragment();
                timePicker3.show(getSupportFragmentManager(), "time picker");
                knop = button3;
            }}
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
            updateTimediff();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
               updateTimediff();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });

    }

    public void updateTimediff(){
        if(calculateTimeDif()>0){
        tijdverschil.setText("Time difference: +" + calculateTimeDif());}
        else
        {tijdverschil.setText("Time difference: " + calculateTimeDif());}
    }

    //Bepaalt welke tekst moet aangepast worden door timepicker
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

        if (knop == findViewById(R.id.btnCurrentSchedule2)) {
            setTime2(hourOfDay, minute);
        } else {
            if (knop == findViewById(R.id.btnCurrentSchedule)) {
                setTime1(hourOfDay, minute);
            } else {
                if (knop == findViewById(R.id.btnNewSchedule)) {
                    setTime3(hourOfDay, minute);
                }
            }
        }
    }



    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setTime1(int hourOfDay, int minute) {
        TextView uur = findViewById(R.id.txtCurrentScheduleHour);
        TextView minuut = findViewById(R.id.txtCurrentScheduleMinute);
        time1h = hourOfDay;
        time1m = minute;
        uur.setText("" + hourOfDay);
        minuut.setText(" " + minute);
        TextView punt = findViewById(R.id.textView11);
        punt.setText(":");
        if(!(time1m ==0 && time1h ==0) ){
            setTime4();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setTime2(int hourOfDay, int minute) {
        TextView uur = findViewById(R.id.txtCurrentScheduleHour2);
        TextView minuut = findViewById(R.id.txtCurrentScheduleMinute2);
        time2h = hourOfDay;
        time2m = minute;
        uur.setText("" + hourOfDay);
        minuut.setText(" " + minute);
        TextView punt = findViewById(R.id.textView12);
        punt.setText(":");

        if(!(time1m ==0 && time1h ==0) ){
            setTime4();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setTime3(int hourOfDay, int minute) {
        TextView uur = findViewById(R.id.txtNewScheduleHour);
        TextView minuut = findViewById(R.id.txtNewScheduleMinute);
        time3h = hourOfDay;
        time3m = minute;
        uur.setText("" + hourOfDay);
        minuut.setText(" " + minute);
        TextView punt = findViewById(R.id.textView13);
        punt.setText(":");

        //automatisch bepalen 4e tijd --> totale slaaptijd blijft gelijk
        setTime4();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setTime4() {
        TextView uur2 = findViewById(R.id.txtNewScheduleHour2);
        TextView minuut2 = findViewById(R.id.txtNewScheduleMinute2);
        LocalTime tijd4= getSlaapLengte(time1h, time2h, time1m, time2m);
        uur2.setText("" + tijd4.getHour());
        minuut2.setText(" " + tijd4.getMinute());
        TextView punt2 = findViewById(R.id.textView8);
        punt2.setText(":");
    }

    //berekent de slaaplengte zodat tijd 4 hier automatisch aan kan worden aangepast
    @RequiresApi(api = Build.VERSION_CODES.O)
    private LocalTime getSlaapLengte(int h1, int h2, int m1, int m2) {
        LocalTime tijd1 = LocalTime.of(h1, m1);
        LocalTime tijd2 = LocalTime.of(h2, m2);
        LocalTime tijd3 = LocalTime.of(time3h, time3m);
        long verschil= tijd1.until(tijd2, ChronoUnit.MINUTES);
        int slaaplengte= (int) (1440+ verschil);
        return tijd3.plusMinutes(slaaplengte);
    }

    //Bevestig ingegeven waarden
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onBtnCalculate_Clicked(View caller) {
        //geef tijden door aan Queue
        Intent intent = new Intent(MainActivity.this, QueueActivity.class);
        LocalTime tijd1 = LocalTime.of(time1h, time1m);
        LocalTime tijd2 = LocalTime.of(time2h, time2m);
        LocalTime tijd3;

        if(checked){
            int verschil= calculateTimeDif();
            tijd3= tijd1.plusHours(-1*verschil);
            intent.putExtra("tijd3", tijd3);
        } else {
            tijd3 = LocalTime.of(time3h, time3m);
            intent.putExtra("tijd3", tijd3);
        }

        intent.putExtra("tijd1", tijd1);
        intent.putExtra("tijd2", tijd2);

        startActivity(intent);
    }



    //Bereken tijdverschil tussen gekozen locaties aan de hand van database
    public int calculateTimeDif() {
        int tijdverschil = 0;
        Land current = null;

        //als de permission checkbox niet gechecked is, wordt de locatie via de spinner gehaald
        if(!permission.isChecked()){
            String currentCountry = spinner2.getSelectedItem().toString(); //gekozen huidige locatie
            current = new Land(currentCountry);
            getFromDatabase(current);

        }
        //als de permission checkbox gechecked is, wordt de locatie via de gps gebruikt ipv van uit de spinner
        if(permission.isChecked()){
            current = new Land(country);
            getFromDatabase(current);
        }

        String destination = spinner.getSelectedItem().toString(); //gekozen bestemming
        Land dest = new Land(destination);
        getFromDatabase(dest);

        //bepaling tijdsverschil locaties
        int destTijdzone=dest.getTijdzone();
        int destZomeruur=dest.getZomeruur();
        int currentTijdzone=current.getTijdzone();
        int currentZomeruur=current.getZomeruur();

        if (currentTijdzone > 0 && destTijdzone > 0) {
            tijdverschil =  destTijdzone - currentTijdzone;
        } else {
            if (currentTijdzone > 0 && destTijdzone < 0) {
                tijdverschil = -(currentTijdzone + abs(destTijdzone));
            } else {
                if (currentTijdzone < 0 && destTijdzone > 0) {
                    tijdverschil = abs(currentTijdzone) + destTijdzone;
                } else { //beide zones negatief
                    tijdverschil = -(currentTijdzone - (destTijdzone));
                }
            }
        }

        //Zomer en winteruur
        if(currentZomeruur == 0 && destZomeruur == 1){
            tijdverschil++;}
        else{ if(currentZomeruur ==1 && destZomeruur==0){
            tijdverschil--;
        }}

        //Bepaalt gedrag SLEEPSHIFT knop --> indien tijdverschil groter dan 0 in jetlag mode voldaan
        if(tijdverschil==0 && jetLagMode==true){
            Toast.makeText(this, "No time difference between selected locations or check internet connection", Toast.LENGTH_SHORT).show();
            btncalculate.setClickable(false);
            btncalculate.setBackground(getResources().getDrawable(R.drawable.seek));}
        else {
            if (timeSet || tijdverschil!=0) {
                btncalculate.setClickable(true);
                btncalculate.setBackground(getResources().getDrawable(R.drawable.seek2));
            }}

       return tijdverschil;
    }


    //haalt tijdzone en of er zomer uur is van gekozen locatie
    public void getFromDatabase(Land land) {

        try {

            //website https://studev.groept.be/api/a19SD401/GetTimeZone/name
            URL url = new URL("https://studev.groept.be/api/a19SD401/GetTimeZone/" + land.getNaam());
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            int responseCode = con.getResponseCode();
            System.out.println(responseCode);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));

            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            String dataArray = content.toString();
            String data = dataArray.substring(1, dataArray.length() - 1);

            JSONObject response = new JSONObject(data);
            land.setTijdzone(response.getInt("Tijdzone"));
            land.setZomeruur(response.getInt("heeftZomerUur"));
            con.disconnect();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    //Heeft de gebruiker al toestemming gegeven of niet?
    private void getLocationFromGPS() {
        if (ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_LOCATION_PERMISSION);
        } else {
            getCurrentLocation();
        }
    }

    //zoekt de coordinaten van het toestel en zet ze in longitude en latitude
    public void getCurrentLocation() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.getFusedLocationProviderClient(MainActivity.this)
                .requestLocationUpdates(locationRequest, new LocationCallback() {

                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        LocationServices.getFusedLocationProviderClient(MainActivity.this)
                                .removeLocationUpdates(this);
                        if (locationResult != null && locationResult.getLocations().size() > 0) {
                            int latestLocationIndex = locationResult.getLocations().size() - 1;
                            lat = locationResult.getLocations().get(latestLocationIndex).getLatitude();
                            longi = locationResult.getLocations().get(latestLocationIndex).getLongitude();
                            try {
                                geoCoder();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                }, Looper.getMainLooper());
    }

    //Gaat na of de gebruiker toestemming geeft voor de locatie te delen
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //Converteer coordinaten naar locatie
    public void geoCoder() throws IOException {
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(MainActivity.this, Locale.ENGLISH);
        addresses = geocoder.getFromLocation(lat, longi, 1);
        country = addresses.get(0).getCountryName();
        String city = addresses.get(0).getLocality();
        Log.d("Locatie", "De locatie is " + country + "  " + city);
    }

    public void disableNewSched(){
        TextView tekst = findViewById(R.id.white);
        tekst.setVisibility(TextView.VISIBLE);
        Button btn = findViewById(R.id.btnNewSchedule);
        btn.setVisibility(btn.INVISIBLE);
    }

    public void enableNewSched(){
        TextView tekst = findViewById(R.id.white);
        tekst.setVisibility(TextView.INVISIBLE);
        Button btn = findViewById(R.id.btnNewSchedule);
        btn.setVisibility(btn.VISIBLE);
    }
}