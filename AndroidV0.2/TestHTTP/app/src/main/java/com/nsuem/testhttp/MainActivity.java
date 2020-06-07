package com.nsuem.testhttp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    public static String aJsonString = "";
    ImageView imgTest;
    private static final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");
    File fileImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imgTest = (ImageView)findViewById(R.id.imageView);
        SetImgFromAssets();
        try {
            // Google Play will install latest OpenSSL
            ProviderInstaller.installIfNeeded(getApplicationContext());
            SSLContext sslContext;
            sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, null, null);
            sslContext.createSSLEngine();
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException
                | NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
        //Uri path = Uri.parse("file://android_asset/fan.png");
        //String newPath = path.toString();
        //fileImg = new File(newPath);


        try {
            File file = new File(this.getFilesDir(), "fan.png");
            if (file.exists() && file.length() > 0) {
                //return file.getAbsolutePath();
            }else{
                file.createNewFile();
            }
            //AssetManager astM = getAssets();
            //S//tring[] strL = astM.list("");
            InputStream stream = getAssets().open("fan.png");
            //try (InputStream is = astM.open("fan.png")) {
                try (OutputStream os = new FileOutputStream(file)) {
                    byte[] buffer = new byte[4 * 1024];
                    int read;
                    while ((read = stream.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                    os.flush();
                    fileImg = file;
                }
//                return file.getAbsolutePath();
            //}
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(fileImg.exists()){
            Log.v("dd",fileImg.getAbsolutePath());
        }
        new SendLoginData().execute();
    }

    class SendLoginData extends AsyncTask<Void, Void, Void> {

        String resultString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            final OkHttpClient client = new OkHttpClient();
            URL url;
            HttpsURLConnection connection = null;
//
            try {
                url = new URL("https://www.visionhub.ru/api/v2/auth/generate_token/");
                connection = (HttpsURLConnection) url.openConnection();
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = br.readLine();
                JSONObject jObject = new JSONObject(line);
                aJsonString = jObject.getString("token");

                MediaType mediaType = MediaType.parse("text/plain");
                RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("model", "face-blurring")
                        .addFormDataPart("image", "fan.png",
                                RequestBody.create(MediaType.parse("application/octet-stream"),
                                        fileImg))
                        .build();
                Request request = new Request.Builder()
                        .url("http://www.visionhub.ru/api/v2/process/img2img/")
                        .method("POST", body)
                        .addHeader("Authorization", "Bearer " + aJsonString)
                        .build();
                Response response = client.newCall(request).execute();

                if (!response.isSuccessful())
                    throw new IOException("Unexpected code " + response);

                Headers responseHeaders = response.headers();
                for (int i = 0; i < responseHeaders.size(); i++) {
                    System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
                }
                JSONObject jObject2 = new JSONObject(response.body().string());
                System.out.println(response.body().string());
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }


            /*
            try {
                String myURL = "http://www.visionhub.ru/api/v2/process/img2img/";
                //String parammetrs = "header=1&param2=XXX";
                Uri path = Uri.parse("file:///android_asset/fan.png");

                String newPath = path.toString();
//                String parammetrs = "\\\n" +
//                        "--header 'Authorization: " + aJsonString +"’ \\\n" +
//                        "--form 'model=face-blurring’ \\\n" +
//                        "--form 'image=" + newPath +"’";
                String parammetrs = " --header 'Authorization: " + aJsonString +"’ " +
                        "--form 'model=face-blurring’ " +
                        "--form 'image=" + newPath +"’";
                byte[] data = null;
                InputStream is = null;

                try {
                    url = new URL(myURL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    //conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    conn.setRequestProperty("Content-Length", "" + Integer.toString(parammetrs.getBytes().length));
                    OutputStream os = conn.getOutputStream();
                    data = parammetrs.getBytes("UTF-8");
                    os.write(data);
                    data = null;

                    conn.connect();
//                    int responseCode = conn.getResponseCode();

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

//                    if (responseCode == 200) {
                        is = conn.getInputStream();

                        byte[] buffer = new byte[8192]; // Такого вот размера буфер
                        // Далее, например, вот так читаем ответ
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            baos.write(buffer, 0, bytesRead);
                        }
                        data = baos.toByteArray();
                        resultString = new String(data, "UTF-8");
//                    } else {
//                    }
                    JSONObject jObject = new JSONObject(resultString);
                    aJsonString = jObject.getString("token");

                } catch (MalformedURLException e) {
                    resultString = "MalformedURLException:" + e.getMessage();
                } catch (IOException e) {

                    resultString = "IOException:" + e.getMessage();
                } catch (Exception e) {

                    resultString = "Exception:" + e.getMessage();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

             */
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if(resultString != null) {
                Toast toast = Toast.makeText(getApplicationContext(), resultString, Toast.LENGTH_SHORT);
                toast.show();
            }

        }
    }

    public void GerResponse() throws UnirestException {
        Unirest.setTimeouts(0, 0);
        HttpResponse<String> response = Unirest.post("http://www.visionhub.ru/api/v2/process/img2img/")
                .header("Authorization", "Bearer "+ aJsonString)
                .field("model", "image-colorization")
                .field("file", fileImg)
                .asString();
        Log.v("dd",response.toString());
    }

    void SetImgFromAssets(){
        try {
            InputStream stream = getAssets().open("fan.png");
            Drawable d = Drawable.createFromStream(stream, null);
            imgTest.setImageDrawable(d);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public AssetManager getAssets() {
        // Ensure we're returning assets with the correct configuration.
        return getResourcesInternal().getAssets();
    }

    private Configuration mOverrideConfiguration;
    private Resources mResources;
    private Resources getResourcesInternal() {
        if (mResources == null) {
            if (mOverrideConfiguration == null) {
                mResources = super.getResources();
            } else {
                final Context resContext = createConfigurationContext(mOverrideConfiguration);
                mResources = resContext.getResources();
            }
        }
        return mResources;
    }
}
