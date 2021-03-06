package edu.buffalo.cse.phonelab.datalogger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import edu.buffalo.cse.phonelab.utilities.Util;

/**
 * @author mike
 *
 */
public class LoggerService extends Service {
	private final IBinder mBinder = new LogBinder();
	private Timer timer = new Timer();
	private final String LOG_DIR = Environment.getExternalStorageDirectory() + "/" + DataLoggerConstants.LOG_DIR + "/";
	private SharedPreferences settings;
	private Editor editor;
	
	public void onCreate() {
		super.onCreate();
		Log.i(getClass().getSimpleName(), "Start Service");
		settings = getApplicationContext().getSharedPreferences(Util.SHARED_PREFERENCES_FILE_NAME, 0);
		editor = settings.edit();
		
		start();
	}
	
	
	public void start() {
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				Log.i(getClass().getSimpleName(), "Service Running");
				// Check for LogCat Process
				checkLogCatProcess();
				
			}
		},0, DataLoggerConstants.UPDATE_INTERVAL);
	}
	
	/**
	 * Checks whether LogCat Process is running or not, if not initiates the process 
	 */
	private void checkLogCatProcess() {
		List<Integer> pids = getPID("logcat");
		int pidFromDb = settings.getInt(Util.SHARED_PREFERENCES_DATA_LOGGER_PID, -1);
		boolean foundLogCat = false;
		if (pids.size() > 0) {
			for(int pid:pids) {
				// If logcat is found
				if(pidFromDb == pid) {
					Log.i(getClass().getSimpleName(), "Logcat process found " + pid);
					// Process Found
					if (logFileThreshold()) {
						transferLogFiles();
					}
					foundLogCat = true;
					break;
				} else {
					Log.i(getClass().getSimpleName(), "Killing bogus process " + pid);
					// Kill other bogus processes
					android.os.Process.killProcess(pid);
					// Start a new LogCat process
					foundLogCat = false;
				}
			}
			// Start LogCat if no process found
			if (!foundLogCat) {
				startLogcat();
			}
		} else {
			startLogcat();
		}
	}
	
	private boolean logFileThreshold() {
		File f = new File(LOG_DIR + "log.out");
		boolean threshold = f.length() < DataLoggerConstants.THRESHOLD * 1024;
		Log.i(getClass().getSimpleName(), "Checking Threshold ... " + threshold);
		return threshold;
	}
		
	/**
	 * Check for valid Log Files.
	 * Log.out is continously written
	 * Log.out.1
	 * Log.out.2
	 * ...
	 * Log.out.n will be created in worst case
	 * else Log.out.1 will be transferred successfully
	 * After Successful transfer, delete all files except Log.out
	 * 
	 * @param String filename - filename
	 * @return boolean
	 */
	private void transferLogFiles() {
		Log.i(getClass().getSimpleName(), "Starting to Merge files");
		File[] allFiles = new File(LOG_DIR).listFiles();
		String mergedFileSrc = LOG_DIR + "merged.txt";
		Log.i(getClass().getSimpleName(), "Files found .. " + allFiles.length);
		String line = "";
		AsyncHttpClient client = new AsyncHttpClient();
		RequestParams params = new RequestParams();
		String deviceId = ((TelephonyManager)getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
		String url = DataLoggerConstants.POST_URL + "?deviceId=" + deviceId;
		
		// If Log file exists in the directory
		if (allFiles.length > 1) {
			// Check all files
			for(int i=0; i<allFiles.length; i++) {
				String fileName = allFiles[i].getName();
				
				// Merge files together & save as merged.txt
				if (fileName != "log.out" && fileName.startsWith("log.out.")) {
					Log.i(getClass().getSimpleName(), "File " + i + " " + allFiles[i].getName());
					// Merge files
					try {
						BufferedReader ip = new BufferedReader(new FileReader(allFiles[i]));
						BufferedWriter op = new BufferedWriter(new FileWriter(mergedFileSrc, true));
						
						while((line = ip.readLine()) != null) {
							op.write(line);
						}
						op.flush(); 
						op.close(); 
						ip.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
					Log.i(getClass().getSimpleName(), "Removing File " + i + " " + allFiles[i].delete());
				}
			}
			
			// Now lets send Merged File
			final File f = new File(mergedFileSrc);
			Log.i(getClass().getSimpleName(), "Merged File " + f.length());
			// If File is new i.e created after last uploaded time
			// Set last successful upload time			
			try {
			    params.put("file", f);
			} catch(FileNotFoundException e) {}
			client.post(url , params, new AsyncHttpResponseHandler() {
			    @Override
			    public void onSuccess(String response) {
			    	Date now = new Date();
					editor.putLong(Util.SHARED_PREFERENCES_DATA_LOGGER_LAST_UPDATE_TIME, (System.currentTimeMillis()/1000 - ((now.getMinutes() * 60  + now.getSeconds()))));
					editor.commit();
					Log.i(getClass().getSimpleName(), "Removing Merged File " + f.delete());
					Log.i(getClass().getSimpleName(), "Response " + response);
			    }
			});
		} else {
			// No File
		}
	}
	
	/**
	 * Create Log Dir if it doesn`t exist
	 * @return boolean
	 */
	private void createLogDir() {
		File f = new File(LOG_DIR);
		if (f.exists() && f.isDirectory()) {
			return;
		} else {
			f.mkdirs();
		}
	}
	
	/**
	 * Returns PIDs of process
	 * @param String processName - process name
	 * @return List<Integer> pids - 0 is not found and pid > 0 is found
	 */
	private List<Integer> getPID(String processName) {
		List<Integer> pids = new ArrayList<Integer>();
		
		StringBuilder psString = new StringBuilder();
		try {
			Process process = Runtime.getRuntime().exec("ps");// Verbose filter
	        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	        String line;
	        while ((line = bufferedReader.readLine()) != null) {
	        	// check whether processName exists in line
	        	if (line.endsWith(processName)) {
	        		psString.append(line + "\n");
	        	}
	        }
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Check whether some processes were collected & Terminated is not contained
		if(psString.length() > 0 && !psString.toString().matches("Terminated")) {
			String[] splitString = psString.toString().split("\n");
			
			for(int i = 0; i<splitString.length; i++) {
				if (splitString[i].startsWith("app_")) {
					Log.i(getClass().getSimpleName(), splitString[i].toString());
					String[] splitPs = splitString[i].split("\\s+");
					pids.add(Integer.parseInt(splitPs[1]));
				}
			}
		}
		
	    return pids;
	}
	
	/**
	 * Create Logcat process if it doesn`t exist
	 */
	private void startLogcat() {
		//Process process;
		int pid = 0;
		try {
			Log.i(getClass().getSimpleName(), "Logcat process not found, starting process");
			// create log dir if it doesn`t exist
			createLogDir();
			Runtime.getRuntime().exec("logcat -v long -f " + LOG_DIR + "log.out -r " + DataLoggerConstants.LOG_FILE_SIZE + " -n " + DataLoggerConstants.AUX_LOG_FILES + " &");
			pid = getPID("logcat").iterator().next();
	        editor.putInt(Util.SHARED_PREFERENCES_DATA_LOGGER_PID, pid);
	        Log.i(getClass().getSimpleName(), "PID to db" + pid);
		} catch (IOException e) {
			Log.w(getClass().getSimpleName(), e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void onDestroy() {
		super.onDestroy();
		if (timer != null) {
			timer.cancel();
		}
		
		Log.i(getClass().getSimpleName(), "Kill Service");
		Log.i(getClass().getSimpleName(), "Timer stopped.");
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return mBinder;
	}
	
	public class LogBinder extends Binder {
		LoggerService getService() {
			return LoggerService.this;
		}
	}
}
