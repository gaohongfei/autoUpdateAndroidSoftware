package com.test.up_soft;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import android.os.*;
import org.xml.sax.InputSource;

import com.test.up_soft.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

public class DownloadService extends Service 
{
	private static final int NOTIFY_DOW_ID = 0;
	private static final int NOTIFY_OK_ID = 1;

    private Context mContext = this; 
    private boolean cancelled;  
    private int progress; 
    private NotificationManager mNotificationManager;  
    private Notification mNotification;  
    private DownloadBinder binder = new DownloadBinder();  
    
    private String pastVersion;													//旧的本号
//    private String serverUrl = "http://192.168.1.30:800/tcm_android_web/apk/";	//服务器下载地址
    private String serverUrl = "http://www.gaohf.com/ctuc/auto_update/";	//服务器下载地址
//    private String serverUrl = "http://10.3.88.53/";	//服务器下载地址
    private String xmlName ="versionInfo.xml";									//XML文件名
    
    private int fileSize;		//文件大小
    private int readSize;		//读取长度
    private int downSize;		//已下载大小
    private File downFile;		//下载的文件
    
    private Map<String, String> versionInfo;	//版本信息
	public enum versionInfoField
	{
		filename, filetype, version, description
	}

    private Handler handler = new Handler()
    {  
        public void handleMessage(android.os.Message msg) 
        {  
            switch (msg.what) 
            {  
            	case 0:
            		// 更新进度
	    			RemoteViews contentView = mNotification.contentView;
	            	contentView.setTextViewText(R.id.rate, (readSize < 0 ? 0 : readSize) + "b/s   " + msg.arg1 + "%");  
	            	contentView.setProgressBar(R.id.progress, 100, msg.arg1, false);  
	
	            	// 更新UI
	            	mNotificationManager.notify(NOTIFY_DOW_ID, mNotification);
            		
                    break;
            	case 1:
            		mNotificationManager.cancel(NOTIFY_DOW_ID);
            		createNotification(NOTIFY_OK_ID);
            		
                	/*打开文件进行安装*/
                	openFile(downFile);
	                break;
            	case 2:
            		mNotificationManager.cancel(NOTIFY_DOW_ID);
            		break;
	        }  
        };  
    };  
    
    private Handler handMessage = new Handler()
    {
    	public void handleMessage(Message msg)
    	{
    		switch(msg.what)
    		{
    			case 0:
    				Toast.makeText(mContext, "服务器连接失败，请稍后再试！", Toast.LENGTH_SHORT).show();
    				break;
    			case 1:
    				Toast.makeText(mContext, "服务器端文件不存在，下载失败！", Toast.LENGTH_SHORT).show();
    				break;    				
    		}
    		
    		handler.sendEmptyMessage(2);
    	}
    };
  
    @Override  
    public void onCreate() 
    {  
        super.onCreate();  
        mNotificationManager = (NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        cancelled = true;
    }  
  
    @Override  
    public IBinder onBind(Intent intent)
    {  
        // 返回自定义的DownloadBinder实例   
        return binder;  
    }  
  
    @Override  
    public void onDestroy() 
    {  
        super.onDestroy();  
        cancelled = true; // 取消下载线程   
    }  
      
    /** 
     * 创建通知 
     */  
    private void createNotification(int notifyId) 
    {
    	switch(notifyId)
    	{
    		case NOTIFY_DOW_ID:
    	        int icon = R.drawable.icon;  
    	        CharSequence tickerText = "开始下载";  
    	        long when = System.currentTimeMillis();  
    	        mNotification = new Notification(icon, tickerText, when);  
    	  
    	        // 放置在"正在运行"栏目中   
    	        mNotification.flags = Notification.FLAG_ONGOING_EVENT;  
    	  
    	        RemoteViews contentView = new RemoteViews(mContext.getPackageName(), R.layout.download_notification_layout);  
    	        contentView.setTextViewText(R.id.fileName, "正在下载：" + versionInfo.get(versionInfoField.filename.toString()) +
    	    			"." + versionInfo.get(versionInfoField.filetype.toString()));  
    	        
    	        // 指定个性化视图   
    	        mNotification.contentView = contentView;  
    	  
    	        Intent intent = new Intent(this, MainForm.class);  
    	        PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);  
    	        // 指定内容意图   
    	        mNotification.contentIntent = contentIntent;  
    	        
    			break;
    		case NOTIFY_OK_ID:     //  int 1
    			int icon2 = R.drawable.icon;  
    	        CharSequence tickerText2 = "下载完毕";  
    	        long when2 = System.currentTimeMillis();  
    	        mNotification = new Notification(icon2, tickerText2, when2);  
    	        
    	        //放置在"通知形"栏目中   
    	        mNotification.flags = Notification.FLAG_AUTO_CANCEL;
    	  //      PendingIntent contentInten2 = PendingIntent.getActivity(mContext, 0, null, 0);
    	    //    mNotification.setLatestEventInfo(mContext, "下载完成", "文件已下载完毕", contentInten2);
                stopSelf();//停掉服务自身   
                Toast.makeText(DownloadService.this, "下载完成", Toast.LENGTH_SHORT).show();
            	
                cancelled = true;
                break;
    	}
    	
    	// 最后别忘了通知一下,否则不会更新   
//        mNotificationManager.notify(notifyId, mNotification);
    }  
  
    /** 
     * 下载模块 
     */  
    private void startDownload()
    {  
    	String dowUrl = serverUrl + versionInfo.get(versionInfoField.filename.toString())
		+ "." + versionInfo.get(versionInfoField.filetype.toString());

    	//初始化数据
    	fileSize = 0;
    	readSize = 0;
    	downSize = 0;
    	progress = 0;
    	
    	InputStream is = null;
        FileOutputStream fos = null;
//    	DataOutputStream fos = null;
    	
    	try 
    	{
    		URL myURL = new URL(dowUrl+".doc");						//取得URL
        	URLConnection conn = myURL.openConnection();		//建立联机
            conn.connect();
        	fileSize = conn.getContentLength();					//获取文件长度
        	is = conn.getInputStream();   						//InputStream 下载文件
        	
        	if (is == null) 
        	{  
        		Log.d("tag","error");
        		throw new RuntimeException("stream is null"); 
        	}
        	String temPath = "gaohongfei_apk";
        	//建立临时文件 
//        	downFile = File.createTempFile(versionInfo.get(versionInfoField.filename.toString()), "." + versionInfo.get(versionInfoField.filetype.toString()),getDir(temPath, MODE_WORLD_WRITEABLE+MODE_WORLD_READABLE));
        	downFile = File.createTempFile(versionInfo.get(versionInfoField.filename.toString()), "." + versionInfo.get(versionInfoField.filetype.toString()));


//        	downFile = File.createTempFile("ctuc","apk",getDir(temPath, MODE_PRIVATE) );
//            fos = openFileOutput("CTUC.apk",MODE_WORLD_READABLE+MODE_WORLD_WRITEABLE);
        	//将文件写入临时盘
        	fos = new FileOutputStream(downFile);
//            fos = new DataOutputStream(this.openFileOutput("ctuc.apk", MODE_WORLD_READABLE+MODE_WORLD_WRITEABLE));
        	byte buf[] = new byte[1024 * 1024];
        	while (!cancelled && (readSize = is.read(buf)) > 0) 
        	{   
        		fos.write(buf, 0, readSize);
        		downSize += readSize;
                
        		sendMessage(0);
        	}
        	
        	if(cancelled)
        	{
        		handler.sendEmptyMessage(2);
        		downFile.delete();
        	}
        	else
        	{
        		handler.sendEmptyMessage(1);
        	}
		}
    	catch (MalformedURLException e) 
    	{
			handMessage.sendEmptyMessage(0);
		} 
    	catch (IOException e) 
    	{
			handMessage.sendEmptyMessage(1);
		}  
        catch (Exception e)
        {
			handMessage.sendEmptyMessage(0);
		}
        finally
        {
        	try 
        	{
        		if(null != fos)	fos.close();
				if(null != is)	is.close();
			} 
        	catch (IOException e) 
			{
				e.printStackTrace();
			}
        }
    }
    
    public void sendMessage(int what)
    {
    	int num = (int)((double)downSize / (double)fileSize * 100);
    	    	
    	if(num > progress + 1)
    	{
			progress = num;
			
		    Message msg0 = handler.obtainMessage();  
		    msg0.what = what;  
		    msg0.arg1 = progress;
		    handler.sendMessage(msg0);
    	}
    }
    
	//在手机上打开文件的method 
    private void openFile(File f) 
    {
    	Intent intent = new Intent();
    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	intent.setAction(android.content.Intent.ACTION_VIEW);
      
    	//调用getMIMEType()来取得MimeType 
    	String type = getMIMEType(f);
    	//设定intent的file与MimeType 
    	intent.setDataAndType(Uri.fromFile(f),type);
//    	intent.setDataAndType(Uri.parse("file:///data/data/com.test.up_soft/files/ctuc.apk"),"application/vnd.android.package-archive");
    	startActivity(intent);
    }
    
    //判断文件MimeType的method 
    private String getMIMEType(File f) 
    { 
        String type = "";
        String fName = f.getName();
        // 取得扩展名 
        String end=fName.substring(fName.lastIndexOf(".")+1,fName.length()).toLowerCase(); 

        // 按扩展名的类型决定MimeType 
        if(end.equals("m4a")||end.equals("mp3")||end.equals("mid")||end.equals("xmf")||end.equals("ogg")||end.equals("wav"))
        {
        	type = "audio"; 
        }
        else if(end.equals("3gp")||end.equals("mp4"))
        {
        	type = "video";
        }
        else if(end.equals("jpg")||end.equals("gif")||end.equals("png")||end.equals("jpeg")||end.equals("bmp"))
        {
        	type = "image";
        }
        else if(end.equals("apk"))
        { 
        	// android.permission.INSTALL_PACKAGES 
        	type = "application/vnd.android.package-archive"; 
        } 
        else
        {
        	type="*";
        }
        //如果无法直接打开，就跳出软件清单给使用者选择 
        if(!end.equals("apk")) 
        { 
        	type += "/*";  
        } 
        
        return type;  
	}
    
    //SAX解析resourceUrl 页面的内容
    public Map<String, String> getXMLElements(String resourceUrl) throws MalformedURLException, IOException, Exception
    {
    	//获取XML
		URL url = new URL(resourceUrl);
		InputSource is = new InputSource(url.openStream()); 
        is.setEncoding("UTF-8");
        
        //解析XML
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser saxParser = spf.newSAXParser(); 	//创建解析器
        ParsingXMLElements handler = new ParsingXMLElements();
        saxParser.parse(is, handler);
        return handler.getElement();
    }
    
    /** 
     * DownloadBinder中定义了一些实用的方法 
     * @author user 
     */  
    public class DownloadBinder extends Binder
    {
        /** 
         * 开始
         */  
        public void start() 
        {  
            cancelled = false;
            new Thread() 
            {  
                public void run() 
                {  
                    createNotification(NOTIFY_DOW_ID);		//创建通知   
                    startDownload();	//下载   
                    cancelled = true; 
                };  
            }.start();
        }  
  
        /** 
         * 获取进度 
         *  
         * @return 
         */  
        public int getProgress() 
        {  
            return progress;  
        }  
  
        /** 
         * 取消下载 
         */  
        public void cancel() 
        {  
            cancelled = true;  
        }  
  
        /** 
         * 是否已被取消 
         *  
         * @return 
         */  
        public boolean isCancelled() 
        {  
            return cancelled;  
        }
        
        /**
         * 检查当前系统是否是最新版本
         * @return 1：当前最新版本 0：发现新的版本可更新 -1：检测新版本失败
         */
        public int isNewVersion()
        {
        	try 
        	{
        		versionInfo = getXMLElements(serverUrl + xmlName);
        		pastVersion = getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
            	
            	return null == versionInfo || null == versionInfo.get("versionCode") || null == pastVersion
            		|| pastVersion.equals(versionInfo.get(versionInfoField.version.toString())) ? 1 : 0;
			} 
        	catch (NameNotFoundException e) 
			{
				e.printStackTrace();
			} 
        	catch (MalformedURLException e) 
        	{
				e.printStackTrace();
			} 
        	catch (IOException e) 
        	{
				e.printStackTrace();
				handMessage.sendEmptyMessage(0);
			} 
        	catch (Exception e) 
			{
				e.printStackTrace();
			}
        	
        	return -1;
        }
        
        /**
         * 获取最新版本的信息
         * @return
         */
        public Map<String, String> getVersionInfo()
        {
        	return versionInfo;
        }
    }
}
