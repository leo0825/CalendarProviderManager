package com.leo.calendarprovidermanager;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import java.util.TimeZone;

/**
 * Created by : Leo
 * Date : 2019/12/3
 * Describe :
 */
@SuppressLint("MissingPermission")

public final class CalendarProviderManager {
    private Context mContext;
    //事件开始时间 时间戳
    private long mAlarmStartTime;
    //事件持续时长 默认10分钟
    private int mAlarmDurationTime;

    //可选  是否闹钟提醒
    private boolean hasAlarm;
    //可选  提前提醒时间
    private long mAlarmLeadTime;
    //提前提醒时间类型 时、分、秒、天
    private CalendarAlarmDateType mAlarmDateType;

    //可选  新建日历名称
    private String mCalendarName;
    private String mAccountName;
    private String mAccountDisplayName;
    private String mAccountType = "LOCAL";

    //添加事件的标题
    private String mEventTitle;
    //添加事件的描述
    private String mEventDescription;

    private String TAG = "calendar";

    private  CalendarProviderManager(Builder builder) {
        mContext = builder.mContext;
        hasAlarm = builder.hasAlarm;
        mAlarmStartTime = builder.mAlarmStartTime;
        if (mAlarmStartTime <= 0) {
            mAlarmStartTime = System.currentTimeMillis();
        }

        mAlarmDurationTime = builder.mAlarmDurationTime;
        mAlarmLeadTime = builder.mAlarmLeadTime;
        mAlarmDateType = builder.mAlarmDateType;
        mCalendarName = builder.mCalendarName;
        if (!TextUtils.isEmpty(mCalendarName)) {
            mAccountName = "calendar@" + builder.mCalendarName + ".com";
            mAccountDisplayName = mCalendarName;
        }
        mEventTitle = builder.mEventTitle;
        mEventDescription = builder.mEventDescription;
    }


    /**
     * 查询是否存在calendar 账户
     * 如果accountName为空，使用已有账户，否则使用指定账户
     *
     * @return account id,  -1 表示不存在账户
     */
    private long checkCalendarAccount() {
        long accountId = -1;
        String selections = null;
        String[] selectionArgs = null;
        if (!TextUtils.isEmpty(mAccountName)) {
            selections = CalendarContract.Calendars.ACCOUNT_NAME + "=?";
            selectionArgs = new String[]{mAccountName};
        }
        Cursor cursor = mContext.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,
                null, selections, selectionArgs, null);
        try {
            if (cursor == null) return accountId;

            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                if (TextUtils.isEmpty(mAccountName)) {

                    log("未指定账户，默认返回系统第一个账户ID");
                    accountId = cursor.getLong(cursor.getColumnIndex(CalendarContract.Calendars._ID));
                } else {
                    while (cursor.moveToNext()) {
                        String name = cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME));
                        if (mAccountName.equals(name)) {
                            log("已指定账户，返回指定账户ID");

                            accountId = cursor.getLong(cursor.getColumnIndex(CalendarContract.Calendars._ID));
                        }
                    }

                }
            } else {
                //无账户，添加新账户
                log("系统无账户，添加新账户");

                accountId = addCalendarAccount();
                if (accountId > -1) {
                    log("添加账户成功");
                } else {
                    log("添加账户失败");
                }

            }
        } finally {
            if (cursor != null) cursor.close();
        }

        return accountId;

    }

    /**
     * 添加账户
     *
     * @return account id
     */
    private long addCalendarAccount() {
        TimeZone timeZone = TimeZone.getDefault();

        ContentValues values = new ContentValues();
        values.put(CalendarContract.Calendars.NAME, mCalendarName);
        values.put(CalendarContract.Calendars.ACCOUNT_NAME, mAccountName);
        values.put(CalendarContract.Calendars.ACCOUNT_TYPE, mAccountType);
        values.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, mAccountDisplayName);
        values.put(CalendarContract.Calendars.VISIBLE, 1);
        values.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);
        values.put(CalendarContract.Calendars.SYNC_EVENTS, 1);
        values.put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, timeZone.getID());
        values.put(CalendarContract.Calendars.OWNER_ACCOUNT, mAccountName);
        values.put(CalendarContract.Calendars.CAN_ORGANIZER_RESPOND, 0);
        values.put(CalendarContract.CALLER_IS_SYNCADAPTER, true);
        values.put(CalendarContract.Events.SYNC_EVENTS, 1);

        Uri insertUri = mContext.getContentResolver().insert(CalendarContract.Calendars.CONTENT_URI, values);
        long id = insertUri == null ? -1 : ContentUris.parseId(insertUri);
        return id;
    }

    /**
     * 删除账户
     *
     * @param accountId
     */
    private void deleteCalendarAccount(long accountId) {
      mContext.getContentResolver().delete(CalendarContract.Calendars.CONTENT_URI, CalendarContract.Calendars._ID + "=?", new String[]{String.valueOf(accountId)});
    }

    /**
     * 添加日历事件
     *
     * @return eventId
     */
    public long addCalendarEvent() {
        long eventId = -1;
        long accountId = checkCalendarAccount();
        if (accountId > -1) {


            boolean isExist = queryCalendarEvent(mEventTitle, mEventDescription) > -1;
            log("添加日历事件开始时间：" + mAlarmStartTime + "     结束时间：" + mAlarmStartTime + mAlarmDurationTime * 60 * 1000 + "  " + isExist);

            if (isExist) return eventId;

            ContentValues events = new ContentValues();
            events.put(CalendarContract.Events.CALENDAR_ID, accountId);
            events.put(CalendarContract.Events.TITLE, mEventTitle);
            events.put(CalendarContract.Events.DESCRIPTION, mEventDescription);
            events.put(CalendarContract.Events.DTSTART, mAlarmStartTime);
            events.put(CalendarContract.Events.DTEND, mAlarmStartTime + mAlarmDurationTime * 60 * 1000);
            events.put(CalendarContract.Events.EVENT_TIMEZONE, "Asia");

            events.put(CalendarContract.Events.HAS_ALARM, hasAlarm ? 1 : 0);

            Uri insert = mContext.getContentResolver().insert(CalendarContract.Events.CONTENT_URI, events);
            eventId = insert == null ? -1 : ContentUris.parseId(insert);


            if (hasAlarm && eventId > -1) {
                addOrUpdateCalendarEventAlarm(eventId,false);

            }

        } else {
            log("无法添加账户，添加事件失败");
        }
        log("添加事件" + (eventId > -1 ? "成功" : "失败"));

        return eventId;
    }


    /**
     * 添加或修改事件闹钟提醒
     *
     * @param eventId
     * @return
     */
    private boolean addOrUpdateCalendarEventAlarm(long eventId,boolean isUpdate) {

        ContentValues values = new ContentValues();
        values.put(CalendarContract.Reminders.EVENT_ID, eventId);
        values.put(CalendarContract.Reminders.MINUTES, toMillis());
        values.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);

        if (isUpdate){
//            Uri uri = ContentUris.withAppendedId(CalendarContract.Reminders.CONTENT_URI, eventId);
            mContext.getContentResolver().update(CalendarContract.Reminders.CONTENT_URI, values, CalendarContract.Reminders.EVENT_ID + "=?", new String[]{String.valueOf(eventId)});
            log("更新闹钟 "+eventId);
            return true;
        }
        else
        {
            Uri insert = mContext.getContentResolver().insert(CalendarContract.Reminders.CONTENT_URI, values);
            log("添加闹钟" + (insert == null ? "失败" : "成功"));
            return insert == null;
        }
    }


    /**
     * 删除日历事件
     *
     * @param eventId
     */
    public boolean deleteCalendarEvent(long eventId) {
        if (eventId > -1) {
            mContext.getContentResolver().delete(CalendarContract.Events.CONTENT_URI, CalendarContract.Events._ID + "=?", new String[]{String.valueOf(eventId)});
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * 更新日历事件
     *
     * @param eventId
     */
    public boolean updateCalendarEvent(long eventId) {
        ContentValues valuesEvent = new ContentValues();

        valuesEvent.put(CalendarContract.Events.TITLE, mEventTitle);

        valuesEvent.put(CalendarContract.Events.DESCRIPTION, mEventDescription);
        valuesEvent.put(CalendarContract.Events.HAS_ALARM, hasAlarm ? 1 : 0);
        log("hasAlarm  "+hasAlarm);

        valuesEvent.put(CalendarContract.Events.DTSTART, mAlarmStartTime);

        valuesEvent.put(CalendarContract.Events.DTEND, mAlarmStartTime + mAlarmDurationTime * 60 * 1000);


        Uri uriEvent = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);

        mContext.getContentResolver().update(CalendarContract.Events.CONTENT_URI, valuesEvent,
                CalendarContract.Events._ID+"=?", new String[]{String.valueOf(eventId)});
        log("更新事件 "+eventId);
        return addOrUpdateCalendarEventAlarm(eventId,true);

    }

    /**
     *
     * 判断日历账户中是否已经存在此事件
     * 事件标题
     * 事件开始时间
     * 事件结束时间
     * @return eventId
     */
    public long queryCalendarEvent(String eventTitle, String eventDescription) {
        long eventId = -1;
        if (eventTitle == null || eventDescription == null)
            throw new NullPointerException("event title and description can't be null");
        Cursor cursor = mContext.getContentResolver().query(CalendarContract.Events.CONTENT_URI, null, null, null, null);

        if (cursor == null) return eventId;
        try {
            cursor.moveToFirst();
            while (cursor.moveToNext()) {

                log(
                        cursor.getString(cursor.getColumnIndex(CalendarContract.Events._ID)) + "\n"
                                + cursor.getString(cursor.getColumnIndex(CalendarContract.Events.TITLE)) + "\n"
                                + cursor.getString(cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)) + "\n"
                                + cursor.getString(cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)) + "\n"
                );
                if (eventTitle.equals(cursor.getString(cursor.getColumnIndex(CalendarContract.Events.TITLE)))) {
                    if (eventDescription.equals(cursor.getString(cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)))) {
                        eventId = cursor.getLong(cursor.getColumnIndex(CalendarContract.Events._ID));
                    }
                }
            }
            log("事件" + (eventId > -1 ? "已存在" : "不存在") + eventId);

            return eventId;


        } finally {
            cursor.close();

        }

    }

    /**
     * 将闹钟提前提醒时间转为分钟
     *
     * @return
     */
    private long toMillis() {
        long l = 0;
        if (mAlarmDateType == null) return l;
        log("     switch       "+mAlarmDateType.name()+"     "+mAlarmLeadTime);
        switch (mAlarmDateType) {
            case SECOND:
                l = mAlarmLeadTime / 1000 / 60;
                break;
            case MINUTE:
                l = mAlarmLeadTime;

                break;
            case HOUR:
                l = mAlarmLeadTime * 60;

                break;
            case DAY:
                l = mAlarmLeadTime * 24 * 60;

                break;
            default:
                break;
        }
        log("闹钟将在事件发生 " + l + " 分钟前提醒");
        return l;
    }

    private boolean isLog = true;

    private void log(String log) {
        if (isLog) {
            Log.e(TAG, log);
        }
    }

    public static class Builder {
        private Context mContext;
        //可选  是否闹钟提醒
        private boolean hasAlarm;
        //事件开始时间  时间戳
        private long mAlarmStartTime;
        //事件持续时长 默认10 分钟
        private int mAlarmDurationTime = 10;
        //提前提醒时间
        private long mAlarmLeadTime;
        //提前提醒时间类型 时、分、秒、天
        private CalendarAlarmDateType mAlarmDateType;
        //可选  新建日历名称
        private String mCalendarName;

        //        添加事件的标题
        private String mEventTitle;
        //添加事件的描述
        private String mEventDescription;

        public Builder(Context context) {
            mContext = context;
        }

        public Builder setHasAlarm(boolean hasAlarm) {
            this.hasAlarm = hasAlarm;

            return this;
        }

        public Builder setAlarmStartTime(long alarmStartTime) {
            mAlarmStartTime = alarmStartTime;
            return this;
        }

        public Builder setAlarmDurationTime(int alarmDurationTime) {
            mAlarmDurationTime = alarmDurationTime;
            return this;
        }

        public Builder setAlarmLeadTime(int alarmLeadTime, CalendarAlarmDateType alarmDateType) {
            mAlarmLeadTime = alarmLeadTime;
            mAlarmDateType = alarmDateType;
            return this;
        }


        public Builder setCalendarName(String calendarName) {
            mCalendarName = calendarName;
            return this;
        }

        public Builder setEvent(@NonNull String eventTitle, @NonNull String eventDescription) {
            this.mEventTitle = eventTitle;
            this.mEventDescription = eventDescription;
            return this;
        }


        public CalendarProviderManager build() {

            return new CalendarProviderManager(this);
        }
    }


}
