package com.example.just.myapplication;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import com.example.just.myapplication.entities.FileInfo;
import com.example.just.myapplication.services.DownloadService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private ListView mListView;
    private List<FileInfo> mFileInfoList;
    private FileListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mListView = (ListView) findViewById(R.id.id_listview);
        mFileInfoList=initFileInfoList();
        mAdapter=new FileListAdapter(this,mFileInfoList);
        mListView.setAdapter(mAdapter);

        //注册广播接收器
        IntentFilter filter=new IntentFilter();
        filter.addAction(DownloadService.ACTION_UPDATE);
        filter.addAction(DownloadService.ACTION_FINISHED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private List<FileInfo> initFileInfoList() {
        List<FileInfo> list=new ArrayList<FileInfo>();

        FileInfo aFileInfo=new FileInfo(0,"http://www.imooc.com/mobile/imooc.apk","imooc0.apk");
        FileInfo bFileInfo=new FileInfo(1,"http://www.imooc.com/download/Activator.exe","Activator.exe");
        FileInfo cFileInfo=new FileInfo(2,"http://www.imooc.com/download/iTunes64Setup.exe","iTunes64Setup.exe");
        FileInfo dFileInfo=new FileInfo(3,"http://www.imooc.com/download/BaiduPlayerNetSetup_100.exe","BaiduPlayerNetSetup_100.exe");
        FileInfo eFileInfo=new FileInfo(3,"http://www.imooc.com/download/iTunes_x32_12.1.1.4.1424917513.exe","iTunes_x32_12.1.1.4.1424917513.exe");
        FileInfo fFileInfo=new FileInfo(3,"http://www.imooc.com/download/Evernote_5.8.3.6507.exe","Evernote_5.8.3.6507.exe");

        list.add(aFileInfo);
        list.add(bFileInfo);
        list.add(cFileInfo);
        list.add(dFileInfo);
        list.add(eFileInfo);
        list.add(fFileInfo);

        return list;
    }

    /**
     * 更新UI广播的广播接收器
     */
    BroadcastReceiver mReceiver=new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(DownloadService.ACTION_UPDATE)) {
                int finished=intent.getIntExtra("finished",0);
                int id=intent.getIntExtra("fileId",-1);
                if(id!=-1) {
                    mAdapter.updateProgress(id,finished);
                }
            }
            else if(intent.getAction().equals(DownloadService.ACTION_FINISHED)) {
                FileInfo fileInfo= (FileInfo) intent.getSerializableExtra("fileInfo");

                mAdapter.updateProgress(fileInfo.getId(),100);
                Toast.makeText(MainActivity.this,fileInfo.getFileName()+"下载完毕",Toast.LENGTH_SHORT).show();
            }
        }
    };
}
