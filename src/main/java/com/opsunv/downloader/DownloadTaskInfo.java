package com.opsunv.downloader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.http.HttpHost;
import org.apache.http.message.BasicNameValuePair;

public class DownloadTaskInfo {
	
	public final static int FILE_SAVE_DOWNLOAD = 1;
	
	/**
	 * 此类型的下载任务将为把数据保存在内存中
	 */
	public final static int WEB_PAGE_LOAD = 2;
	
	//任务id
	private String id;
	
	//任务名
	private String name;
	
	//保存路径
	private String savePath;
	
	//下载地址
	private String url;
	
	//线程数
	private int threads = 1;
	
	//下载任务类型
	private int type = 2;
	
	//请求method
	private String method;
	
	//任务状态:1.暂停 2.下载中 3.完成
	private int state;
	
	//在post提交时
	private String charset;
	
	//post提交时的参数
	private List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
	
	//请求头header
	private Map<String,String> headers = new HashMap<String, String>();
	
	//代理
	private HttpHost proxy;
	
	private DownloadCompleteCallback callback;
	
	//是否只对响应200的读取body
	private boolean only200 = false;
	
	public DownloadTaskInfo(String url) {
		this.url = url;
		this.setId(UUID.randomUUID().toString());
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSavePath() {
		return savePath;
	}

	public void setSavePath(String savePath) {
		this.savePath = savePath;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getThreads() {
		return threads;
	}

	public void setThreads(int threads) {
		this.threads = threads;
	}

	/**
	 * 获取userAgent
	 * @return
	 */
	public String getUserAgent() {
		return headers.get("User-Agent");
	}

	/**
	 * 设置userAgent
	 * @param userAgent
	 */
	public void setUserAgent(String userAgent) {
		headers.put("User-Agent", userAgent);
	}

	public String getReferer() {
		return headers.get("Referer");
	}

	/**
	 * 设置referer
	 * @param referer
	 */
	public void setReferer(String referer) {
		headers.put("Referer", referer);
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public List<BasicNameValuePair> getParams() {
		return params;
	}

	public void setParams(List<BasicNameValuePair> params) {
		this.params = params;
	}

	/**
	 * 设置post的请求参数
	 * @param name
	 * @param value
	 */
	public void setParameter(String name,String value){
		params.add(new BasicNameValuePair(name, value));
	}

	public Map<String, String> getHeaders() {
		return headers;
	}
	
	/**
	 * 添加请求header
	 * @param name
	 * @param value
	 */
	public void addHeader(String name,String value){
		headers.put(name, value);
	}

	public HttpHost getProxy() {
		return proxy;
	}

	public void setProxy(HttpHost proxy) {
		this.proxy = proxy;
	}
	
	public void setCallback(DownloadCompleteCallback callback) {
		this.callback = callback;
	}
	
	public DownloadCompleteCallback getCallback() {
		return callback;
	}

	public boolean isOnly200() {
		return only200;
	}
	
	/**
	 * 是否只对响应200的返回读取body数据
	 * @param only200
	 */
	public void setOnly200(boolean only200) {
		this.only200 = only200;
	}
	
}
