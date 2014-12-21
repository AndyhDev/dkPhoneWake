package com.dk.phonewake;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dk.phonewake.AddPhoneNumberDlg.Listener;


public class MainActivity extends Activity implements OnClickListener, LoaderManager.LoaderCallbacks<Cursor>{

	private static final int PICK_CONTACT_REQUEST = 123;
	private static final String LIST_KEY = "list_key";

	private String PHONE_NUMBER = Phone.NUMBER;
	private String DISPLAY_NAME = Contacts.DISPLAY_NAME;

	private ListView list;
	private ImageButton fab;
	private Uri data;
	private ArrayList<Contact> contacts;
	private Adapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		loadList();

		adapter = new Adapter();

		list = (ListView) findViewById(R.id.list);
		list.setAdapter(adapter);

		fab = (ImageButton) findViewById(R.id.fab);
		fab.setOnClickListener(this);

		SwipeDismissListViewTouchListener touchListener = new SwipeDismissListViewTouchListener(list, new SwipeDismissListViewTouchListener.DismissCallbacks() {
			@Override
			public boolean canDismiss(int position) {
				return true;
			}

			@Override
			public void onDismiss(ListView listView, int[] reverseSortedPositions) {
				for (int position : reverseSortedPositions) {
					contacts.remove(adapter.getItem(position));
					saveList();
				}
				adapter.notifyDataSetChanged();
			}
		});
		list.setOnTouchListener(touchListener);

		list.setOnScrollListener(touchListener.makeScrollListener());
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if(id == R.id.start_service){
			Intent i = new Intent(this, Service.class);
			startService(i);
		}
		return super.onOptionsItemSelected(item);
	}
	@Override
	public void onClick(View v) {
		int id = v.getId();
		if(id == R.id.fab){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.number_title);
			builder.setMessage(getString(R.string.number_choose));
			builder.setCancelable(true);
			builder.setPositiveButton(R.string.select_contact, new ContactsOnClickListener());
			builder.setNegativeButton(R.string.select_number, new NumberOnClickListener());
			AlertDialog dialog = builder.create();
			dialog.show();
		}
	}

	private void add(String number, String name) {
		if(number.length() > 0){
			number = number.replaceAll("\\s+","");
			for(int i = 0; i < contacts.size(); i++){
				if(contacts.get(i).getNumber().equals(number)){
					Toast.makeText(getApplicationContext(), R.string.number_already, Toast.LENGTH_LONG).show();
					return;
				}
			}
			contacts.add(new Contact(name, number));
			saveList();
			actListView();
		}
	}

	private void loadList(){
		SharedPreferences prefs = getSharedPreferences("list", MODE_PRIVATE);
		String raw = prefs.getString(LIST_KEY, "[]");
		contacts = new ArrayList<Contact>();
		try {
			JSONArray array = new JSONArray(raw);
			for(int i = 0; i < array.length(); i++){
				JSONArray c = array.optJSONArray(i);
				String name = c.optString(0);
				String number = c.optString(1);
				if(name != null && number != null){
					contacts.add(new Contact(name, number));
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void actListView() {
		adapter.notifyDataSetChanged();
	}

	private void saveList(){
		JSONArray array = new JSONArray();
		for(int i = 0; i < contacts.size(); i++){
			JSONArray tmp = new JSONArray();
			tmp.put(contacts.get(i).getName());
			tmp.put(contacts.get(i).getNumber());
			array.put(tmp);
		}
		Editor edit = getSharedPreferences("list", MODE_PRIVATE).edit();
		edit.putString(LIST_KEY, array.toString());
		edit.commit();
	}

	private final class ContactsOnClickListener implements DialogInterface.OnClickListener {
		public void onClick(DialogInterface dialog, int which) {
			Intent pickContactIntent = new Intent( Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI );
			pickContactIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
			startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
		}
	}

	private final class NumberOnClickListener implements DialogInterface.OnClickListener {
		public void onClick(DialogInterface dialog, int which) {
			AddPhoneNumberDlg dlg = new AddPhoneNumberDlg(MainActivity.this);
			dlg.setListener(new Listener() {

				@Override
				public void onCancel() {
					// TODO Auto-generated method stub

				}

				@Override
				public void onAdd(String number, String name) {
					add(number, name);
				}

			});
			dlg.show();
		}
	}
	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent intent ) {
		super.onActivityResult( requestCode, resultCode, intent );
		if(requestCode == PICK_CONTACT_REQUEST) {
			if(resultCode == RESULT_OK) {
				data = intent.getData();
				Log.d("test", data.toString());

				getLoaderManager().restartLoader(PICK_CONTACT_REQUEST, null, this);

			}
		}
	}
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if (id == PICK_CONTACT_REQUEST) {
			String[] projection = { PHONE_NUMBER, DISPLAY_NAME };
			if(data != null){
				return new CursorLoader(this, data, projection, null, null, null);
			}
		}

		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (data == null)
			return;

		if (loader.getId() == PICK_CONTACT_REQUEST) {
			if (cursor.moveToFirst()) {
				String number = cursor.getString(cursor.getColumnIndex(PHONE_NUMBER));
				String name = cursor.getString(cursor.getColumnIndex(DISPLAY_NAME));
				add(number, name);
				data = null;
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {}

	private class ViewHolder{
		public TextView name;
		public TextView number;
	}

	private class Adapter extends BaseAdapter{

		@Override
		public int getCount() {
			return contacts.size();
		}

		@Override
		public Object getItem(int position) {
			return contacts.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@SuppressLint("InflateParams")
		@Override
		public View getView(int position, View v, ViewGroup parent) {
			Contact c = contacts.get(position);
			ViewHolder holder;
			if(v == null){
				v = getLayoutInflater().inflate(R.layout.list_item, null);
				TextView name = (TextView) v.findViewById(R.id.name);
				TextView number = (TextView) v.findViewById(R.id.number);
				holder = new ViewHolder();
				holder.name = name;
				holder.number = number;
				v.setTag(holder);
			}else{
				holder = (ViewHolder) v.getTag();
			}
			holder.name.setText(c.getName());
			holder.number.setText(c.getNumber());

			return v;
		}

	}
}

