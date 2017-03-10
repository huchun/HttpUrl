package com.example.just.myapplication.services;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;

import com.example.just.myapplication.entities.FileInfo;

import org.apache.http.HttpStatus;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Just on 2016/2/4.
 */
public class DownloadService extends Service{
    public static final String ACTION_DOWNLOAD="DOWNLOAD";
    public static final String ACTION_STOP="STOP";
    public static final String ACTION_UPDATE="UPDATE";
    public static final String ACTION_FINISHED="FINISHED";

    /**
     * 存放下载文件的文件夹路径
     */
    public static final String DOWNLOAD_PATH = Environment
            .getExternalStorageDirectory().getAbsolutePath()
            + "/DownloadsTest/";

    private static final int MSG_INIT=0;//代表创建本地文件完成

    //下载任务的集合
    private Map<Integer,DownloadTask> mDownloadloadTaskMap=new LinkedHashMap<Integer, DownloadTask>();

    /**
     * 在每次服务启动的时候调用
     * 如果我们希望服务一旦启动就立刻去执行某个动作，就可以将逻辑写在onStartCommand()方法里
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //获得Activity传来的参数
        if(ACTION_DOWNLOAD.equals(intent.getAction())) {

            FileInfo fileInfo = (FileInfo) intent
                    .getSerializableExtra("fileInfo");

            // 启动初始化线程
//          new InitThread(fileInfo).start();
            //因为只有执行了初始化线程才会开始执行相应的子线程去下载文件，所以可以将初始化线程放入线程池中，而不用担心子线程先于初始化线程执行
            DownloadTask.sExecutorService.execute(new InitThread(fileInfo));

        }
        else if(ACTION_STOP.equals(intent.getAction())){
            FileInfo fileInfo = (FileInfo) intent
                        .getSerializableExtra("fileInfo");
            DownloadTask task=mDownloadloadTaskMap.get(fileInfo.getId());
            if(task!=null) {
                task.isPause=true;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Handler mHandler=new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch(msg.what) {
                case MSG_INIT:
                    FileInfo fileInfo=(FileInfo) msg.obj;

                    //启动下载任务
                    DownloadTask task=new DownloadTask(DownloadService.this, fileInfo,3);
                    task.download();
                    mDownloadloadTaskMap.put(fileInfo.getId(), task);

                    break;
            }
        }
    };

    /**
     * 从网上读取文件的长度然后再本地建立文件
     */
    private class InitThread extends Thread {
        private FileInfo mFileInfo;

        public InitThread(FileInfo fileInfo) {
            mFileInfo=fileInfo;
        }

        public void run() {
            HttpURLConnection connection=null;
            RandomAccessFile raf=null;
            try {
                //连接网络文件
                URL url=new URL(mFileInfo.getUrl());
                connection=(HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(3000);//设置连接超时
                connection.setReadTimeout(3000);//设置读取超时
                connection.setRequestMethod("GET");

                int length=-1;
                if(connection.getResponseCode()== HttpStatus.SC_OK) {//判断是否成功连接
                    //获得文件长度
                    length=connection.getContentLength();
                }
                if(length<=0) {
                    return;
                }

                File dir=new File(DOWNLOAD_PATH);
                if(!dir.exists()) {//判断存放下载文件的文件的文件夹是否存在
                    dir.mkdir();
                }
                //在本地创建文件
                File file=new File(dir,mFileInfo.getFileName());
                raf=new RandomAccessFile(file,"rwd");//随机存取文件，用于断点续传,r-读取/w-写入/d-删除权限

                //设置本地文件长度
                raf.setLength(length);

                mFileInfo.setLength(length);
                mHandler.obtainMessage(MSG_INIT, mFileInfo).sendToTarget();
            } catch (Exception e) {
            } finally {
                try {
                    raf.close();
                    connection.disconnect();
                } catch (Exception e) {
                }
            }
        }
    }
}
