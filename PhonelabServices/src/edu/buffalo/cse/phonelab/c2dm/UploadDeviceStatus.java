/**
 * @author fatih
 */
package edu.buffalo.cse.phonelab.c2dm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import edu.buffalo.cse.phonelab.manifest.PhoneLabApplication;
import edu.buffalo.cse.phonelab.manifest.PhoneLabManifest;
import edu.buffalo.cse.phonelab.utilities.Util;

public class UploadDeviceStatus {
	/**
	 * This method upload device status to server including all the applications, deviceId and Registration Id
	 * @param context
	 * @param urlToUpload
	 */
	public void uploadDeviceStatus(Context context, String urlToUpload) {
		try {
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpPost httpost = new HttpPost(urlToUpload);
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			String response = "";
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			try {
				SharedPreferences settings = context.getSharedPreferences(Util.SHARED_PREFERENCES_FILE_NAME, 0);
				nameValuePairs.add(new BasicNameValuePair("device_id", Util.getDeviceId(context)));
				String regId = settings.getString(Util.SHARED_PREFERENCES_REG_ID_KEY, null);
				if (regId != null) {
					nameValuePairs.add(new BasicNameValuePair("reg_id", regId));
				}
				HashMap<String, JSONArray> map = new HashMap<String, JSONArray>();
				map.put("apps", getApss());
				JSONObject jsonObj = new JSONObject(map);
				nameValuePairs.add(new BasicNameValuePair("apps", jsonObj.toString()));
				map.clear();
				httpost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				response = httpclient.execute(httpost,responseHandler);
				Log.i(getClass().getSimpleName(), "Response: \n" + response);

				JSONObject responseJ = new JSONObject(response);
				if (responseJ.getString("error").equals("")) {//success
					
				} else {//error
					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Internal method for reading all the apps from manifest
	 * @return
	 */
	private JSONArray getApss(){
		List<JSONObject> apps = new ArrayList<JSONObject>();
		PhoneLabManifest manifest = new PhoneLabManifest(Util.CURRENT_MANIFEST_DIR);
		if (manifest.getManifest()) {
			try {
				ArrayList<PhoneLabApplication> applications = manifest.getAllApplications();
				for (PhoneLabApplication application:applications) {
					HashMap<String, String> app = new HashMap<String, String>();
					app.put("name", application.getName());
					app.put("package_name", application.getPackageName());
					app.put("description", "" + application.getDescription());
					app.put("type", "" + application.getType());
					app.put("start_time", "" + application.getStartTime());
					app.put("end_time", "" + application.getEndTime());
					app.put("download", "" + application.getDownload());
					app.put("version", "" + application.getVersion());
					app.put("action", "" + application.getAction());
					JSONObject object = new JSONObject(app);
					apps.add(object);
				}
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			}
		}
		
		JSONArray jsonArray = new JSONArray(apps);
		return jsonArray;
	}
}
