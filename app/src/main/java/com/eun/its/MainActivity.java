package com.eun.its;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_AUDIO_AND_WRITE_EXTERNAL_STORAGE = 0;
    private static final int DURATION = 1;
    private static final String TAG = "EUN_DEBUG";
    private static final String IP = "http://192.168.0.42/its";        // 10.0.2.2
    private static final String PORT = ":9091";
    private static final String url = IP ;
    private static long backKeyPressedTime;                    // 앱종료 위한 백버튼 누른시간
    private static final int ON = 1;
    private static final int OFF = 0;
    private static final int USING = 6;

    private static String status;

    private Button bt[];

    // 나중에 지울 변수들
    private Button bt_clear;
    private Button bt_update;
    private WebView mWebView;
    private TextView tv[];

    private RecyclerView mRecyclerView;
    private ArrayList<PersonalData> mArrayList;
    private UsersAdapter mAdapter;
    private String mJsonString;
    private TextView mTextViewResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_AUDIO_AND_WRITE_EXTERNAL_STORAGE);
        }

        status = new String();
        status = "-1";
        bt = new Button[2];
        tv = new TextView[2];
        bt[0] = findViewById(R.id.bt1);
        bt[1] = findViewById(R.id.bt2);
        tv[0] = findViewById(R.id.tv1);
        tv[1] = findViewById(R.id.tv2);
        bt_clear = findViewById(R.id.bt_clear);
        bt_update = findViewById(R.id.bt_update);
        mWebView = findViewById(R.id.wb);
        mRecyclerView = (RecyclerView) findViewById(R.id.listView_main_list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mArrayList = new ArrayList<>();
        mTextViewResult = (TextView)findViewById(R.id.textView_main_result);

        mAdapter = new UsersAdapter(this, mArrayList);
        mRecyclerView.setAdapter(mAdapter);

        // 웹뷰 설정
        mWebView.loadUrl(url + "/output.php");                          // 서버에 있는 html 파일
        mWebView.addJavascriptInterface(new WebBridge(), "ITS");  // js에서 안드로이드 함수를 쓰기 위한 브릿지 설정 -> window.NOA.functionname();
        mWebView.setWebViewClient(new WebViewClient());                 // 웹뷰 클라이언트
        mWebView.setWebChromeClient(new WebChromeClient());             // 웹뷰 크롬 클라이언트
        mWebView.getSettings().setJavaScriptEnabled(true);              // 웹뷰에서 자바스크립트 사용 가능하게
        mWebView.setWebContentsDebuggingEnabled(true);                  // 크롬에서 웹뷰 디버깅 가능하게

        initialize();

        bt[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setStatus_0to1(0);
            }
        });

        bt[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setStatus_0to1(1);
            }
        });

        bt_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.loadUrl(url + "/clear.php");
                for (int i = 0; i < bt.length; i++) {
                    bt[i].setBackgroundColor(getResources().getColor(R.color.colorPink));
                    bt[i].setEnabled(true);
                }
            }
        });

        bt_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initialize();
                Log.d(TAG, "bt_update");

                mArrayList.clear();
                mAdapter.notifyDataSetChanged();

                GetData task = new GetData();
                task.execute("" + url + "/getjson.php", "");
                Log.d(TAG, "bt_update return");
            }
        });

    }

    void initialize() {
        mWebView.loadUrl(url + "/output.php");

        for (int i = 0; i < bt.length; i++) {
            mWebView.loadUrl("javascript:checkNum('" + i + "');");

            tv[i].setText(status);
            if (!status.equals("0")) {
                bt[i].setEnabled(false);
                bt[i].setBackgroundColor(getResources().getColor(R.color.colorGray));
            } else {
                bt[i].setEnabled(true);
                bt[i].setBackgroundColor(getResources().getColor(R.color.colorPink));
            }
        }
    }


    void setStatus_0to1(final int num) {
        mWebView.loadUrl(url + "/output.php");

        mWebView.loadUrl("javascript:checkNum('" + num + "');");
        tv[num].setText(status);

        if (status.equals("0")) {
            Toast.makeText(MainActivity.this, "자리에 불이 켜졌습니다.", Toast.LENGTH_LONG).show();
            bt[num].setBackgroundColor(getResources().getColor(R.color.colorGreen));
            mWebView.loadUrl(url + "/input.php?id=" + num + "&status=1");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    bt[num].setEnabled(false);
                }
            });
        }
    }

    /***************************************************************************************************************************/

    //뒤로가기 2번하면 앱종료
    @Override
    public void onBackPressed() {
        //1번째 백버튼 클릭
        if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
            backKeyPressedTime = System.currentTimeMillis();
            Toast.makeText(this, getString(R.string.APP_CLOSE_BACK_BUTTON), Toast.LENGTH_SHORT).show();
        }
        //2번째 백버튼 클릭 (종료)
        else {
            AppFinish();
        }
    }

    //앱종료
    public void AppFinish() {
        finish();
        System.exit(0);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /***************************************************************************************************************************/
    /***************************************************************************************************************************/
    /***************************************************************************************************************************/

    // 자바스크립트에서 안드로이드 함수 호출할 때 사용
    // 자바스크립트 코드 : window.ITS.함수이름()
    class WebBridge {
        @JavascriptInterface
        public void receiveData(String data) {
            status = data;
        }

        // 임산부가 좌석을 선택하고 앉았을 경우 호출
        @JavascriptInterface
        public void setStatus_1to7(int num) {
            bt[num].setBackgroundColor(getResources().getColor(R.color.colorGray));
        }

        // 임산부가 좌석에서 일어났을 경우 호출
        @JavascriptInterface
        public void setStatus_7to0(int num) {
            bt[num].setBackgroundColor(getResources().getColor(R.color.colorPink));
            bt[num].setEnabled(true);
        }
    }

    /***************************************************************************************************************************/
    /***************************************************************************************************************************/
    /***************************************************************************************************************************/


    private void showResult() {
        String TAG_JSON = "webnautes";
        String TAG_ID = "id";
        String TAG_STATUS = "status";

        Log.d(TAG, "showResult");


        try {
            JSONObject jsonObject = new JSONObject(mJsonString);
            JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);

            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject item = jsonArray.getJSONObject(i);

                String id = item.getString(TAG_ID);
                String status = item.getString(TAG_STATUS);

                PersonalData personalData = new PersonalData();

                personalData.setMember_id(id);
                personalData.setMember_status(status);

                mArrayList.add(personalData);
                mAdapter.notifyDataSetChanged();
            }


        } catch (JSONException e) {

            Log.d(TAG, "showResult : ", e);
        }

    }

    private class GetData extends AsyncTask<String, Void, String> {

        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG, "onPreExecute");

            progressDialog = ProgressDialog.show(MainActivity.this,
                    "Please Wait", null, true, true);
        }


        @Override
        protected String doInBackground(String... params) {

            String serverURL = params[0];
            String postParameters = params[1];


            try {

                java.net.URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                Log.d(TAG, "httpURLConnection");

                httpURLConnection.setReadTimeout(20000);
                httpURLConnection.setConnectTimeout(20000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();


                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();


                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "response code - " + responseStatusCode);

                InputStream inputStream;
                if (responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                } else {
                    inputStream = httpURLConnection.getErrorStream();
                }


                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }

                bufferedReader.close();

                Log.d(TAG, "doInBackground return");

                return sb.toString().trim();


            } catch (Exception e) {

                Log.d(TAG, "GetData : Error ", e);
                errorString = e.toString();

                return null;
            }

        }


        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();
            mTextViewResult.setText(result);
            Log.d(TAG, "response - " + result);

            if (result == null){

                mTextViewResult.setText(errorString);
            }
            else {

                mJsonString = result;
                showResult();
            }
        }


        protected final void publishProgress (Integer... values){

        }

    }

}
