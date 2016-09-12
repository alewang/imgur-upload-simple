package com.alex.imgur_upload;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
	static final int GET_IMAGE_REQUEST_CODE = 0;

	private static final String IMGUR_CLIENT_ID_KEY = "com.alex.imgur_upload.IMGUR_CLIENT_ID";

	private boolean isActivityRunning;

	private Uri selectedImage;
	private View imageStatusSection;
	private TextView imgurLinkText;

	private String imgurLink;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		imageStatusSection = findViewById(R.id.image_status_section);
		imgurLinkText = (TextView) findViewById(R.id.imgur_link);
		isActivityRunning = false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		isActivityRunning = true;
		if(selectedImage != null) {
			imageStatusSection.setVisibility(View.VISIBLE);
		}
		showImgurLink();
	}

	@Override
	protected void onPause() {
		super.onPause();
		isActivityRunning = false;
	}

	public void pickImage(View v) {
		Intent pickImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
		pickImageIntent.setType("image/*");
		pickImageIntent.addCategory(Intent.CATEGORY_OPENABLE);
		startActivityForResult(pickImageIntent, GET_IMAGE_REQUEST_CODE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode != GET_IMAGE_REQUEST_CODE || data == null) {
			return;
		}

		selectedImage = data.getData();
		if(resultCode != Activity.RESULT_OK || selectedImage == null) {
			Toast.makeText(this, R.string.image_not_selected, Toast.LENGTH_LONG).show();
			return;
		}
	}

	public void uploadImage(View v) {
		if(selectedImage == null) {
			Toast.makeText(this, R.string.image_not_selected, Toast.LENGTH_LONG).show();
			return;
		}

		new ImageUploader().execute(selectedImage);
	}

	private void showImgurLink() {
		if(!isActivityRunning) {
			return;
		}

		if(TextUtils.isEmpty(imgurLink)) {
			imgurLinkText.setVisibility(View.GONE);
		}

		imgurLinkText.setText(imgurLink);
		final String imgLink = imgurLink;
		imgurLinkText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent viewLink = new Intent(Intent.ACTION_VIEW, Uri.parse(imgLink));
				startActivity(viewLink);
			}
		});
		imgurLinkText.setVisibility(View.VISIBLE);
	}

	private class ImageUploader extends AsyncTask<Uri, Integer, JSONObject> {
		@Override
		protected JSONObject doInBackground(Uri... uris) {
			if(uris == null || uris.length == 0) {
				return null;
			}

			Uri oneUri = uris[0];

			String imgurEndpoint = "https://api.imgur.com/3/image";
			HttpURLConnection conn = null;
			OutputStream toImgur = null;
			InputStream fromImage = null;
			Reader fromImgur = null;
			JSONObject response = null;
			try {
				conn = (HttpURLConnection) new URL(imgurEndpoint).openConnection();

				PackageInfo packages = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
				Bundle metaData = packages.applicationInfo.metaData;
				String clientId = metaData.getString(IMGUR_CLIENT_ID_KEY);
				conn.addRequestProperty("Authorization", "Client-ID " + clientId);

				conn.setRequestMethod("POST");
				conn.setDoOutput(true);

				toImgur = new BufferedOutputStream(conn.getOutputStream());
				fromImage = getContentResolver().openInputStream(oneUri);
				if(fromImage != null) {
					fromImage = new BufferedInputStream(fromImage);

					byte[] buffer = new byte[512];
					int numRead;
					while((numRead = fromImage.read(buffer)) >= 0) {
						toImgur.write(buffer, 0, numRead);
					}
					toImgur.flush();
					conn.connect();
					fromImgur = new InputStreamReader(new BufferedInputStream(conn.getInputStream()), StandardCharsets.UTF_8);
					char[] cbuffer = new char[512];
					StringBuilder sb = new StringBuilder();
					while((numRead = fromImgur.read(cbuffer)) >= 0) {
						sb.append(cbuffer, 0, numRead);
					}
					fromImgur.close();
					response = new JSONObject(sb.toString());
				}
			} catch(IOException|PackageManager.NameNotFoundException|JSONException e) {
				return null;
			} finally {
				if(conn != null) {
					conn.disconnect();
				}
				if(toImgur != null) {
					try {
						toImgur.close();
					} catch(IOException e) {}
				}
				if(fromImage != null) {
					try {
						fromImage.close();
					} catch(IOException e) {}
				}
				if(fromImgur != null) {
					try {
						fromImgur.close();
					} catch(IOException e) {}
				}
			}
			return response;
		}

		@Override
		protected void onPostExecute(JSONObject imgurResponse) {
			if(imgurResponse == null) {
				Toast.makeText(MainActivity.this, R.string.imgur_upload_fail, Toast.LENGTH_LONG).show();
				return;
			}

			try {
				JSONObject data = imgurResponse.getJSONObject("data");
				imgurLink = data.optString("link", null);
			} catch(JSONException e) {
				Toast.makeText(MainActivity.this, R.string.imgur_upload_fail, Toast.LENGTH_LONG).show();
			}
			showImgurLink();
		}
	}
}
