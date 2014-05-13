/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.myhoard.app.activities;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.myhoard.app.Managers.UserManager;
import com.myhoard.app.R;
import com.myhoard.app.adapters.NavDrawerListAdapter;
import com.myhoard.app.dialogs.GeneratorDialog;
import com.myhoard.app.dialogs.SynchronizationDialog;
import com.myhoard.app.fragments.CollectionFragment;
import com.myhoard.app.fragments.CollectionsListFragment;
import com.myhoard.app.model.RowItem;
import com.myhoard.app.provider.DataStorage;
import com.myhoard.app.services.SynchronizationService;

import java.util.ArrayList;
import java.util.List;

/*
Created by Rafał Soudani, modified by Tomasz Nosal, Mateusz Czyszkiewicz
*/
public class MainActivity extends BaseActivity implements FragmentManager.OnBackStackChangedListener {

    private static final String TAG = "MainActivity";

    private CollectionsListFragment collectionsListFragment;
    private Menu actionBarMenu;
    private AlertDialog.Builder builder;
    private Intent synchronizationIntent;

    private ActionBarDrawerToggle actionBarDrawerToggle;

    private static final int XOFFSET = 0;
    private static final int YOFFSET = 0;
    private static final String NEWCOLLECTION = "NewCollection";
    private static final String MAIN = "Main";
    private static final String WYLOGOWANO = "Wylogowano";
    private static final String NEWELEMENT = "NewElement";
    private static final String ITEMSLIST = "ItemsList";
    private static final String FRAGMENT = "fragment";
    private static DrawerLayout drawerLayout = null;
    private SynchronizationDialog synchronizationDialog;

    private NavDrawerListAdapter navDrawerListAdapter;

    public static long collectionSelected = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fm = getSupportFragmentManager();

        //Listen for changes in the back stack
        fm.addOnBackStackChangedListener(this);
        shouldDisplayHomeUp();

        setVariables();
        openFragment(savedInstanceState, fm);
        setDrawer();

        builder = new AlertDialog.Builder(MainActivity.this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter("notification"));
        //needItForDebugging();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    private void setDrawer() {

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        final ListView navigationList = (ListView) findViewById(R.id.drawer_list);
        navigationList.setAdapter(navDrawerListAdapter);

        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close);

        drawerLayout.setDrawerListener(actionBarDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        navigationList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int i, final long l) {

                displayView(i);

            }
        });
    }


    private void search() {
        //Set zgodnie z grafikami oparty na drawerze, jesli bedzie zmiana i przeniesienie
        //na action bar to zrobie na actionbarze ;)
        MenuItem miSearch = actionBarMenu.findItem(R.id.action_search);
        if (miSearch != null) {
            miSearch.setVisible(true);
            MenuItemCompat.expandActionView(miSearch);
            MenuItemCompat.setOnActionExpandListener(miSearch, new MenuItemCompat.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem menuItem) {
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                    menuItem.setVisible(false);
                    return true;
                }
            });

            final SearchView svSearch = (SearchView) MenuItemCompat.getActionView(miSearch);
            if (svSearch != null) {
                svSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String s) {
                        if (s.equals("generate")) {
                            GeneratorDialog generatorDialog = new GeneratorDialog();
                            generatorDialog.show(getSupportFragmentManager(), "");
                        }
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String s) {
                        if (s.length() > 20) {
                            svSearch.setQuery(s.substring(0, 20), false);
                        } else if (s.length() >= 2){
                            Bundle args = new Bundle();
                            args.putString(CollectionsListFragment.QUERY, s);
                            collectionsListFragment.fillGridView(args);
                        } else if (s.length() <2){
                            collectionsListFragment.fillGridView(null); // reset listy wyników

                        }
                        return true;
                    }
                });
            }
        }
    }

    private void openFragment(Bundle savedInstanceState, FragmentManager fm) {
        if (savedInstanceState == null) {

            fm.beginTransaction()
                    .add(R.id.container, collectionsListFragment, MAIN)
                    .commit();
        } else {
            fm.beginTransaction()
                    .replace(R.id.container,
                            fm.findFragmentByTag(savedInstanceState.getString(FRAGMENT)))
                    .commit();

        }
    }

    private void setVariables() {
        collectionsListFragment = new CollectionsListFragment();
        List<RowItem> list = preparing_navigationDrawer();
        navDrawerListAdapter = new NavDrawerListAdapter(this, R.layout.drawer_menu_row, list);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        actionBarDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        actionBarDrawerToggle.syncState();
    }

    /*@Override
    protected void onStart() {
        IntentFilter filter = new IntentFilter(ResponseReceiver.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new ResponseReceiver();
        registerReceiver(receiver, filter);
        super.onStart();
    }*/

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(FRAGMENT, getVisibleFragmentTag());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (synchronizationIntent!=null)
            stopService(synchronizationIntent);
    }


    String getVisibleFragmentTag() {
        FragmentManager fragmentManager = MainActivity.this.getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        for (Fragment fragment : fragments) {
            if (fragment != null && fragment.isVisible())
                return fragment.getTag();
        }
        throw new IllegalStateException(getString(R.string.no_fragment));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        //setting sort option unvisible
        //Mplewko: usunę jak ostatecznie zakończę sortowanie
        //menu.findItem(R.id.action_sort).setVisible(false);
        //set search option unvisible
        //menu.findItem(R.id.action_search).setVisible(false);

        actionBarMenu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.action_new_collection:
                item.setTitle(R.string.action_new_collection);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, new CollectionFragment(), NEWCOLLECTION)
                        .addToBackStack(NEWCOLLECTION)
                        .commit();

                break;
            case R.id.action_login:
                UserManager userManager = UserManager.getInstance();
                if (!userManager.isLoggedIn()) {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                } else {
                    userManager.logout();
                    Toast toast = Toast.makeText(getBaseContext(), WYLOGOWANO,
                            Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, XOFFSET, YOFFSET);
                    toast.show();
                }
                break;
            case R.id.action_generate:
                GeneratorDialog generatorDialog = new GeneratorDialog();
                generatorDialog.show(getSupportFragmentManager(), "");
                break;
            //MPLewko: usunę jak ostatecznie zakończę sortowanie
            /*case R.id.action_sort:

                if (!getVisibleFragmentTag().equals(NEWCOLLECTION) &&
                        !getVisibleFragmentTag().equals(ITEMSLIST) &&
                        !getVisibleFragmentTag().equals(NEWELEMNT)) {
                    //TODO collection list custom sort

                } else if (getVisibleFragmentTag().equals(ITEMSLIST)) {
                    // items list sort order change
                    ItemsListFragment fragment = (ItemsListFragment) getSupportFragmentManager().
                            findFragmentByTag(ITEMSLIST);
                    fragment.itemsSortOrderChange(item);
                }
                break;*/
            case R.id.action_download:
                Intent synchronize = new Intent(this, SynchronizationService.class);
                synchronize.putExtra("option","download");
                startService(synchronize);
                break;
            case R.id.action_upload:
                synchronizationIntent = new Intent(this, SynchronizationService.class);
                synchronizationIntent.putExtra("option","upload");
                startService(synchronizationIntent);
                break;
            case R.id.action_synchronize:
                synchronizationIntent = new Intent(MainActivity.this, SynchronizationService.class);
                synchronizationIntent.putExtra("option","synchronization");
                startService(synchronizationIntent);
                synchronizationDialog = new SynchronizationDialog(synchronizationIntent, getApplicationContext());
                synchronizationDialog.show(getSupportFragmentManager(), "");
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }


    @Override
    public void onBackStackChanged() {
        //    shouldDisplayHomeUp();
    }

    void shouldDisplayHomeUp() {
        //Enable Up button only  if there are entries in the back stack
        //  boolean canBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
        //  getSupportActionBar().setDisplayHomeAsUpEnabled(canBack);
    }

    @Override
    public boolean onSupportNavigateUp() {
        //This method is called when the up button is pressed. Just the pop back stack.
        //   getSupportFragmentManager().popBackStack();
        return true;
    }

    private void displayView(int position)
    {
        
        switch (position) {
            //search option
            case 0:
                if (getVisibleFragmentTag().equals(MAIN)) {
                    search();
                }
                break;

              //new collection
            case 1:
                if (!getVisibleFragmentTag().equals(NEWCOLLECTION) &&
                        !getVisibleFragmentTag().equals(ITEMSLIST) &&
                        !getVisibleFragmentTag().equals(NEWELEMENT)) {
                    //item.setTitle(R.string.action_new_collection);//TODO correct
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.container, new CollectionFragment(), NEWCOLLECTION)
                            .addToBackStack(NEWCOLLECTION)
                            .commit();
                } else if (getVisibleFragmentTag().equals(ITEMSLIST)) {
                    //item.setTitle(R.string.action_new_element);//TODO correct
                    Intent in = new Intent(this,ElementActivity.class);
                    in.putExtra("categoryId",collectionSelected);
                    startActivity(in);
                }
                break;
            //Friends
            case 2:
                Toast.makeText(getBaseContext(),getString(R.string.not_implement_yet),Toast.LENGTH_SHORT).show();
                break;
            //profile
            case 3:
                Toast.makeText(getBaseContext(),getString(R.string.not_implement_yet),Toast.LENGTH_SHORT).show();
                break;
            default:
                break;

        }
        drawerLayout.closeDrawers();
    }
    List<RowItem> preparing_navigationDrawer() {

            String[] drawerListItems = getResources().getStringArray(R.array.drawer_menu);
            int[] images = {R.drawable.szukaj, R.drawable.kolekcje, R.drawable.znajomi, R.drawable.profilpng};

        List<RowItem> list = new ArrayList<RowItem>();
        for (int i = 0; i < drawerListItems.length; i++) {
            RowItem item = new RowItem(drawerListItems[i], images[i]);
            list.add(item);
        }
        return list;
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {

        List <String> errorSynchronizationList = new ArrayList<String>();

        @Override
        public void onReceive(Context context, Intent intent) {
            String stringExtra = intent.getStringExtra("result");
            if (stringExtra != null) {
                if (stringExtra.equals("synchronized")){
                    //progress.dismiss();
                    synchronizationDialog.dismiss();
                    showErrors();
                }
            }
            stringExtra = intent.getStringExtra("error");
            if (stringExtra != null) {
                errorSynchronizationList.add(stringExtra);
            }
        }

        private void showErrors() {
            if (errorSynchronizationList.size()>0) {
                builder.setMessage(errorSynchronizationList.toString())
                        .setTitle("Errors")
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                builder.create();
                builder.show();
            }
        }
    };

    private void needItForDebugging() {
        //Kod potrzebny do debugowania, prosze go nie uswuac
        Cursor cursor = getContentResolver().query(DataStorage.Media.CONTENT_URI, null, null, null, null);
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            Log.d(TAG, "id "+cursor.getString(cursor.getColumnIndex(DataStorage.Media._ID)));
            Log.d(TAG, cursor.getString(cursor.getColumnIndex(DataStorage.Media.FILE_NAME)));
        }
        Log.d(TAG,"<-----KOLKECJE----->");
        cursor = getContentResolver().query(DataStorage.Collections.CONTENT_URI, null, null, null, null);
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            Log.d(TAG, "id "+cursor.getString(cursor.getColumnIndex(DataStorage.Collections._ID)));
            Log.d(TAG, "name "+cursor.getString(cursor.getColumnIndex(DataStorage.Collections.NAME)));
            Log.d(TAG, "iloscItemow "+cursor.getString(cursor.getColumnIndex(DataStorage.Collections.ITEMS_NUMBER)));

            Log.d(TAG, ((Boolean)(cursor.getInt(cursor.getColumnIndex(DataStorage.Collections.SYNCHRONIZED))>0)).toString());
            Log.d(TAG, ((Boolean)(cursor.getInt(cursor.getColumnIndex(DataStorage.Collections.DELETED))>0)).toString());
            Log.d(TAG, (cursor.getString(cursor.getColumnIndex(DataStorage.Collections.MODIFIED_DATE))).toString());
            Log.d(TAG, ((Integer)cursor.getInt(cursor.getColumnIndex(DataStorage.Collections.TYPE))).toString());
            Log.d(TAG, cursor.getString(cursor.getColumnIndex(DataStorage.Collections.TYPE)));
            Log.d(TAG, DataStorage.TypeOfCollection.values()[cursor.getInt(cursor.getColumnIndex(DataStorage.Collections.TYPE))].toString());
        }

        Log.d(TAG,"<--------ITEMY-------->");
        //String selection = String.format(DataStorage.Items.TABLE_NAME + "." + DataStorage.Items.ID_SERVER
        String[] projection = new String[] { DataStorage.Items.TABLE_NAME + "." + DataStorage.Items.ID_SERVER,
                DataStorage.Items.TABLE_NAME + "." + DataStorage.Items._ID,
                DataStorage.Items.TABLE_NAME + "." + DataStorage.Items.ID_COLLECTION,
                DataStorage.Items.TABLE_NAME + "." + DataStorage.Items.SYNCHRONIZED,
                //DataStorage.Media.TABLE_NAME + "." + DataStorage.Media.FILE_NAME,
        };
        cursor = getContentResolver().query(DataStorage.Items.CONTENT_URI, projection, null, null, null);
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            Log.d(TAG,"<-ITEM->");
            Log.d(TAG, "id "+cursor.getString(cursor.getColumnIndex(DataStorage.Items._ID)));
            Log.d(TAG, "id_server "+cursor.getString(cursor.getColumnIndex(DataStorage.Items.ID_SERVER)));
            Log.d(TAG, "id_collection "+cursor.getString(cursor.getColumnIndex(DataStorage.Items.ID_COLLECTION)));
            Log.d(TAG, ((Boolean)(cursor.getInt(cursor.getColumnIndex(DataStorage.Items.SYNCHRONIZED))>0)).toString());
            //Log.d(TAG, "file name "+cursor.getString(cursor.getColumnIndex(DataStorage.Media.FILE_NAME)));
        }

        cursor = getContentResolver().query(DataStorage.Media.CONTENT_URI, null, null, null, null);
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            Log.d(TAG, "<-Media->");
            Log.d(TAG, "id "+cursor.getString(cursor.getColumnIndex(DataStorage.Media._ID)));
            Log.d(TAG, "id_server "+cursor.getString(cursor.getColumnIndex(DataStorage.Media.ID_SERVER)));
            Log.d(TAG, ((Boolean)(cursor.getInt(cursor.getColumnIndex(DataStorage.Media.SYNCHRONIZED))>0)).toString());
        }
    }
}
