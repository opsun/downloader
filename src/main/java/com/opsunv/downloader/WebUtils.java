package com.opsunv.downloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

import com.herodl.utils.Assert;


/**
 * 一些静态工具方法
 * @author opsun
 * @version 1.0.0, 2013-1-4 下午4:54:53
 */
public class WebUtils {
	
	/**
	 * 获取任务中的数据，根据返回数据采用合理的编码
	 * @param result
	 * @return
	 */
	public static String getDownloadTaskContent(DownloadResult result){
		
		if(result==null){
			throw new NullPointerException();
		}
		
		if(result.getData()==null||result.getData().length<1){
			return null;
		}
		
		
		String encoding = result.getHeader("Content-Encoding");
		if(encoding!=null){
			try {
				InputStream in = null;
				if("gzip".equalsIgnoreCase(encoding)){
					in = new GZIPInputStream(new ByteArrayInputStream(result.getData()));
				}else if("deflate".equalsIgnoreCase(encoding)){
					in = new DeflaterInputStream(new ByteArrayInputStream(result.getData()));
				}
				
				if(in!=null){
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					
					byte[] data = new byte[1024];
					int len=0;
					while((len=in.read(data,0,1024))!=-1){
						out.write(data,0,len);
					}
					
					in.read(data);
					out.close();
					result.setData(out.toByteArray());
					in.close();
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}

		String charset = null;
		
		//从content-type中获取charset
		String contentType = result.getHeader("Content-Type");
		if(contentType!=null){
			for(String str:contentType.split(";")){
				if(str.contains("charset")){
					charset = str.split("=")[1];
					break;
				}
			}
		}
		
		//从meta标签中获取charset
		if(charset==null){
			Pattern pattern = Pattern.compile("<meta.*?http-equiv=\"Content-Type\".*?>",Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(new String(result.getData()));
			
			if(matcher.find()){
				Pattern p = Pattern.compile("<meta.*?content=\"(.*?)\".*?>",Pattern.CASE_INSENSITIVE);
				Matcher m = p.matcher(matcher.group());
				
				if(m.find()){
					for(String str:m.group(1).split(";")){
						if(str.contains("charset")){
							charset = str.split("=")[1];
							break;
						}
					}
				}
			}
		}
		
		if(charset!=null){
			try {
				return new String(result.getData(),charset);
			} catch (UnsupportedEncodingException e) {
				//e.printStackTrace();
				return new String(result.getData());
			}
		}else{
			return new String(result.getData());
		}
	}


	/**
	 * 获取给定内容中的所有超链接，如果存在相对路径的链接，
	 * 根据提供的baseurl转换为绝对路径的链接地址
	 * @param content
	 * @param baseurl
	 * @return
	 */
	public static List<String> getSuperLink(String content,String baseurl){
		if(Assert.isEmpty(content)){
			return null;
		}
		
		if(baseurl==null){
			baseurl = "";
		}
		
		Pattern pattern = Pattern.compile("<a.*?href=\"(.*?)\".*?>.*?</a>");
		Matcher matcher = pattern.matcher(content);
		
		List<String> list = new ArrayList<String>();
		
		while(matcher.find()){
			list.add(matcher.group(1));
		}
		
		return list;
		
	}
	
	private static long lastOkTime = 0;
	
	/**
	 * 网络是否可用
	 * @return
	 */
	public synchronized static boolean networkIsOk(){
		
		//两秒内测试ok则认为ok
		if(System.currentTimeMillis()-lastOkTime<2000){
			return true;
		}
		
		try{
			URL url = new URL("http://www.baidu.com/robots.txt");
			URLConnection conn = url.openConnection();
			conn.setConnectTimeout(3000);
			conn.setReadTimeout(10000);
			url.getContent();
			lastOkTime = System.currentTimeMillis();
			return true;
		}catch (Exception e) {
			return false;
		}
		
	}
	
}
