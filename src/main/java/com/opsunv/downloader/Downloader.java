package com.opsunv.downloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.FixedPoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

public class Downloader {
	private final static Logger log = Logger.getLogger(Downloader.class);
	
	private ThreadPoolExecutor executor;
	
	private Map<String, DownloadTaskInfo> tasks = new ConcurrentHashMap<String, DownloadTaskInfo>();
	
	//任务队列
	private LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
	
	//线程池大小
	private int maxThreads = 100;
	
	//空闲线程回收时间单位:秒 默认设置为30分钟
	private long keepAliveTime = 1800;
	
	//通过本地连接的httpclient，如果是使用代理则重新实例化httpclient
	private HttpClient httpClient;
	
	private ClientConnectionManager clientConnectionManager;
	
	//已完成的任务总数
	private AtomicInteger completedTask = new AtomicInteger();
	
	private boolean isPaused;
	private ReentrantLock pauseLock = new ReentrantLock();
	private Condition unpaused = pauseLock.newCondition();
	
	private boolean isMoniterPause = false;

	public Downloader() {
		init();
	}
	
	public void init(){
		log.info("初始化下载器:maxThreads="+maxThreads+",keepAliveTime="+keepAliveTime);
		executor = new ThreadPoolExecutor(maxThreads, maxThreads, keepAliveTime, TimeUnit.SECONDS, queue){
			
			//执行任务前检查是否下载器被暂停
			protected void beforeExecute(Thread t, Runnable r) {
				super.beforeExecute(t, r);
				pauseLock.lock();
				try {
					while (isPaused)
						unpaused.await();
				} catch (InterruptedException ie) {
					t.interrupt();
				} finally {
					pauseLock.unlock();
				}
			}

		};
		executor.allowCoreThreadTimeOut(true);
		
		FixedPoolingClientConnectionManager manager = new FixedPoolingClientConnectionManager();
		manager.setDefaultMaxPerRoute(10);
		manager.setMaxTotal(100);
		this.clientConnectionManager = manager;
		
		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(params, 10000);
		HttpConnectionParams.setConnectionTimeout(params, 5000);
		
		httpClient = new DefaultHttpClient(clientConnectionManager,params);
		
		Thread networkMoniter = new Thread("network-state-moniter"){
			@Override
			public void run() {
				while(true){
					if(WebUtils.networkIsOk()){
						if(isPaused&&isMoniterPause){
							Downloader.this.resume();
							isMoniterPause = false;
						}
					}else{
						pause();
						isMoniterPause = true;
					}
					
					try {
						sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		networkMoniter.setDaemon(true);
		networkMoniter.start();
		
		Thread idleThread = new Thread("IDLE-CONNECTION-MONITOR-THREAD"){
			@Override
			public void run() {
	            while (true) {
					try {
						sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
                    clientConnectionManager.closeExpiredConnections();
                    clientConnectionManager.closeIdleConnections(30, TimeUnit.SECONDS);
	            }
			}
		};
		
		idleThread.setDaemon(true);
		idleThread.start();
	}
	
	
	public Future<DownloadResult> submit(DownloadTaskInfo taskInfo){
		if(taskInfo==null) throw new NullPointerException();
		if(tasks.containsKey(taskInfo.getId())){
			throw new DownloaderException("任务已存在!");
		}
		
		tasks.put(taskInfo.getId(), taskInfo);
		
		return executor.submit(new DownloadTask(taskInfo));
		
	}
	
	public void remove(String taskId){
		
	}
	
	public void start(){
		
	}
	
	public void pause() {
		pauseLock.lock();
		try {
			isPaused = true;
		} finally {
			pauseLock.unlock();
		}
	}

	public void resume() {
		pauseLock.lock();
		try {
			isPaused = false;
			unpaused.signalAll();
		} finally {
			pauseLock.unlock();
		}
	}
	
	public boolean isPaused() {
		return isPaused;
	}

	public void stop(){
		executor.shutdown();
	}
	
	public long getCompletedTasks(){
		return executor.getCompletedTaskCount();
	}
	
	public int getActiveTasks(){
		return executor.getActiveCount();
	}
	
	public int getQueueTasks(){
		return executor.getQueue().size();
	}
	
	private class DownloadTask implements Callable<DownloadResult>{
		private HttpResponse response;
		private DownloadTaskInfo taskInfo;
		private SimpleHttpRequest request;
		private String defaultUserAgent = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";
		private DownloadResult result;
		
		public DownloadTask(DownloadTaskInfo task) {
			this.taskInfo = task;
		}

		private void downloadFile() throws Exception{
			
			FileOutputStream out = null;
			InputStream in = response.getEntity().getContent();
			try{
				out = new FileOutputStream(new File(taskInfo.getSavePath()));
				in = response.getEntity().getContent();
				byte[] buf = new byte[1024];
				int len;
				while((len=in.read(buf, 0, 1024))>0){
					out.write(buf,0,len);
				}

			}catch (Exception e) {
				throw e;
			}finally{
				EntityUtils.consume(response.getEntity());
				try{
					out.close();
				}catch (Exception e) {
				}
			}
			
		}
		
		/**
		 * 获取内容的byte数组
		 * @return
		 * @throws Exception
		 */
		private byte[] getContentByte() throws Exception{
			InputStream in = response.getEntity().getContent();
			byte[] buf = new byte[1024];
			int len;
			ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
			while((len=in.read(buf, 0, 1024))!=-1){
				out.write(buf, 0, len);
			}
			in.close();
			
			return out.toByteArray();
		}
		
		/**
		 * 初始化请求
		 */
		private void initRequest(){
			//设置请求相关参数
			request = new SimpleHttpRequest(taskInfo.getUrl(), taskInfo.getMethod());
			request.setHeader("User-Agent", StringUtils.isEmpty(taskInfo.getUserAgent())?defaultUserAgent:taskInfo.getUserAgent());
			if(!StringUtils.isEmpty(taskInfo.getReferer())){
				request.setHeader("Referer", taskInfo.getReferer());
			}
			
			request.setCharset(taskInfo.getCharset());
			request.setParameters(taskInfo.getParams());
			
			if(taskInfo.getHeaders()!=null){
				for(Map.Entry<String, String> header : taskInfo.getHeaders().entrySet()){
					Header old = request.getLastHeader(header.getKey());
					if(old!=null){
						request.removeHeaders(old.getName());
					}
					request.addHeader(header.getKey(),header.getValue());
				}
			}
			
			request.getParams().setParameter("http.protocol.cookie-policy", CookiePolicy.IGNORE_COOKIES);
			
			if(taskInfo.getProxy()!=null){
				request.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, taskInfo.getProxy());
			}
		}
		
		private int loopTimes = 0;
		
		/**
		 * 测试网络是否可用
		 * @return 返回true表示可用 返回false不可用
		 */
		private boolean testNetwork(){
			//检查网络是否连接
			while(true){
				
				//如果网络可用则跳出循环 
				if(WebUtils.networkIsOk()){
					//只有在第一次循环时才跳出之后若多次进入则可认为主机不可达
					if(loopTimes==0){
						loopTimes++;
						log.info("网络已恢复");
						return true;
					}else{
						return false;
					}
				}else{
					//如果测试网络不可用则等待1秒后重新检测网络是否可用
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
				
			}
		}
		
		private void doRequest(){
			while(true){
				try{
					response = httpClient.execute(request);
					break;
				}catch (NoRouteToHostException e) {
					log.info("网络可能断开："+e.getMessage());
					if(!testNetwork()){
						return;
					}
				}catch (UnknownHostException e) {
					log.info("网络可能断开："+e.getMessage());
					if(!testNetwork()){
						return;
					}
				}catch (Exception e) {
//					e.printStackTrace();
					break;
				}
				
			}
			
		}

		@Override
		public DownloadResult call() throws Exception {
			long time = System.currentTimeMillis();
			initRequest();
			doRequest();
			
			result = new DownloadResult();
			if(response!=null){
				result.setHeaders(response.getAllHeaders());
				result.setStatusCode(response.getStatusLine().getStatusCode());
				if(taskInfo.getType()==DownloadTaskInfo.WEB_PAGE_LOAD){
					log.debug("任务为读取 "+taskInfo.getUrl()+" 的数据");
					if(!(taskInfo.isOnly200()&&result.getStatusCode()!=200)){
						result.setData(getContentByte());
					}else{
						request.abort();
					}
				}else if(taskInfo.getType()==DownloadTaskInfo.FILE_SAVE_DOWNLOAD){
					log.debug("任务为下载 "+taskInfo.getUrl()+" 并保存到"+taskInfo.getSavePath());
					downloadFile();
				}
			}
			
			result.setElapsedTime(System.currentTimeMillis()-time);
			
			if(taskInfo.getCallback()!=null){
				taskInfo.getCallback().callback(taskInfo, result);
			}
			
			completedTask.getAndIncrement();
			tasks.remove(taskInfo.getId());
			
			return result;
		}
		
	}

}
