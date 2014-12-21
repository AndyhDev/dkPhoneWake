package com.dk.phonewake;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class AddPhoneNumberDlg extends Dialog implements android.view.View.OnClickListener{

	private EditText number;
	private EditText name;
	private Button cancel;
	private Button add;
	private Listener listener;
	
	public static abstract interface Listener{
		public void onAdd(String number, String name);
		public void onCancel();
	}
	
	public AddPhoneNumberDlg(Context context) {
		super(context);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_dlg);
		
		setTitle(getContext().getString(R.string.add_dlg_title));
		
		number = (EditText) findViewById(R.id.number);
		name = (EditText) findViewById(R.id.name);
		
		cancel = (Button) findViewById(R.id.cancel);
		cancel.setOnClickListener(this);
		add = (Button) findViewById(R.id.add);
		add.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		if(id == R.id.cancel){
			dismiss();
			if(listener != null){
				listener.onCancel();
			}
		}else if(id == R.id.add){
			String name = this.name.getText().toString();
			String number = this.number.getText().toString();
			dismiss();
			if(listener != null){
				listener.onAdd(number, name);
			}
		}
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}
	
	
}
