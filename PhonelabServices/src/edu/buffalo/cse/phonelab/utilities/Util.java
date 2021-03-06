/**
 * @author fatih
 */
package edu.buffalo.cse.phonelab.utilities;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import edu.buffalo.cse.phonelab.phonelabservices.PhoneLabMainView;
import edu.buffalo.cse.phonelab.phonelabservices.R;

public class Util {

	public static final String SHARED_PREFERENCES_FILE_NAME = "phonelab_settings";
	public static final String SHARED_PREFERENCES_REG_ID_KEY = "reg_id";
	public static final String SHARED_PREFERENCES_SYNC_KEY = "is_synced";
	public static final String SHARED_PREFERENCES_DATA_LOGGER_PID = "logcat_pid";
	public static final String SHARED_PREFERENCES_DATA_LOGGER_LAST_UPDATE_TIME = "data_logger_last_updated";
	
	public static final String CURRENT_MANIFEST_DIR = "/sdcard/manifest.xml";//directory for current, latest manifest
	public static String getDeviceId (Context context) {
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		return tm.getDeviceId();
	}
	
	/**
	 * send Notification message to device user and points him to PhonelabMainView Class
	 * @param context
	 * @param Header
	 * @param Message
	 */
	public void nofityUser(Context context,String Header, String Message) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
		int icon = R.drawable.ic_launcher;
		CharSequence tickerText = Header;
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		Context appContext = context.getApplicationContext();
		CharSequence contentTitle = Header;
		CharSequence contentText = Message;
		
		Intent notificationIntent = new Intent(context, PhoneLabMainView.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		notification.setLatestEventInfo(appContext, contentTitle, contentText, contentIntent);
		mNotificationManager.notify((int) when, notification);
	}
}
