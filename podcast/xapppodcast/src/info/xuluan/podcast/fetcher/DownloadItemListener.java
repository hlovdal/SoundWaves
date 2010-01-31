package info.xuluan.podcast.fetcher;

import info.xuluan.podcast.provider.FeedItem;

public interface DownloadItemListener {

	public void onBegin(FeedItem item);

	public void onUpdate(FeedItem item);

	public void onFinish();

}
