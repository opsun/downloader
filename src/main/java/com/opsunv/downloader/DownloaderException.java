package com.opsunv.downloader;

public class DownloaderException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public DownloaderException() {
		super();
	}
	
	public DownloaderException(String s){
		super(s);
	}
}
