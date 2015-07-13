package com.opsunv.downloader;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.message.BasicNameValuePair;

/**
 * 封装了一些常用的http请求操作
 * 如提交表单
 * @author opsun
 * @createDate 2011-12-2 下午09:54:25
 */
public class SimpleHttpRequest extends HttpEntityEnclosingRequestBase{

    public final static String GET = "GET";
    public final static String POST = "POST";

    private String method;
    
    private String charset;
    
    public SimpleHttpRequest(String method) {
        super();
        this.method = method;
    }

    public SimpleHttpRequest(URI uri,String method) {
        super();
        this.setMethod(method);
        setURI(uri);
    }

    public SimpleHttpRequest(String uri,String method) {
        super();
        this.setMethod(method);
        setURI(URI.create(uri));
    }

    @Override
    public String getMethod() {
        return method;
    }
    
    /**
     * 设置请求提交的参数
     * @param params
     */
    public void setParameters(List<BasicNameValuePair> params){
    	if(params ==null) return;
    	
    	if(POST.equalsIgnoreCase(method)){
    		UrlEncodedFormEntity entity = null;
			try {
				entity = new UrlEncodedFormEntity(params,charset);
			} catch (UnsupportedEncodingException e) {
			}
    		setEntity(entity);
    	}else if(GET.equals(method)){
    		//get方式下的参数格式
    	}

    }
    
	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public void setMethod(String method) {
		this.method = StringUtils.isEmpty(method)?GET:method.toUpperCase();
	}
    
}
