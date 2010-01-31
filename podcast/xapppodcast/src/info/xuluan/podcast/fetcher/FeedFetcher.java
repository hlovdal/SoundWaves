package info.xuluan.podcast.fetcher;

import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import android.util.Log;

public class FeedFetcher {

	private int maxSize = 100 * 1024;
	private static final int TIMEOUT = 20 * 1000;
	private boolean canceled = false;

	public void setProxy(String host, int port) {
		Properties props = System.getProperties();
		props.put("proxySet", "true");
		props.put("proxyHost", host);
		props.put("proxyPort", String.valueOf(port));
	}

	public Response fetch(String feedUrl) {
		return fetch(feedUrl, 0L);
	}

	public Response fetch(String feedUrl, long ifModifiedSince) {
		setCanceled(false);
		Response response = null;
		try {
			response = get(feedUrl, ifModifiedSince);
		} catch (InterruptedException e) {
			return null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!isCanceled()) {

		}

		return response;

	}

	Response get(String url, long ifModifiedSince) throws IOException,
			InterruptedException {
		URL u = new URL(url);
		InputStream input = null;
		ByteArrayOutputStream output = null;
		HttpURLConnection hc = null;
		HttpURLConnection.setFollowRedirects(true);
		try {
			hc = (HttpURLConnection) u.openConnection();
			if (ifModifiedSince > 0)
				hc.setIfModifiedSince(ifModifiedSince);
			hc.setRequestMethod("GET");
			hc.setUseCaches(false);
			hc.addRequestProperty("Accept", "*/*");
			hc
					.addRequestProperty("User-Agent",
							"Mozilla/4.0 (compatible; Windows XP 5.1; MSIE 6.0.2900.2180)");
			hc.addRequestProperty("Accept-Encoding", "gzip");
			hc.setReadTimeout(TIMEOUT);

			hc.connect();
			int code = hc.getResponseCode();
			if (code == 304)
				return null;
			if (code != 200)
				throw new IOException("Connection failed: " + code);
			// detect content type and charset:
			String contentType = hc.getContentType();
			String charset = null;
			if (contentType != null) {
				int n = contentType.indexOf("charset=");
				if (n != (-1)) {
					charset = contentType.substring(n + 8).trim();
					contentType = contentType.substring(0, n).trim();
					if (contentType.endsWith(";")) {
						contentType = contentType.substring(0,
								contentType.length() - 1).trim();
					}
				}
			}
			boolean gzip = "gzip".equals(hc.getContentEncoding());
			input = gzip ? new GZIPInputStream(hc.getInputStream()) : hc
					.getInputStream();
			output = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int total = 0;
			for (;;) {
				int n = input.read(buffer);
				if (n == (-1))
					break;
				total += n;
				if (total > maxSize)
					break;
				// throw new IOException("Feed size is too large. More than " +
				// maxSize + " bytes.");

				output.write(buffer, 0, n);
			}
			output.close();
			Log.w("RSS", "download length = " + total);

			return new Response(contentType, charset, output.toByteArray());
		} catch (Exception e) {
			e.printStackTrace();
			return null;

		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
				}
			}
			if (hc != null) {
				hc.disconnect();
			}

		}
	}

	/**
	 * Get if operation is marked as canceled.
	 */
	public synchronized boolean isCanceled() {
		return this.canceled;
	}

	/**
	 * Set operation canceled.
	 */
	public synchronized void cancel() {
		setCanceled(true);
	}

	synchronized void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}

	public static int download(FeedItem item, DownloadItemListener listener) {
		String pathname = item.pathname;

		int nStartPos = item.offset;

		RandomAccessFile oSavedFile = null;
		InputStream input = null;
		HttpURLConnection httpConnection = null;
		try {
			URL url = new URL(item.resource);
			Log.w("RSS", "url = " + url);
			oSavedFile = new RandomAccessFile(pathname, "rw");

			httpConnection = (HttpURLConnection) url.openConnection();
			httpConnection.setReadTimeout(TIMEOUT);

			httpConnection
					.setRequestProperty("User-Agent", "Internet Explorer");
			if (item.offset != 0) {
				String sProperty = "bytes=" + item.offset + "-";
				httpConnection.setRequestProperty("RANGE", sProperty);
				System.out.println(sProperty);
				oSavedFile.seek(item.offset);
			}

			int responseCode = httpConnection.getResponseCode();
			Log.w("RSS", "Error Code : " + responseCode);
			if (responseCode >= 500) {
				item.offset = 0;
				throw new IOException("Error Code : " + responseCode);
			} else if (responseCode >= 400) {
				throw new IOException("Error Code : " + responseCode);
			}

			long nEndPos = item.length;
			if (item.offset == 0) {

				nEndPos = httpConnection.getContentLength();
				if (nEndPos < 0) {
					Log.w("RSS", "Cannot get content length: " + nEndPos);

					throw new IOException("Cannot get content length: "
							+ nEndPos);
				}
				item.length = nEndPos;
			}
			Log.w("RSS", "nEndPos = " + nEndPos);

			input = httpConnection.getInputStream();
			int buff_size = 1024 * 4;
			byte[] b = new byte[buff_size];
			int nRead = 0;

			while ((nRead = input.read(b, 0, buff_size)) > 0
					&& nStartPos < nEndPos) {
				if (listener != null)
					listener.onUpdate(item);
				oSavedFile.write(b, 0, nRead);
				nStartPos += nRead;
				item.offset = nStartPos;
			}
			if (nStartPos >= nEndPos)
				item.status = ItemColumns.ITEM_STATUS_NO_PLAY;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			try {
				if (httpConnection != null)
					httpConnection.disconnect();
			} catch (Exception e) {
			}

			try {
				if (input != null)
					input.close();
			} catch (Exception e) {
			}

			try {
				if (oSavedFile != null)
					oSavedFile.close();
			} catch (Exception e) {
			}

		}

		return 0;

	}

}
