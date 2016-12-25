package com.example.jashun.jscall;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {
    private LinkedList<MyContact> myContactList;
    ListView listview_contact;
    private Button btn_hangup;

    class ItemStruct {
        public String iName;
        public String iDesc;

        ItemStruct(String name, String desc) {
            iName = name;
            iDesc = desc;
        }
    }
    public class MyPhoneStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            switch (state) {
                //電話狀態是閒置的
                case TelephonyManager.CALL_STATE_IDLE:
                    btn_hangup.setText("歡迎使用");
                    btn_hangup.setTextColor(Color.BLUE);
                    btn_hangup.setEnabled(false);
                    break;
                //電話狀態是接起的
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    btn_hangup.setText("結束通話");
                    btn_hangup.setTextColor(Color.RED);
                    btn_hangup.setEnabled(true);
                    break;
                //電話狀態是響起的
                case TelephonyManager.CALL_STATE_RINGING:
                    btn_hangup.setText("結束通話");
                    btn_hangup.setTextColor(Color.RED);
                    btn_hangup.setEnabled(true);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getContactInfo();
        prioritizeList();
        initView();

        //電話狀態的Listener
        MyPhoneStateListener myPhoneStateListener = new MyPhoneStateListener();
        //取得TelephonyManager
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //將電話狀態的Listener加到取得TelephonyManager
        telephonyManager.listen(myPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    protected void onDestroy() {
        endCall();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        endCall();
        super.onPause();
    }

    private void initView() {
        btn_hangup = (Button) findViewById(R.id.btn_hangup);
        btn_hangup.setEnabled(false);
        btn_hangup.setOnClickListener(this);

        listview_contact = (ListView) findViewById(R.id.listview_contact);

        ItemStruct[] mItems = buildData();
        ListAdapter mAdapter =
                new ArrayAdapter<ItemStruct>(this,
                        android.R.layout.simple_list_item_2,
                        android.R.id.text1,
                        mItems) {
                    @Override
                    public View getView(int pos, View convert, ViewGroup group) {
                        View v = super.getView(pos, convert, group);
                        TextView t1 = (TextView) v.findViewById(android.R.id.text1);
                        TextView t2 = (TextView) v.findViewById(android.R.id.text2);
                        t1.setText(getItem(pos).iName);
                        t1.setTextSize(50);
                        t2.setText(getItem(pos).iDesc);
                        t2.setTextSize(10);
                        return v;
                    }
                };
        listview_contact.setAdapter(mAdapter);
        listview_contact.setOnItemClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_hangup:
                endCall();
                break;
            default:
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView,
                            View view,
                            int position,
                            long l) {

        Adapter adapter = adapterView.getAdapter();
        ItemStruct clicked = (ItemStruct) adapter.getItem(position);
        Toast.makeText(getApplicationContext(),
                clicked.iName, Toast.LENGTH_SHORT).show();

        // Perform Dialing ....
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:"+clicked.iDesc)); // We saved phone number in clicked.iDesc

        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        startActivity(callIntent);

    }

    private void endCall(){
        // required permission <uses-permission android:name="android.permission.CALL_PHONE"/>
        try {
            //String serviceManagerName = "android.os.IServiceManager";
            String serviceManagerName = "android.os.ServiceManager";
            String serviceManagerNativeName = "android.os.ServiceManagerNative";
            String telephonyName = "com.android.internal.telephony.ITelephony";

            Class telephonyClass;
            Class telephonyStubClass;
            Class serviceManagerClass;
            Class serviceManagerStubClass;
            Class serviceManagerNativeClass;
            Class serviceManagerNativeStubClass;

            Method telephonyCall;
            Method telephonyEndCall;
            Method telephonyAnswerCall;
            Method getDefault;

            Method[] temps;
            Constructor[] serviceManagerConstructor;

            // Method getService;
            Object telephonyObject;
            Object serviceManagerObject;

            telephonyClass = Class.forName(telephonyName);
            telephonyStubClass = telephonyClass.getClasses()[0];
            serviceManagerClass = Class.forName(serviceManagerName);
            serviceManagerNativeClass = Class.forName(serviceManagerNativeName);

            Method getService = // getDefaults[29];
                    serviceManagerClass.getMethod("getService", String.class);

            Method tempInterfaceMethod = serviceManagerNativeClass.getMethod(
                    "asInterface", IBinder.class);

            Binder tmpBinder = new Binder();
            tmpBinder.attachInterface(null, "fake");

            serviceManagerObject = tempInterfaceMethod.invoke(null, tmpBinder);
            IBinder retbinder = (IBinder) getService.invoke(serviceManagerObject, "phone");
            Method serviceMethod = telephonyStubClass.getMethod("asInterface", IBinder.class);

            telephonyObject = serviceMethod.invoke(null, retbinder);
            //telephonyCall = telephonyClass.getMethod("call", String.class);
            telephonyEndCall = telephonyClass.getMethod("endCall");
            //telephonyAnswerCall = telephonyClass.getMethod("answerRingingCall");

            telephonyEndCall.invoke(telephonyObject);

        } catch (Exception e) {
            e.printStackTrace();
            /**
             Log.error(DialerActivity.this,
             "FATAL ERROR: could not connect to telephony subsystem");
             Log.error(DialerActivity.this, "Exception object: " + e);
             **/
        }
    }

    private ItemStruct[] buildData() {
        int length = myContactList.size();

        ItemStruct[] array = new ItemStruct[length];
        for (int i = 0; i < length; i++) {
            MyContact myContact = myContactList.get(i);
            array[i] = new ItemStruct(myContact.getContactName(), myContact.getPhoneNum());
        }
        return array;
    }

    private int getDesignedPriFromString(String inputString){
        String regularExpression = "([0-9]+)";
        Pattern pattern = Pattern.compile(regularExpression, Pattern.CASE_INSENSITIVE);
        int pri = 10000;
        Matcher matcher = pattern.matcher(inputString);
        if (matcher.find()){
            pri = parseInt(matcher.group(1));
        }
        return pri;
    }

    private void prioritizeList(){
        LinkedList<MyContact> myList = myContactList;
        MyContact tmpEntity;

        int totalCnt = myList.size();
        int currentPos = 0;
        int ii;

        for (currentPos=0;currentPos<totalCnt;currentPos++){
            int pri = myList.get(currentPos).getPri();
            for (ii=0;ii<currentPos;ii++){
                int tmpPri = myList.get(ii).getPri();
                if (pri<tmpPri){
                    tmpEntity = myList.get(currentPos);
                    myList.remove(currentPos);
                    myList.add(ii, tmpEntity);
                    break;
                }
            }
        }
    }


    private void getContactInfo(){
        myContactList = new LinkedList<MyContact>();

        Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null,null, null);
        while (cursor.moveToNext()) {
            MyContact myContact = new MyContact();

            String name =cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            myContact.setContactName(name);
            myContact.setPri(getDesignedPriFromString(name));

            String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            myContact.setPhoneNum(phoneNumber);

            myContactList.add(myContact);


            /**
                    String contact_Id = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                    Cursor cursor_phone = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contact_Id, null, null);
                    while (cursor_phone.moveToNext()) {
                        String phNumber = cursor_phone.getString(cursor_phone
                                .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                        int PHONE_TYPE = cursor_phone.getInt(cursor_phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                        switch (PHONE_TYPE) {
                            case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                                // home number
                                break;
                            case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                                // mobile number
                                break;
                            case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                                // work(office) number
                                break;
                        }

                        if (phNumber.length() >= 9) {
                            myContact.setPhoneNum(phNumber);
                        }
                    }
                    **/
        }
    }

}
