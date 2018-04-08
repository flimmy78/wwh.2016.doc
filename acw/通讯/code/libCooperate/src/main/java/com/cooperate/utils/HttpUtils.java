package com.cooperate.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import net.sf.json.JSONObject;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooperate.shstop.SHStopService;
import com.ec.utils.LogUtil;

/**
 * Created by zangyaoyi on 2016/12/30.
 */
public class HttpUtils {
	private static final Logger logger =  LoggerFactory.getLogger(LogUtil.getLogName(SHStopService.class.getName()));
	/**
     * POST方式发起http请求
     *
     * @param url    要请求的url
     * @param params 请求参数
     * @param token  运营商授权返回的token
     * @return http返回的response的body内容
     */
    public static String httpJSONPost(String url, Map<String, String> params, String token) throws IOException {
    	logger.debug(LogUtil.addExtLog("url|params|token"),new Object[]{url,params,token});
        HttpPost post = new HttpPost(url);
        List<NameValuePair> list = new ArrayList<NameValuePair>();

        if (token != null && token.length() > 0 ) {			
        	post.addHeader("Authorization", token);
        	post.addHeader("Content-Type", "application/json;charset=utf-8");
		}
        
        JSONObject jsonObject = JSONObject.fromObject(params);
        // 设置参数
        StringEntity stringEntity = new StringEntity(jsonObject.toString(), "UTF-8");
        stringEntity.setContentType("application/json;charset=utf-8");
        post.setEntity(stringEntity);
        
        HttpClient httpClient = getHttpClient();
        HttpResponse response = httpClient.execute(post);
        StringBuffer sb = new StringBuffer();
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
	

    /**
     * POST方式发起http请求
     *
     * @param url    要请求的url
     * @param params 请求参数
     * @return http返回的response的body内容
     */
    public static String httpPost(String url, Map<String, String> params) throws IOException {
        HttpPost post = new HttpPost(url);
        List<NameValuePair> list = new ArrayList<NameValuePair>();

        // params中参数放入list
        for (Map.Entry<String, String> entry : params.entrySet()) {
            BasicNameValuePair basicNameValuePair = new BasicNameValuePair(entry.getKey(), entry.getValue());
            list.add(basicNameValuePair);
        }
        post.setEntity(new UrlEncodedFormEntity(list, "UTF-8"));
        
        HttpClient httpClient = getHttpClient();
        HttpResponse response = httpClient.execute(post);
        StringBuffer sb = new StringBuffer();
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    public static String httpPostObject(String url, Map<String, Object> params) throws IOException {
        HttpPost post = new HttpPost(url);
        List<NameValuePair> list = new ArrayList<NameValuePair>();

        // params中参数放入list
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            BasicNameValuePair basicNameValuePair = new BasicNameValuePair(entry.getKey(), (String)entry.getValue());
            list.add(basicNameValuePair);
        }

        post.setEntity(new UrlEncodedFormEntity(list, "UTF-8"));
        HttpClient httpClient = getHttpClient();
        HttpResponse response = httpClient.execute(post);
        StringBuffer sb = new StringBuffer();
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    public static HttpClient getHttpClient() {
        try {
            SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
            schemeRegistry.register(new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));
            ClientConnectionManager ccm = new PoolingClientConnectionManager(schemeRegistry);

            //fixme 此处创建支持https的httpClient对象，但会接受任意的https证书，有安全隐患，生产环境中应避免不对https证书做校验
            SSLContext ctx = SSLContext.getInstance("TLS");
            X509TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] xcs, String string) {
                }

                public void checkServerTrusted(X509Certificate[] xcs, String string) {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            ctx.init(null, new TrustManager[]{tm}, null);
            SSLSocketFactory ssf = new SSLSocketFactory(ctx);
            ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            SchemeRegistry sr = ccm.getSchemeRegistry();
            sr.register(new Scheme("https", ssf, 443));
            return new DefaultHttpClient(ccm);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
