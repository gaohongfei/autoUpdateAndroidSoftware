package com.test.up_soft;

import com.test.up_soft.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainForm extends Activity
{    
    private DownloadService.DownloadBinder binder;  //内部类的
    private TextView text;
    private ProgressBar progress;
    private Context mContext = this;

    private Handler handler = new Handler()
    {  
        public void handleMessage(android.os.Message msg)
        {  
            text.setText("downloading..." + msg.arg1 + "%");
            progress.setProgress(msg.arg1);
        };  
    };  

    private ServiceConnection conn = new ServiceConnection() 
    {
        @Override  
        public void onServiceConnected(ComponentName name, IBinder service) 
        {
        	binder = (DownloadService.DownloadBinder)service;
        }  
  
        @Override  
        public void onServiceDisconnected(ComponentName name) { }
    };  

    @Override  
    public void onCreate(Bundle savedInstanceState) 
    {  
        super.onCreate(savedInstanceState);  
        setContentView(R.layout.main);  
        text = (TextView) findViewById(R.id.text); 
        progress = (ProgressBar)findViewById(R.id.pbProgress);
        
        Intent intent = new Intent(this, DownloadService.class);  
        startService(intent);   //如果先调用startService,则在多个服务绑定对象调用unbindService后服务仍不会被销毁   
        bindService(intent, conn, Context.BIND_AUTO_CREATE);  
    }  
  
    @Override  
    protected void onDestroy() 
    {  
        super.onDestroy();  
        if (null != binder)
        {  
            unbindService(conn);              
        }
    }
    
    public void cancel(View view)
    {
    	if(null != binder && !binder.isCancelled())
    	{
    		binder.cancel();
    	}
    }
  
    /** 
     * 监听进度 
     */  
    private void listenProgress() 
    {  
        new Thread()
        {  
            public void run()
            {  
                while (binder.getProgress() <= 100) 
                {  
                    int progress = binder.getProgress();  
                    Message msg = handler.obtainMessage();  
                    msg.arg1 = progress;  
                    handler.sendMessage(msg);  
                    
                    if (progress == 100)
                    {  
                        break;  
                    }  
                    
                    try 
                    {  
                        Thread.sleep(200);  
                    }
                    catch (InterruptedException e) 
                    {  
                        e.printStackTrace();  
                    }  
                }  
            };  
        }.start();  
    }  

    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        menu.add(Menu.NONE, Menu.FIRST + 1, 5, "检测更新").setIcon(android.R.drawable.ic_menu_upload);
        menu.add(Menu.NONE,Menu.FIRST+2,4,"退出").setIcon(android.R.drawable.ic_menu_delete);
        return true;
    }
    
    protected final Handler hand = new Handler()
	{
		public void handleMessage(Message msg)
		{
			super.handleMessage(msg);
			
			switch(msg.what)
			{
				case 0:
					String mgs = binder.getVersionInfo().get(DownloadService.versionInfoField.description.toString());
					
					new AlertDialog.Builder(mContext).setTitle("发现新的版本可更新")
	        		.setMessage(mgs)
	        		.setPositiveButton("确定", new DialogInterface.OnClickListener() 
	        		{
	        			public void onClick(DialogInterface dialog, int which) 
	        			{
	        				binder.start();
	        				listenProgress();
	        			}
	        		})
	        		.setNegativeButton("取消",null).create().show();
					break;
				case 1:
		    		Toast.makeText(mContext, "您当前已经是最新版本！", Toast.LENGTH_SHORT).show();
		    		break;
				case 2:
		    		Toast.makeText(mContext, "最新版本正在下载！", Toast.LENGTH_SHORT).show();
		    		break;
					
			};
		}
	};

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	switch(item.getItemId() - Menu.FIRST)
    	{
    		case 1:
    			final ProgressDialog pd = new ProgressDialog(mContext);
    			pd.setMessage("正在检测是否有新的版本...");
    			pd.setCancelable(false);
    			pd.show();
    			
    			new Thread()
    			{
    				public void run()
    				{
    					if(!binder.isCancelled()) 
    					{
    						binder.start();
    						hand.sendEmptyMessage(3);
    			    		return;
    					}
    					
    	    			if(binder.isNewVersion() == 0)
    	    			{
    	    				hand.sendEmptyMessage(0);
    				    }
    			    	else
    			    	{
    			    		hand.sendEmptyMessage(1);
    			    	}
    	    			
    	    			pd.dismiss();
    				}
    			}.start();
    			break;
    		case 2:
    			finish();
    			break;
    	}
    	
    	return super.onOptionsItemSelected(item);
    }
}