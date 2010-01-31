package info.xuluan.podcast;

import info.xuluan.podcast.service.PodcastService;
import info.xuluan.podcast.utils.Log;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.widget.SimpleCursorAdapter;

public class PodcastBaseActivity extends ListActivity {

	public static final int COLUMN_INDEX_TITLE = 1;

	protected PodcastService mServiceBinder = null;
	protected final Log log = Log.getLog(getClass());

	protected SimpleCursorAdapter mAdapter;
	protected Cursor mCursor = null;

	protected ComponentName mService = null;
	protected boolean mInit = false;

	protected ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mServiceBinder = ((PodcastService.ReadingBinder) service)
					.getService();
			log.info("onServiceConnected");
		}

		public void onServiceDisconnected(ComponentName className) {
			mServiceBinder = null;
			log.info("onServiceDisconnected");
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			unbindService(serviceConnection);
		} catch (Exception e) {
			e.printStackTrace();

		}

		// stopService(new Intent(this, service.getClass()));
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mInit) {
			mInit = false;

			if (mCursor != null)
				mCursor.close();

			unbindService(serviceConnection);

			startInit();

		}

	}

	@Override
	protected void onPause() {
		super.onPause();

	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mInit = true;

		log.info("onLowMemory()");
	}

	public void startInit() {

		log.info("startInit()");

		mService = startService(new Intent(this, PodcastService.class));

		Intent bindIntent = new Intent(this, PodcastService.class);
		bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
	}
}
