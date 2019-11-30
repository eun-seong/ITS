package com.eun.its;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
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
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements BeaconConsumer {
    private static final int REQUEST_CODE_AUDIO_AND_WRITE_EXTERNAL_STORAGE = 0;
    private static long backKeyPressedTime;                    // 앱종료 위한 백버튼 누른시간
    private static final int DELAY_TIME = 2000;
    private static final String TAG = "EUN_DEBUG";
    private static final String IP = "http://192.168.0.42/its";        // 10.0.2.2
    private static final String url = IP;

    private Button bt[];

    // 나중에 지울 변수들
    private Button bt_clear;
    private Button bt_update;
    private WebView mWebView;

    private RecyclerView mRecyclerView;
    private ArrayList<PersonalData> mArrayList;
    private UsersAdapter mAdapter;
    private String mJsonString;
    private TextView mTextViewResult;

    private BeaconManager beaconManager;
    // 감지된 비콘들을 임시로 담을 리스트
    private List<Beacon> beaconList = new ArrayList<>();
    private TextView textView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_AUDIO_AND_WRITE_EXTERNAL_STORAGE);
        }

        bt = new Button[2];
        bt[0] = findViewById(R.id.bt1);
        bt[1] = findViewById(R.id.bt2);

        bt_clear = findViewById(R.id.bt_clear);
        bt_update = findViewById(R.id.bt_update);
        mWebView = findViewById(R.id.wb);
        mRecyclerView = (RecyclerView) findViewById(R.id.listView_main_list);
        mTextViewResult = (TextView) findViewById(R.id.textView_main_result);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mArrayList = new ArrayList<>();

        mAdapter = new UsersAdapter(this, mArrayList);
        mRecyclerView.setAdapter(mAdapter);

        // 웹뷰 설정
        mWebView.loadUrl(url + "/output.php");                          // 서버에 있는 html 파일
        mWebView.addJavascriptInterface(new WebBridge(), "ITS");  // js에서 안드로이드 함수를 쓰기 위한 브릿지 설정 -> window.ITS.functionname();
        mWebView.setWebViewClient(new WebViewClient());                 // 웹뷰 클라이언트
        mWebView.setWebChromeClient(new WebChromeClient());             // 웹뷰 크롬 클라이언트
        mWebView.getSettings().setJavaScriptEnabled(true);              // 웹뷰에서 자바스크립트 사용 가능하게
        mWebView.setWebContentsDebuggingEnabled(true);                  // 크롬에서 웹뷰 디버깅 가능하게


        // 실제로 비콘을 탐지하기 위한 비콘매니저 객체를 초기화
        beaconManager = BeaconManager.getInstanceForApplication(this);
        textView = (TextView) findViewById(R.id.Textview);

        // 여기가 중요한데, 기기에 따라서 setBeaconLayout 안의 내용을 바꿔줘야 하는듯 싶다.
        // 필자의 경우에는 아래처럼 하니 잘 동작했음.
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        // 비콘 탐지를 시작한다. 실제로는 서비스를 시작하는것.
        beaconManager.bind(this);

        update();

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
                Log.d(TAG, "bt_update");

                update();
            }
        });
    }

    void update() {
        mArrayList.clear();
        mAdapter.notifyDataSetChanged();

        GetData task = new GetData();
        task.execute("" + url + "/getjson.php", "");
        Log.d(TAG, "update :" + "Array Size : " + mArrayList.size());
    }

    void print() {
        Log.d(TAG, "print :" + "Array Size : " + mArrayList.size());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });


    }

    // PHP로 id가 num인 데이터의 status를 0에서 1로 바꾼다.
    void setStatus_0to1(final int num) {
        mWebView.loadUrl(url + "/output.php");

        if (mArrayList.get(num).getMember_status().equals("0")) {
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
    /***************************************************************************************************************************/
    /***************************************************************************************************************************/
    // Beacon
    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            // 비콘이 감지되면 해당 함수가 호출된다. Collection<Beacon> beacons에는 감지된 비콘의 리스트가,
            // region에는 비콘들에 대응하는 Region 객체가 들어온다.
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    beaconList.clear();
                    for (Beacon beacon : beacons) {
                        beaconList.add(beacon);
                    }
                } else {
                    Log.d(TAG, "onBeaconServiceConnect " + beacons.size());
                }
            }

        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {

        }
    }

    // 버튼이 클릭되면 textView 에 비콘들의 정보를 뿌린다.
    public void OnButtonClicked(View view) {
        // 아래에 있는 handleMessage를 부르는 함수. 맨 처음에는 0초간격이지만 한번 호출되고 나면
        // 1초마다 불러온다.
        handler.sendEmptyMessage(0);
    }

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            textView.setText("");

            // 비콘의 아이디와 거리를 측정하여 textView에 넣는다.
            for (Beacon beacon : beaconList) {
                textView.append("ID : " + beacon.getId2() + " / " + "Distance : " + Double.parseDouble(String.format("%.3f", beacon.getDistance())) + "m\n");
            }

            // 자기 자신을 1초마다 호출
            handler.sendEmptyMessageDelayed(0, 1000);
        }
    };


    /***************************************************************************************************************************/
    /***************************************************************************************************************************/
    /***************************************************************************************************************************/

    private void showResult() {
        String TAG_JSON = "webnautes";
        String TAG_ID = "id";
        String TAG_STATUS = "status";

        Log.d(TAG, "showResult" + "Array Size : " + mArrayList.size());


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

                    if (status.equals("0")) {
                        bt[i].setEnabled(true);
                        bt[i].setBackgroundColor(getResources().getColor(R.color.colorPink));
                    } else if (status.equals("1")) {
                        bt[i].setEnabled(false);
                        bt[i].setBackgroundColor(getResources().getColor(R.color.colorGreen));
                    } else if (status.equals("7")){
                        bt[i].setBackgroundColor(getResources().getColor(R.color.colorGray));
                    }

                mArrayList.add(personalData);
                mAdapter.notifyDataSetChanged();
                Log.d(TAG, "showResult :" + "Array Size : " + mArrayList.size());
            }


        } catch (JSONException e) {

            Log.d(TAG, "showResult : ", e);
        }

    }

    private class GetData extends AsyncTask<String, Void, String> {

        //        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG, "onPreExecute" + "Array Size : " + mArrayList.size());

//            progressDialog = ProgressDialog.show(MainActivity.this,
//                    "Please Wait", null, true, true);
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

//            progressDialog.dismiss();
            mTextViewResult.setText(result);
            Log.d(TAG, "response - " + result);

            if (result == null) {

                mTextViewResult.setText(errorString);
            } else {

                mJsonString = result;
                showResult();
            }

            Log.d(TAG, "onPostExecute :" + "Array Size : " + mArrayList.size());

        }
    }

    /***************************************************************************************************************************/

    class WebBridge {
        @JavascriptInterface
        public void alertUpdate() {  // 목적지에 도착할 경우 실행
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    mArrayList.clear();
                    mAdapter.notifyDataSetChanged();

                    GetData task = new GetData();
                    task.execute("" + url + "/getjson.php", "");
                    Log.d(TAG, "alertUpdate :" + "Array Size : " + mArrayList.size());

                }
            });
        }

        @JavascriptInterface
        public void outputUpdate() {  // 목적지에 도착할 경우 실행
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mWebView.loadUrl(url + "/output.php");
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
}
