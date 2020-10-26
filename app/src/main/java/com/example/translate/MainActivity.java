package com.example.translate;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

//dohyun
import android.content.Intent;
import android.content.res.AssetManager;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
public class MainActivity extends AppCompatActivity {
    private EditText translate_text;
    private Button translate_btn;
    private TextView resultText;
    private  String result;
    //dohyun
    private Button mBtnCameraView;
    private EditText mEditOcrResult;
    private String datapath = "";
    private String lang = "";
    private int ACTIVITY_REQUEST_CODE = 1;

    static TessBaseAPI sTess;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        translate_text = findViewById(R.id.edit_txt);
        translate_btn = findViewById(R.id.translate_btn);
        resultText = findViewById(R.id.textview);

        translate_btn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                new BackgroundTask().execute();
            }
        });
        // 뷰 선언
        mBtnCameraView = (Button) findViewById(R.id.btn_camera);
        mEditOcrResult = (EditText) findViewById(R.id.edit_ocrresult);
        sTess = new TessBaseAPI();


        // Tesseract 인식 언어를 한국어로 설정 및 초기화
        lang = "kor";
        datapath = getFilesDir()+ "/tesseract";

        if(checkFile(new File(datapath+"/tessdata")))
        {
            sTess.init(datapath, lang);
        }

        mBtnCameraView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // 버튼 클릭 시

                // Camera 화면 띄우기
                Intent mIttCamera = new Intent(MainActivity.this, CameraView.class);
                startActivityForResult(mIttCamera, ACTIVITY_REQUEST_CODE);

            }
        });
    }
    boolean checkFile(File dir)
    {
        //디렉토리가 없으면 디렉토리를 만들고 그후에 파일을 카피
        if(!dir.exists() && dir.mkdirs()) {
            copyFiles();
        }
        //디렉토리가 있지만 파일이 없으면 파일카피 진행
        if(dir.exists()) {
            String datafilepath = datapath + "/tessdata/" + lang + ".traineddata";
            File datafile = new File(datafilepath);
            if(!datafile.exists()) {
                copyFiles();
            }
        }
        return true;
    }

    void copyFiles()
    {
        AssetManager assetMgr = this.getAssets();

        InputStream is = null;
        OutputStream os = null;

        try {
            is = assetMgr.open("tessdata/"+lang+".traineddata");

            String destFile = datapath + "/tessdata/" + lang + ".traineddata";

            os = new FileOutputStream(destFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            is.close();
            os.flush();
            os.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK)
        {
            if(requestCode== ACTIVITY_REQUEST_CODE)
            {
                // 받아온 OCR 결과 출력
                mEditOcrResult.setText(data.getStringExtra("STRING_OCR_RESULT"));
            }
        }
    }



    class BackgroundTask extends AsyncTask<Integer, Integer, Integer>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(Integer... integers) {

            StringBuilder output = new StringBuilder();
            String clientId = "rLN7Ft6XFCUvefzEzflo";
            String clientSecret = "fLVtQIPpzU";

            try{

                String text = URLEncoder.encode(translate_text.getText().toString());
                String apiURL = "https://openapi.naver.com/v1/papago/n2mt";

                URL url = new URL(apiURL);
                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("X-Naver-Client-Id", clientId);
                con.setRequestProperty("X-Naver-Client-Secret", clientSecret);


                String postParams = "source=ko&target=en&text=" + text;
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(postParams);
                wr.flush();
                wr.close();

                int responseCode = con.getResponseCode();
                BufferedReader br;
                if(responseCode == 200){
                    br = new BufferedReader(new InputStreamReader(con.getInputStream()));

                }
                else{
                    br = new BufferedReader(new InputStreamReader(con.getErrorStream()));

                }
                String inputLine;
                while((inputLine = br.readLine()) != null){
                    output.append(inputLine);

                }

                br.close();




            }catch(Exception e){
                Log.e("error", "error comes out", e);
                e.printStackTrace();
            }
            result = output.toString();

            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(result);

//            Log.e("error","content : " +element.getAsJsonObject().get("message").getAsJsonObject().get("result").getAsJsonObject().get("translatedText").getAsString());

            if(element.getAsJsonObject().get("errorMessage") != null){
                Log.e("번역오류", "[오류코드: "+element.getAsJsonObject().get("errorCode").getAsString() + " ]");
            }
            else if(element.getAsJsonObject().get("message") != null){
                String x = element.getAsJsonObject().get("message").getAsJsonObject().get("result").getAsJsonObject().get("translatedText").getAsString();
                resultText.setText(x);

            }

        }
    }


}