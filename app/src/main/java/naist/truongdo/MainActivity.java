package naist.truongdo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telecom.Call;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import vn.vais.myapplication.BuildConfig;
import vn.vais.myapplication.R;

public class MainActivity extends AppCompatActivity {

    public static final String USER_AGENT = "Mozilla/5.0";
    Map m1;
    String username;
    String password;
    ProgressDialog dialog;
    Button btnLogin;
    Button btnSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        disableSSLCertificateChecking();
        btnLogin = (Button)findViewById(R.id.btn_login);
        btnSetting = (Button)findViewById(R.id.btn_setting);


        dialog = new ProgressDialog(this);
        dialog.setTitle("Logging in");
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });

        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent i = new Intent(MainActivity.this, MyPreferencesActivity.class);
                startActivity(i);
            }
        });

        Bundle b = getIntent().getExtras();
        int value = -1; // or other values

        if(b != null){
            value = b.getInt("mandara_login");
            if (value == 1){
                login();
            }
        }
        //new CallAPI().execute();
    }

    public void login(){
        m1 = new HashMap();
        m1.put("name", "authenticate");

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        username = SP.getString("username", "");
        password = SP.getString("password","");

        if (username.equals("") || password.equals("")){
            Toast.makeText(MainActivity.this, "Please config your username and password first!", Toast.LENGTH_LONG).show();
        }else{
            m1.put("user", username);
            m1.put("password", password);
            new CallAPI().execute();
        }
    }
    private static void disableSSLCertificateChecking() {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // Not implemented
            }

            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // Not implemented
            }
        } };

        try {
            SSLContext sc = SSLContext.getInstance("TLS");

            sc.init(null, trustAllCerts, new java.security.SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public class CallAPI extends AsyncTask<String, String, String> {

        public CallAPI(){
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.show();
        }


        @Override
        protected String doInBackground(String... params) {
            return sendPost("https://aruba.naist.jp/cgi-bin/login", m1);
        }


        @Override
        protected void onPostExecute(String result) {
            //Update the UI
            if (result == "ok"){
                Toast.makeText(MainActivity.this, "Login successfully!", Toast.LENGTH_LONG).show();
                finish();
            }else{
                Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
            }
            if (dialog.isShowing())
                dialog.dismiss();
        }
    }

    public static String sendPost(String _url, Map<String, String> parameter) {
        StringBuilder params = new StringBuilder();
        String result = BuildConfig.FLAVOR;
        try {
            for (String s : parameter.keySet()) {
                params.append("&" + s + "=");
                params.append(URLEncoder.encode((String) parameter.get(s), "UTF-8"));
            }
            String url = _url;
            HttpsURLConnection con = (HttpsURLConnection) new URL(_url).openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Accept-Language", "UTF-8");
            con.setDoOutput(true);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(con.getOutputStream());
            outputStreamWriter.write(params.toString());
            outputStreamWriter.flush();
            int responseCode = con.getResponseCode();
            /*System.out.println("\nSending 'POST' request to URL : " + url);
            System.out.println("Post parameters : " + params);
            System.out.println("Response Code : " + responseCode);*/

            try{
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuffer response = new StringBuffer();
                while (true) {
                    String inputLine = in.readLine();
                    if (inputLine != null) {
                        response.append(inputLine + "\n");
                    } else {
                        in.close();
                        break;
                        //return response.toString();
                    }
                }

                String res = response.toString();
                if (res.contains("Authentication failed")){
                    return "Cannot login, wrong username or password";
                }
            }catch (FileNotFoundException e){
            }

            if (responseCode == 200 || responseCode == 500){
                return "ok";
            }else{
                return "Cannot login " + "<response code>: " + responseCode;
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return result;
        } catch (MalformedURLException e2) {
            e2.printStackTrace();
            return result;
        } catch (ProtocolException e3) {
            e3.printStackTrace();
            return result;
        } catch (Throwable th) {
            th.printStackTrace();
            return result;
        }
    }

}
