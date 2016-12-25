package com.example.jashun.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener{

    private Button btn_get_contact;
    private TextView txt_phone_num, txt_phone_name;
    ListView listview_contact;

    class Struct {
        public String iName;
        public String iDesc;

        Struct(String name, String desc) {
            iName = name;
            iDesc = desc;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        btn_get_contact = (Button) findViewById(R.id.btn_get_contact);
        txt_phone_num = (TextView) findViewById(R.id.txt_phone_num);
        txt_phone_name = (TextView) findViewById(R.id.txt_phone_name);

        btn_get_contact.setOnClickListener(this);

        listview_contact = (ListView) findViewById(R.id.listview_contact);
        Struct[] mItems = buildData(30, "Name", "Desc");
        ListAdapter mAdapter =
                new ArrayAdapter<Struct>(this,
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

    private Struct[] buildData(int length, String name, String desc) {
        Struct[] array = new Struct[length];
        for (int i = 0; i < length; i++) {
            array[i] = new Struct(name + ":" + i, desc + "," + i);
        }
        return array;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_get_contact:
                getContactInfo();
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
        Struct clicked = (Struct) adapter.getItem(position);
        Toast.makeText(getApplicationContext(),
                clicked.iName, Toast.LENGTH_SHORT).show();
        // 035396386
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:035396386"));

        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        startActivity(callIntent);

    }
    private void getContactInfo(){
        Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null,null, null);
        while (cursor.moveToNext()) {
            String name =cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            txt_phone_name.setText(name);

            String contact_Id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            Cursor cursor_phone = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contact_Id, null, null);
            while (cursor_phone.moveToNext()) {
                String phNumber = cursor_phone.getString(cursor_phone
                        .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                int PHONE_TYPE =cursor_phone.getInt(cursor_phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                switch (PHONE_TYPE) {
                    case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                        // home number
                        break;
                    case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                        // mobile number
                        txt_phone_num.setText("Mobile:"+phNumber);
                        break;
                    case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                        // work(office) number
                        break;
                }
            }

        }
    }

}
