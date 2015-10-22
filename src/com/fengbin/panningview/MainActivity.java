package com.fengbin.panningview;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


import android.os.Bundle;
import android.os.Looper;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {

	ImageView img_bg;
	EditText editTextUname;
	EditText editTextUpwd;
	ImageButton imgbtnUname;
	ImageButton imgbtnUpwd;
	Button btnUlogin;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		final PanningView panningView = (PanningView) findViewById(R.id.img_load_bg);
		panningView.startPanning();
		init();
//		btnUlogin.setFocusable(true);
//		btnUlogin.setFocusableInTouchMode(true);
//		btnUlogin.requestFocus();
		setListeners();
		panningView.stopPanning();
	}

	public void init() {
		img_bg = (ImageView) findViewById(R.id.img_load_bg);
		editTextUname = (EditText) findViewById(R.id.editTextUName);
		editTextUpwd = (EditText) findViewById(R.id.editTextPwd);
		imgbtnUname = (ImageButton) findViewById(R.id.imagetBtnUname);
		imgbtnUpwd = (ImageButton) findViewById(R.id.imageBtnPwd);
		btnUlogin = (Button) findViewById(R.id.buttonLogin);
	}

	public void setListeners() {
		btnUlogin.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				
				final String nameStr = editTextUname.getText().toString();
				final String pwdStr = editTextUpwd.getText().toString();
				new Thread() {
					public void run() {		
					}
				}.start();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
