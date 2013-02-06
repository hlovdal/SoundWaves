package info.bottiger.podcast.receiver;

import info.bottiger.podcast.provider.BitmapProvider;
import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.provider.WithIcon;
import info.bottiger.podcast.service.PodcastDownloadManager;
import info.bottiger.podcast.utils.SDCardManager;

import java.io.File;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

/**
 * Receives event when a download is complete. If the file is succesfully
 * downloaded we move it to the currect destination.
 * 
 * @author bottiger
 * 
 */
public class DownloadManagerReceiver extends BroadcastReceiver {

	private DownloadManager downloadManager;

	@Override
	public void onReceive(Context context, Intent intent) {
		downloadManager = (DownloadManager) context
				.getSystemService(Context.DOWNLOAD_SERVICE);

		String action = intent.getAction();
		if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
			long downloadId = intent.getLongExtra(
					DownloadManager.EXTRA_DOWNLOAD_ID, 0);

			Query query = new Query();
			query.setFilterById(downloadId);
			Cursor c = downloadManager.query(query);
			if (c.moveToFirst()) {
				int columnIndex = c
						.getColumnIndex(DownloadManager.COLUMN_STATUS);
				if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
					
					String currentLocation = c
							.getString(c
									.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));

					FeedItem item = FeedItem.getByDownloadReference(
							context.getContentResolver(), downloadId);

					if (item != null) {
						item.setDownloaded(true);
						item.filesize = c
								.getInt(c
										.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

						String filename = Integer.toString(item
								.getEpisodeNumber())
								+ "_"
								+ item.title.replace(' ', '_')
								+ ".mp3";
						item.setFilename(filename);
						
						/* Calculate the imagePath */
						String imageURL = null;
						if (item != null) {
							imageURL = new BitmapProvider(context, item)
									.getThumbnailPath();
							item.image = imageURL;
						}

						// Rename the file
						File oldFile = new File(currentLocation);
						File newFileName = new File(item.getAbsolutePath());
						oldFile.renameTo(newFileName);

						item.update(context.getContentResolver());
						if (PodcastDownloadManager.getmDownloadingIDs().contains(downloadId))
							PodcastDownloadManager.getmDownloadingIDs().remove(downloadId);
						
						/*
						 * If no more files are being downloaded we purge the tmp dir.
						 * Things might build up here if downloads are aborted for various reasons. 
						 */
						if (PodcastDownloadManager.getmDownloadingIDs().size() == 0) {
							File directory = new File(SDCardManager.getTmpDir());

							// Get all files in directory

							File[] files = directory.listFiles();
							for (File file : files)
							{
							   // Delete each file

							   if (!file.delete())
							   {
							       // Failed to delete file

							       System.out.println("Failed to delete "+file);
							   }
							} 
						}
					}
				}
			}
			
			PodcastDownloadManager.removeExpiredDownloadedPodcasts(context);
		}
	}
}
