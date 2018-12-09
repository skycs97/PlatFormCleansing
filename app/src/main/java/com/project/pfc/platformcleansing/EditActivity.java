package com.project.pfc.platformcleansing;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EditActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap map;                      //맵
    private BunkerDBHelper dbHelper;           //데이터베이스
    private Cursor cursor;                      //커서
    private boolean editFlag;                   //true면 수정, false면 추가
    private FusedLocationProviderClient fusedLocationProviderClient;  //추가시 현재위치 받아오기 위함
    private Location lastLocation;                                      //위치 저장할 곳
    private Address geoAddress;
    private double lastLatitude;
    private double lastLongitude;
    private File photoFile = null;
    private String photoFileName = "image.jpg";
    private Uri imgUri;

    private final static int REQUEST_IMAGE_PICK = 0;
    private final static int REQUEST_IMAGE_CAPTURE = 1;

    EditText name;
    EditText call;
    EditText capacity;
    EditText address;
    EditText remarks;
    ImageButton imageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.edit_map);
        mapFragment.getMapAsync(this);

        dbHelper = new BunkerDBHelper(this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        editFlag = getIntent().getBooleanExtra("edit", false);                  //추가or수정 모드 확인

        name = (EditText) findViewById(R.id.edit_name);
        call = (EditText) findViewById(R.id.edit_call);
        call.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
        capacity = (EditText) findViewById(R.id.edit_capacity);
        address = (EditText) findViewById(R.id.edit_address);
        remarks = (EditText) findViewById(R.id.edit_remarks);
        imageButton = findViewById(R.id.edit_image);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                show();                             //image버튼 클릭시 사진 변경 or 추가
            }
        });
        Button search = (Button) findViewById(R.id.search_button);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchShow();
            }
        });

        if(editFlag){                                    //수정 모드 일 시 database에서 선택한 data 가져와서 값 입력
            int _id = getIntent().getIntExtra("id", -1);
            if(_id > 0) {
                cursor = dbHelper.getDetailData(_id);
                while(cursor.moveToNext()){
                    name.setText(cursor.getString(BunkerContract.CursorIndex.NAME));
                    call.setText(cursor.getString(BunkerContract.CursorIndex.CALL));
                    capacity.setText(cursor.getString(BunkerContract.CursorIndex.CAPACITY));
                    address.setText(cursor.getString(BunkerContract.CursorIndex.ADDRESS));
                    remarks.setText(cursor.getString(BunkerContract.CursorIndex.REMAKRS));
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit_save:
                saveDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        saveDialog();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        if(editFlag){           //수정 모드일 시 데이터베이스에서 넘어온 위도 경도로 카메라 이동 + 마커찍기
            cursor.moveToFirst();
            String name = cursor.getString(BunkerContract.CursorIndex.NAME);
            lastLatitude = cursor.getDouble(BunkerContract.CursorIndex.LATITUDE);
            lastLongitude = cursor.getDouble(BunkerContract.CursorIndex.LONGITUDE);
            addMaker(lastLatitude, lastLongitude, name);
        } else {                //추가 모드일 시 권한 확인 후 현재 위치로 이동 + 마커 찍기
            if(PermissionsStateCheck.permissionState(this, PermissionsStateCheck.permission_location)){
                getLastLocation();
            } else {
                PermissionsStateCheck.setPermissions(EditActivity.this, PermissionsStateCheck.permission_location, 1);
            }
        }
    }
    @SuppressWarnings("MissingPermission")
    private void getLastLocation(){                         //현재 위치 반환
        Task task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null){
                    lastLocation = location;
                    lastLatitude = location.getLatitude();
                    lastLongitude = location.getLongitude();
                    addMaker(lastLatitude, lastLongitude, "현재위치");
                } else {
                    Toast.makeText(getApplicationContext(), "위치정보를 얻어오는데 실패했습니다!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED){
            getLastLocation();
        }
    }

    public void addMaker(double latitude, double longitude, String name){
        map.clear();                    //마커 지우기
        LatLng latLng = new LatLng(latitude, longitude);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        map.addMarker(new MarkerOptions().alpha(0.8f).title(name).position(latLng));
    }

    public void inputAddress(String string){
        if(geoAddress != null){
            lastLatitude = geoAddress.getLatitude();
            lastLongitude = geoAddress.getLongitude();
            addMaker(lastLatitude, lastLongitude, string);
            name.setText(string);
            call.setText(geoAddress.getPhone());
            address.setText(geoAddress.getAddressLine(0).substring(5));   //대한민국자르기
        }
    }

    private void dispatchPickPictureIntent(){
        Intent picPicture = new Intent(Intent.ACTION_PICK);
        picPicture.setType("image/*");
        if(picPicture.resolveActivity(getPackageManager()) != null){
            photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), photoFileName);
            picPicture.setData(FileProvider.getUriForFile(this, "com.project.pfc.platformcleansing", photoFile));
            startActivityForResult(picPicture, REQUEST_IMAGE_PICK);
        }
    }

    private void dispatchTakePictureIntent(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if(takePictureIntent.resolveActivity(getPackageManager()) != null){
            photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), photoFileName);

            if (photoFile != null){
                Uri imageUri = FileProvider.getUriForFile(this, "com.project.pfc.platformcleansing", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK){
            imgUri = data.getData();
            imageButton.setImageURI(imgUri);
        } else if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK){
            imageButton.setImageURI(Uri.fromFile(photoFile));
        }
    }

    public void show(){                         //이미지 버튼 클릭 시 사진을 갤러리 or 카메라에서 가져올 것 선택하는 부분
        final List<String> ListItems = new ArrayList<>();
        ListItems.add("갤러리에서 가져오기");
        ListItems.add("카메라로 사진 찍기");

        final CharSequence[] items = ListItems.toArray(new String[ListItems.size()]);
        final List SelectedItems = new ArrayList();
        int defaultItem = 0;
        SelectedItems.add(defaultItem);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("사진 추가하기");
        builder.setSingleChoiceItems(items, defaultItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SelectedItems.clear();
                SelectedItems.add(which);
            }
        });
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!SelectedItems.isEmpty()){
                    int index = (int) SelectedItems.get(0);
                    if(index == 0){
                        dispatchPickPictureIntent();
                    } else if (index == 1) {
                        dispatchTakePictureIntent();
                    }
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.show();
    }
    public void saveDialog(){                     //delete 버튼 클릭시 안내창
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save");
        builder.setMessage("저장하시겠습니까?");
        builder.setPositiveButton("저장", new DialogInterface.OnClickListener() {          //예  눌렀을 시
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String nameText = name.getText().toString();
                String callText = call.getText().toString();
                int capacityText = Integer.parseInt(capacity.getText().toString());
                String addressText = address.getText().toString();
                String remarksText = remarks.getText().toString();
                try{
                    if(editFlag) {
                        dbHelper.updateBunkerData(nameText, callText, lastLatitude, lastLongitude, addressText, capacityText, remarksText,photoFile.toString() ,cursor.getInt(BunkerContract.CursorIndex._ID));
                    } else {
                        dbHelper.insertBunkerData(nameText, callText, lastLatitude, lastLongitude, addressText, capacityText, remarksText, "Admin");
                        setResult(RESULT_OK);
                    }
                    Toast.makeText(getApplicationContext(), "저장 성공", Toast.LENGTH_SHORT).show();
                } catch (SQLException e){
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "저장 실패", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                }
                EditActivity.super.onBackPressed();
            }
        });
        builder.setNegativeButton("저장하지 않음", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setResult(RESULT_CANCELED);
                EditActivity.super.onBackPressed();
            }
        });
        builder.setNeutralButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.show();
    }
    public void getAddress(String spot){
        Geocoder geocoder = new Geocoder(this, Locale.KOREA);
        try {
            List<Address> addresses = geocoder.getFromLocationName(spot, 1);
            geoAddress = addresses.get(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void searchShow()
    {
        final EditText edittext = new EditText(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("장소검색");
        builder.setMessage("장소를 입력하세요");
        builder.setView(edittext);
        builder.setPositiveButton("검색",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String spot = edittext.getText().toString();
                        getAddress(spot);
                        inputAddress(spot);
                    }
                });
        builder.setNegativeButton("취소",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder.show();
    }

}
