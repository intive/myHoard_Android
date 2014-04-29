package com.myhoard.app.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.myhoard.app.R;
import com.myhoard.app.element.ElementReadFragment;
import com.myhoard.app.element.ElementAddEditFragment;
import com.myhoard.app.gps.GPSProvider;
import com.myhoard.app.model.Item;
import com.myhoard.app.provider.DataStorage;

/**
 * Created by Sebastian Peryt on 27.04.14.
 */

/**
 * README
 * Ta klasa jak i wszystkie w tym pakiecie sa jeszcze przebudowywane, dlatego sa tam rózne hard coded wartości.
 * Zostanie to niedlugo naprawione.
 */
public class ElementActivity extends ActionBarActivity {

    private Item element;
    private long elementId;
    private long categoryId = -1;
    private AsyncElementRead asyncElementRead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_element);

        // API 14+ hack
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        getBaseContext().bindService(
                new Intent(this, GPSProvider.class), mConnection,
                Context.BIND_AUTO_CREATE);

        asyncElementRead = new AsyncElementRead();

        if(getIntent().hasExtra("categoryId")) {
            categoryId = getIntent().getLongExtra("categoryId", 0);
            displayFragment(1);
        } else if(getIntent().hasExtra("elementId")) {
            elementId = getIntent().getLongExtra("elementId", 0);
            asyncElementRead.execute(elementId);
            displayFragment(0);
        } else {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.new_collection, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_accept:
                displayFragment(1);
        }
        return super.onOptionsItemSelected(item);
    }

    private void displayFragment(int position) {
        Fragment fragment = null;
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager
                .beginTransaction();
        Bundle bundle = new Bundle();
        switch (position) {
            case 0:
                fragment = fragmentManager.findFragmentByTag(String
                        .valueOf(position));
                if (fragment == null) {
                    fragment = new ElementReadFragment();
                    bundle.putLong("elementId", elementId);
                    fragment.setArguments(bundle);
                }
                break;
            case 1:
                fragment = fragmentManager.findFragmentByTag(String
                        .valueOf(position));
                if (fragment == null) {
                    fragment = new ElementAddEditFragment();
                    if(categoryId == -1){
                        bundle.putParcelable("element",element);
                    } else {
                        bundle.putLong("categoryId", categoryId);
                    }
                    fragment.setArguments(bundle);
                }
                break;
            default:
                break;
        }

        if (fragment != null) {
            fragmentTransaction.replace(R.id.frame_container, fragment,
                    String.valueOf(position));
            if(!(fragment instanceof ElementReadFragment)) {
                fragmentTransaction.addToBackStack(null);
            }
            fragmentTransaction.commit();
        }
    }

    private class AsyncElementRead extends AsyncTask<Long, Integer, Item> {

        @Override
        protected Item doInBackground(Long... params) {
            String[] projection = {DataStorage.Items.NAME,
                    DataStorage.Items.DESCRIPTION,
                    DataStorage.Items.ID_COLLECTION};
            String[] selection = {String.valueOf(elementId)};
            Cursor cursorItems = getContentResolver().query(DataStorage.Items.CONTENT_URI, projection, DataStorage.Items.TABLE_NAME + "." + DataStorage.Items._ID + " =? ", selection, DataStorage.Items.TABLE_NAME + "." + DataStorage.Items._ID + " DESC");

            cursorItems.moveToFirst();
            Item element = new Item();
            String name = cursorItems.getString(cursorItems.getColumnIndex(DataStorage.Items.NAME));
            String description = cursorItems.getString(cursorItems.getColumnIndex(DataStorage.Items.DESCRIPTION));
            int collection = cursorItems.getInt(cursorItems.getColumnIndex(DataStorage.Items.ID_COLLECTION));
            element.setId(String.valueOf(elementId));
            element.setCollection(String.valueOf(collection));
            element.setName(name);
            element.setDescription(description);
            return element;
        }

        @Override
        protected void onPostExecute(Item item) {
            element = item;
        }
    }

    /*
     * Część kodu odpowiedzialna za GPS
     */

    GPSProvider mService;
    boolean mBound = false;
    /*
     * Część kodu odpowiedzialna za binder
     * (http://developer.android.com/guide/components/bound-services.html)
     */
    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Przywiazano do uslugi i rzutowano IBinder
            GPSProvider.LocalGPSBinder binder = (GPSProvider.LocalGPSBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    /*
     * broadcast reciver dzięki któremu istnieje połączenie z uslugą GPS
     */
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updatePosition(intent);
            Bundle b = intent.getExtras();
			/*
			 * AWA:FIXME: Hardcoded value Umiesc w private final static String,
			 * int, etc.... lub w strings.xml lub innym *.xml
			 */
            if (b != null && !b.getBoolean("GPS")) {
//                tvElementPosition.setText("brak");
//                tvElementPosition.setTextColor(Color.RED);
            }
        }
    };

	/*
	 * KONIEC - Część kodu odpowiedzialna za binder
	 */

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter(
                GPSProvider.BROADCAST_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onDestroy() {
        getBaseContext().unbindService(mConnection);
        super.onDestroy();
    }

    private void updatePosition(Intent intent) {
        Log.e("TAG", "In");
        if (intent == null) {
            Log.e("TAG", "error");
            return;
        }
        Log.e("TAG", "OK");
        Bundle b = intent.getExtras();
        double lat = b.getDouble(GPSProvider.POS_LAT);
        double lon = b.getDouble(GPSProvider.POS_LON);

//        tvElementPosition.setText(pos2str(lat, lon));
//        tvElementPosition.setTextColor(Color.GREEN);
    }

    private String pos2str(double lat, double lon) {
        return lat + ":" + lon;
    }
	/*
	 * KONIEC - Część kodu odpowiedzialna za GPS
	 */
}