/*  
 * Copyright 2012 Andy Botting <andy@andybotting.com>
 *  
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * This file is distributed in the hope that it will be useful, but  
 * WITHOUT ANY WARRANTY; without even the implied warranty of  
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU  
 * General Public License for more details.  
 *  
 * You should have received a copy of the GNU General Public License  
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  
 *  
 * This file incorporates work covered by the following copyright and  
 * permission notice:
 * 
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andybotting.tramhunter.activity;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.MenuItem;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.objects.Destination;
import com.andybotting.tramhunter.objects.Stop;

public class StopsListActivity extends SherlockListActivity {

	private final static int CONTEXT_MENU_VIEW_STOP = 0;

	private ListView mListView;
	private StopsListAdapter mListAdapter;
	private List<Stop> mStops;
	private TramHunterDB mDB;
	private Destination mDestination;

	/**
	 * On Create
	 */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		long destinationId = -1;
		String searchQuery = null;

		setContentView(R.layout.stops_list);

		// Set up the Action Bar
		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);

		mListView = (ListView) getListView();

		mDB = new TramHunterDB();

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			destinationId = extras.getLong("destinationId");
			searchQuery = extras.getString("search_query");
		}

		// Are we looking for stops for a route, or fav stops?
		if (searchQuery != null) {
			final CharSequence title = getString(R.string.search_results, searchQuery);
			actionBar.setTitle(title);
			displaySearchStops(searchQuery);
		} else if (destinationId != -1) {
			Log.d("Testing", "Getting destination: " + destinationId);
			mDestination = mDB.getDestination(destinationId);
			String title = "Route " + mDestination.getRouteNumber() + " to " + mDestination.getDestination();
			actionBar.setTitle(title);
			displayStopsForDestination(destinationId);
		}

	}

	/**
	 * Close the DB connection if this activity finishes
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDB.close();
	}

	/**
	 * Display the list of stops for a search
	 */
	public void displaySearchStops(String search) {
		mStops = mDB.getStopsForSearch(search);
		displayStops();
	}

	/**
	 * Display the list of stops for a destination
	 */
	public void displayStopsForDestination(long destinationId) {
		mStops = mDB.getStopsForDestination(destinationId);

		// Let's remove any Terminus stops, because we can't get times for them
		for (int i = 0; i < mStops.size(); i++) {
			Stop stop = mStops.get(i);
			if (stop.getTramTrackerID() >= 8000) {
				mStops.remove(i);
			}
		}

		displayStops();
	}

	/**
	 * Display the list of stops
	 */
	public void displayStops() {

		mListView.setOnItemClickListener(mListView_OnItemClickListener);
		mListView.setOnCreateContextMenuListener(mListView_OnCreateContextMenuListener);
		mListAdapter = new StopsListAdapter();
		setListAdapter(mListAdapter);
	}

	/**
	 * Start the activity to view a stop
	 * 
	 * @param stop
	 */
	private void viewStop(Stop stop) {
		int tramTrackerId = stop.getTramTrackerID();

		Bundle bundle = new Bundle();
		bundle.putInt("tramTrackerId", tramTrackerId);
		Intent intent = new Intent(StopsListActivity.this, StopDetailsActivity.class);
		intent.putExtras(bundle);

		startActivityForResult(intent, 1);
	}

	/**
	 * List item click action
	 */
	private OnItemClickListener mListView_OnItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> adapterView, View row, int position, long id) {
			Stop stop = mStops.get(position);
			viewStop(stop);
		}
	};

	/**
	 * Create the context menu
	 */
	private OnCreateContextMenuListener mListView_OnCreateContextMenuListener = new OnCreateContextMenuListener() {
		public void onCreateContextMenu(ContextMenu contextMenu, View v, ContextMenu.ContextMenuInfo menuInfo) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			Stop stop = mStops.get(info.position);
			contextMenu.setHeaderTitle(stop.getStopName());
			contextMenu.add(0, CONTEXT_MENU_VIEW_STOP, 0, R.string.title_view_stop);
		}
	};

	/**
	 * Action a context menu item
	 */
	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Stop stop = mStops.get(info.position);

		switch (item.getItemId())
			{

			case CONTEXT_MENU_VIEW_STOP:
				viewStop(stop);
				return true;
			}

		return super.onContextItemSelected(item);
	}

	/**
	 * Menu actions
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId())
			{

			case android.R.id.home:
				finish();
				return true;
			}

		return false;
	}

	/**
	 * Stops list adapter
	 */
	private class StopsListAdapter extends BaseAdapter {

		public int getCount() {
			return mStops.size();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View pv;
			if (convertView == null) {
				LayoutInflater inflater = getLayoutInflater();
				pv = inflater.inflate(R.layout.stops_list_row, parent, false);
			} else {
				pv = convertView;
			}

			Stop thisStop = (Stop) mStops.get(position);

			String stopName = thisStop.getPrimaryName();
			String stopDetails = "Stop " + thisStop.getFlagStopNumber();
			// If the stop has a secondary name, add it
			if (thisStop.getSecondaryName() != null) {
				stopDetails += ": " + thisStop.getSecondaryName();
			}

			stopDetails += " - " + thisStop.getCityDirection();
			stopDetails += " (" + thisStop.getTramTrackerID() + ")";

			((TextView) pv.findViewById(R.id.stopNameTextView)).setText(stopName);
			((TextView) pv.findViewById(R.id.stopDetailsTextView)).setText(stopDetails);

			((TextView) pv.findViewById(R.id.stopRoutesTextView)).setText(thisStop.getRoutesString());

			return pv;
		}

	}

}