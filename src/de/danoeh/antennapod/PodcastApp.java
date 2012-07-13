package de.danoeh.antennapod;

import java.util.concurrent.TimeUnit;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import de.danoeh.antennapod.asynctask.FeedImageLoader;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.receiver.FeedUpdateReceiver;
import de.danoeh.antennapod.util.StorageUtils;

public class PodcastApp extends Application implements
		SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String TAG = "PodcastApp";
	public static final String PREF_NAME = "AntennapodPrefs";

	public static final String PREF_PAUSE_ON_HEADSET_DISCONNECT = "prefPauseOnHeadsetDisconnect";
	public static final String PREF_FOLLOW_QUEUE = "prefFollowQueue";
	public static final String PREF_DOWNLOAD_MEDIA_ON_WIFI_ONLY = "prefDownloadMediaOnWifiOnly";
	public static final String PREF_UPDATE_INTERVALL = "prefAutoUpdateIntervall";
	public static final String PREF_MOBILE_UPDATE = "prefMobileUpdate";

	private static PodcastApp singleton;

	public static PodcastApp getInstance() {
		return singleton;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		singleton = this;
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		if (StorageUtils.storageAvailable()) {
			FeedManager manager = FeedManager.getInstance();
			manager.loadDBData(getApplicationContext());
		}
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		Log.w(TAG, "Received onLowOnMemory warning. Cleaning image cache...");
		FeedImageLoader.getInstance().wipeImageCache();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Log.d(TAG, "Registered change of application preferences");
		if (key.equals(PREF_UPDATE_INTERVALL)) {
			AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
			int hours = Integer.parseInt(sharedPreferences.getString(
					PREF_UPDATE_INTERVALL, "0"));
			PendingIntent updateIntent = PendingIntent.getBroadcast(this, 0,
					new Intent(FeedUpdateReceiver.ACTION_REFRESH_FEEDS), 0);
			alarmManager.cancel(updateIntent);
			if (hours != 0) {
				long newIntervall = TimeUnit.HOURS.toMillis(hours);
				alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
						newIntervall, newIntervall, updateIntent);
				Log.d(TAG, "Changed alarm to new intervall");
			} else {
				Log.d(TAG, "Automatic update was deactivated");
			}
		}
	}
}