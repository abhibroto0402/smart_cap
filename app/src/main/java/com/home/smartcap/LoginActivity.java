package com.home.smartcap;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {
    //Declaration of objects on the login screen

    private Button mlogin = null;
    private TextView _signupLink;
    private EditText emailid;
    private EditText password;
    public String emailId, pswd,med_taken_times,humdityAlert,tempAlert;
    private String jsonData = "Testing";
    private boolean isValid = false;
    private String result = "blank";
    private String user_json=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        final Bundle extras = getIntent().getExtras();
        try {
            if (Boolean.parseBoolean(extras.getString("load"))) {
                emailId = extras.getString("emailId");
                pswd = extras.getString("pswd");
                new AuthenticationLogin().execute(ServerUtil.getUsersEndpoint(emailId, pswd));
                Intent i = new Intent(LoginActivity.this, HomeActivity.class);
                i.putExtra("jsonData", jsonData);
                i.putExtra("emailId", emailId);
                i.putExtra("user_json", user_json);
                i.putExtra("mtimes", med_taken_times);
                i.putExtra("talert", tempAlert);
                i.putExtra("halert", humdityAlert);
                i.putExtra("pswd", pswd);
                startActivity(i);
                finish();
            }
        }catch (NullPointerException e){

        }

        //Initialization of all elements
        _signupLink = (TextView) findViewById(R.id.link_signup);
        mlogin = (Button) findViewById(R.id.login);
        emailid = (EditText) findViewById(R.id.emailid);
        password = (EditText) findViewById(R.id.password);

        //Events for Object click
        _signupLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(view.getContext(), SignUpActivity.class);
                startActivity(i);
            }
        });

        //Event of login
        mlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                emailId = emailid.getText().toString();
                pswd = password.getText().toString();
                isValid = false;
                String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
                CharSequence inputStr = emailId;
                Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(inputStr);
                if (matcher.matches()) {
                    isValid = true;
                }
                if (isValid) {
                    String tempUrl = ServerUtil.getUsersEndpoint(emailId,pswd);
                    for(int i = 0; i<2 ;i++)
                        new AuthenticationLogin().execute(tempUrl);
                    showMessage();
                    if (jsonData != "Testing") {
                        Intent i = new Intent(view.getContext(), HomeActivity.class);
                        i.putExtra("jsonData", jsonData);
                        i.putExtra("emailId", emailId);
                        i.putExtra("user_json", user_json);
                        i.putExtra("mtimes", med_taken_times);
                        i.putExtra("talert", tempAlert);
                        i.putExtra("halert", humdityAlert);
                        i.putExtra("pswd",pswd);
                        startActivity(i);
                        finish();
                    }
                } else
                    throwError("Error in email Format");

            }

        });

    }

    private void throwError(String error) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    private void showMessage() {
        if (result != "blank")
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
    }

    public class AuthenticationLogin extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... urls) {
            StringBuilder result = new StringBuilder();
            StringBuilder info = new StringBuilder();
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }
                user_json= result.toString();
                rd.close();
                if (conn.getResponseCode() == 200) {
                    url = new URL(ServerUtil.getPatientEndpoint(emailId));
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.connect();
                    rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line = rd.readLine()) != null) {
                        info.append(line);
                    }
                    rd.close();
                    jsonData = info.toString();
                    getEvents(new URL(ServerUtil.getBaseEndpoint()+"event/"+emailId));
                    return "200";
                } else
                    return user_json;
            } catch (Exception e) {
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                if (result == "blank" || s == "200")
                    result = "Login Successful";
                else
                    result = "Login Failure. Try again" + user_json;
            } catch (NullPointerException e) {
                result = "Login Failed";
            }
        }
        private void getEvents(URL url){

            StringBuilder result = new StringBuilder();
            JSONObject parent;
            try {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }
                rd.close();
                parent = new JSONObject(result.toString());
                med_taken_times= parent.getJSONObject("Event").getString("open_cap_event_times");
                tempAlert=  parent.getJSONObject("Event").getString("temp_alert");
                humdityAlert= parent.getJSONObject("Event").getString("humidity_alert");


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
