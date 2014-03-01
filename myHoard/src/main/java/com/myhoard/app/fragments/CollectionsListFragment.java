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

package com.myhoard.app.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.myhoard.app.R;
import com.myhoard.app.images.ImageAdapter;

/**
 * Created by Rafał Soudani on 20/02/2014
 */
public class CollectionsListFragment extends Fragment {
    private static GridView gridView;
    private static Context context;
    private static final int DELETE_ID = Menu.FIRST + 1;
    private static final int EDIT_ID = Menu.FIRST + 2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getActivity();
        return inflater.inflate(R.layout.fragment_collections_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        gridView = (GridView) view.findViewById(R.id.gridview);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //TODO: show items in collection
                /* Testing for collection element - author Sebastian Peryt
                // Create new fragment and transaction
                Fragment newFragment = new ElementFragment();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();

                // Add arguments to opened fragment element
                Bundle b = new Bundle();
                // put name
                b.putString("name","NAME");
                // put description
                b.putString("description","DESCRIPTION");
                newFragment.setArguments(b);

                // Replace whatever is in the fragment_container view with this fragment,
                // and add the transaction to the back stack
                transaction.replace(R.id.container, newFragment);
                transaction.addToBackStack(null);

                // Commit the transaction
                transaction.commit();
                */
            }
        });
        registerForContextMenu(gridView);
        gridView.setEmptyView(view.findViewById(R.id.tvEmpty));
        fillGridView();
    }

    public static void fillGridView() {
        gridView.setAdapter(new ImageAdapter(context));
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, EDIT_ID, 0, R.string.menu_edit);
        //menu.add(0, DELETE_ID, 1, R.string.menu_delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case DELETE_ID:
                //TODO: Delete collection
                return true;
            case EDIT_ID:
                //TODO: Edit collection
                AdapterView.AdapterContextMenuInfo info =
                        (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                Bundle args = new Bundle();
                if (info != null) {
                    args.putLong("id", info.id);
                }
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, new CollectionFragment(args), "EditCollection")
                        .addToBackStack("EditCollection")
                        .commit();
                return true;
        }
        return super.onContextItemSelected(item);
    }


}