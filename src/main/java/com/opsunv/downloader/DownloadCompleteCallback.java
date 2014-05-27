package com.opsunv.downloader;

/**
 * 下载完成时执行回调
 * @author opsun
 * @version 0.1 2013-8-13 下午5:00:53
 */
public interface DownloadCompleteCallback {
	
	/**
	 * 
	 * @param info
	 * @param result
	 */
	public void callback(DownloadTaskInfo info,DownloadResult result);
}
