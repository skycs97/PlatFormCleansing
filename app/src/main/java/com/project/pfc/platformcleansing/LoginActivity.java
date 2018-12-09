package com.project.pfc.platformcleansing;


import android.content.SharedPreferences;
import android.database.Cursor;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import static android.os.Build.ID;
import static android.provider.Telephony.Carriers.PASSWORD;

public class LoginActivity extends AppCompatActivity {

    public static final String PREFERENCES_ID = "";
    public static final String PREFERENCES_PWD = "";

    SharedPreferences setting;

    private BunkerDBHelper dbHelper;

    final TextView loginText = (TextView) findViewById(R.id.LOGIN_text);
    final Button btn_Signin = (Button) findViewById(R.id.btn_signin);
    final Button btn_Signup = (Button) findViewById(R.id.btn_signup);
    final EditText editID = (EditText) findViewById(R.id.user_id);
    final EditText editPASS = (EditText) findViewById(R.id.user_pwd);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setting = getSharedPreferences(PREFERENCES_ID, MODE_PRIVATE);


        dbHelper = new BunkerDBHelper(this);

        btn_Signin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ID = editID.getText().toString();
                Cursor cursor = dbHelper.getUserData(ID);
                cursor.moveToFirst();
                String PASSWORD = editPASS.getText().toString();
                cursor = dbHelper.getUserData(PASSWORD);
                String IDdata = cursor.getString(0);
                String PASSWORDdata = cursor.getString(1);

                if(editID.equals(IDdata) && editPASS.equals(PASSWORDdata)){
                    Toast.makeText(getApplicationContext(), "LOGIN SUCCESSFUL", Toast.LENGTH_LONG).show();
                }
                else{
                    Toast.makeText(getApplicationContext(), "아이디 또는 비밀번호를 잘못입력하셨습니다", Toast.LENGTH_LONG).show();
                }
            }
        });

        saveID(ID);
        savePWD(PASSWORD);

        btn_Signup.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //회원가입 누르면 LOGIN 텍스트와 버튼이 비뀐다
                loginText.setText("회 원 가 입");
                btn_Signup.setText("가입하기");
                btn_Signin.setEnabled(false);

                btn_Signup.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dbHelper.insertUserData(ID, PASSWORD);

                        btn_Signup.setOnClickListener(new View.OnClickListener(){
                            @Override
                            public void onClick(View v) {               //다시 버튼 바꾸고 JOIN버튼 비활도 풀어준다
                                loginText.setText("LOGIN");
                                btn_Signup.setText("회원가입");
                                btn_Signin.setEnabled(true);
                            }
                        });

                    }
                });

            }
        });

    }

    private void saveID(String text){
        SharedPreferences.Editor editor = setting.edit();
        editor.putString(PREFERENCES_ID, text);
    }

    private void savePWD(String text) {
        SharedPreferences.Editor editor = setting.edit();
        editor.putString(PREFERENCES_PWD, text);
    }



}
