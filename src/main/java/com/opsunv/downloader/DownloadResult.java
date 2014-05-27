package com.opsunv.downloader;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;

import com.herodl.utils.Assert;


/**
 * 下载结果
 * @author opsun
 * @version 0.1 2013-8-13 下午4:47:12
 */
public class DownloadResult {
	
	private Header[] headers;
	
	private long elapsedTime;
	
	private byte[] data;
	
	private int statusCode;

	public Header[] getHeaders() {
		return headers;
	}

	public void setHeaders(Header[] headers) {
		this.headers = headers;
	}
	
	/**
	 * 获取key对应的header,可能包含多个key对应的header
	 * @param key
	 * @return
	 */
	public List<Header> getHeaders(String key){
		List<Header> list = new ArrayList<Header>();
		if(headers!=null&&!Assert.isEmpty(key)){
			for(Header h :headers){
				if(key.equals(h.getName())){
					list.add(h);
				}
			}
		}
		
		return list;
	}

	public long getElapsedTime() {
		return elapsedTime;
	}

	public void setElapsedTime(long elapsedTime) {
		this.elapsedTime = elapsedTime;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	
	public String getHeader(String key){
		if(Assert.isEmpty(key)){
			return null;
		}
		
		for(Header header : headers){
			if(key.equals(header.getName())){
				return header.getValue();
			}
		}
		
		return null;
		
	}
	
}
