package com.leo.calendarprovidermanager_project;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.leo.calendarprovidermanager.CalendarAlarmDateType;
import com.leo.calendarprovidermanager.CalendarProviderManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.security.auth.login.LoginException;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {

    private TextView mTvEvents;
    private CheckBox mCbAlarm;
    private CalendarProviderManager.Builder mBuilder;

    private EditText mEdtEventTitle;
    private EditText mEdtEventDescription;
    private EditText mEdtInputEventId;

    private long id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTvEvents = findViewById(R.id.tv_events);
        mCbAlarm = findViewById(R.id.cb_alarm);
        mEdtEventTitle = findViewById(R.id.edt_input_event_title);
        mEdtEventDescription = findViewById(R.id.edt_input_event_description);
        mEdtInputEventId = findViewById(R.id.edt_input_event_id);




        mBuilder = new CalendarProviderManager.Builder(this);

        initClick();

    }

    private void initClick() {
        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivityPermissionsDispatcher.addWithPermissionCheck(MainActivity.this);
            }
        });

        findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mEdtInputEventId.getText().toString())){
                    Toast.makeText(MainActivity.this,"请输入event id",Toast.LENGTH_LONG).show();
                    return;
                }
                MainActivityPermissionsDispatcher.deleteWithPermissionCheck(MainActivity.this,
                        new Long(mEdtInputEventId.getText().toString().trim()));
            }
        });

        findViewById(R.id.btn_update).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mEdtInputEventId.getText().toString())){
                    Toast.makeText(MainActivity.this,"请输入event id",Toast.LENGTH_LONG).show();
                    return;
                }
                MainActivityPermissionsDispatcher.updateWithPermissionCheck(MainActivity.this,
                        new Long(mEdtInputEventId.getText().toString().trim()));
            }
        });

        findViewById(R.id.btn_query).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivityPermissionsDispatcher.queryWithPermissionCheck(MainActivity.this);
            }
        });
    }


    @NeedsPermission({Manifest.permission.READ_CALENDAR,Manifest.permission.WRITE_CALENDAR})
    public void add() {

        long eventId = mBuilder
                .setCalendarName(null)
                .setEvent(mEdtEventTitle.getText().toString(), mEdtEventDescription.getText().toString())
                .setHasAlarm(mCbAlarm.isChecked())
                .setAlarmStartTime(date2TimeMillis("2019-12-07 14:00:00"))
                .setAlarmDurationTime(10)
                .setAlarmLeadTime(30, CalendarAlarmDateType.MINUTE)
                .build()
                .addCalendarEvent();
        id = eventId;
        if (eventId > -1){
            showText("添加:成功, EVENT ID = "+eventId);
            mEdtInputEventId.setText(String.valueOf(eventId));
        }
        else {
            showText("添加:失败");

        }

    }

    @NeedsPermission({Manifest.permission.READ_CALENDAR,Manifest.permission.WRITE_CALENDAR})
    public void delete(long eventId) {

        boolean isDelete = mBuilder.build().deleteCalendarEvent(eventId);
       if (isDelete){
           showText("删除:成功");
           mEdtInputEventId.setText(null);
       }
       else
       {
          showText("删除:失败，请检查 EVENT ID 是否正确");
       }
    }

    @NeedsPermission({Manifest.permission.READ_CALENDAR,Manifest.permission.WRITE_CALENDAR})
    public void update(long eventId) {

        boolean isUpdate = mBuilder
                .setHasAlarm(true)
                .setAlarmStartTime(date2TimeMillis("2019-12-07 15:00:00"))
                .setAlarmLeadTime(2,CalendarAlarmDateType.MINUTE)
                .build()
                .updateCalendarEvent(id);
        Log.e("tag","is hasAlarm "+mCbAlarm.isChecked());


            showText(isUpdate?"更新:成功":"更新:失败，请检查 EVENT ID 是否正确");


    }

    @NeedsPermission({Manifest.permission.READ_CALENDAR,Manifest.permission.WRITE_CALENDAR})
    public void query() {

        long eventId = mBuilder.build().queryCalendarEvent(mEdtEventTitle.getText().toString(), mEdtEventDescription.getText().toString());

        showText("查询:"+(eventId>-1 ? "事件存在, EVENT ID = "+eventId:"事件不存在"));
    }

    public void showText(String s){
        mTvEvents.append(s+"\n");
    }

    public static long date2TimeMillis(String date)  {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return sdf.parse(date).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;

    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this,requestCode,grantResults);
    }
}
