package com.example.just.myapplication;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.example.just.myapplication.entities.FileInfo;
import com.example.just.myapplication.services.DownloadService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件列表适配器
 */
public class FileListAdapter extends BaseAdapter {
    private Context mContext;
    private List<FileInfo> mFileInfoList;
    LayoutInflater mInflater;

    //用来判断ListView中相应的item是否正在下载还是暂停了下载任务
    //因为getView中的position可能不是顺序的，所以在这里用Map
    public Map<Integer,Boolean> isDownloadingMap=new HashMap<Integer,Boolean>();

    public FileListAdapter(Context context,List<FileInfo> fileInfoList) {
        mContext=context;
        mFileInfoList=fileInfoList;
        mInflater=LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        return mFileInfoList.size();
    }

    @Override
    public Object getItem(int position) {
        return mFileInfoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder=null;
        final FileInfo fileInfo=mFileInfoList.get(position);
        if(convertView==null) {
            if(isDownloadingMap.get(position)==null)
                isDownloadingMap.put(position,false);

            holder=new ViewHolder();
            convertView=mInflater.inflate(R.layout.item_list,null);
            holder.tvFileName= (TextView) convertView.findViewById(R.id.id_fileName);
            holder.btDown= (Button) convertView.findViewById(R.id.id_download);
            holder.btStop= (Button) convertView.findViewById(R.id.id_stop);
            holder.progressBar= (ProgressBar) convertView.findViewById(R.id.id_progressBar);

            convertView.setTag(holder);
        }
        else {
            if(isDownloadingMap.get(position)==null)
                isDownloadingMap.put(position,false);
            holder= (ViewHolder) convertView.getTag();
        }
        holder.tvFileName.setText(fileInfo.getFileName());
        holder.progressBar.setMax(100);
        holder.btDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isDownloadingMap.get(position)) {
                    Intent intent = new Intent(mContext, DownloadService.class);
                    intent.setAction(DownloadService.ACTION_DOWNLOAD);
                    intent.putExtra("fileInfo", fileInfo);
                    mContext.startService(intent);
                    isDownloadingMap.put(position, true);
                    Log.d("测试", "下载");
                }
            }
        });
        holder.btStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isDownloadingMap.get(position)) {
                    Intent intent = new Intent(mContext, DownloadService.class);
                    intent.setAction(DownloadService.ACTION_STOP);
                    intent.putExtra("fileInfo", fileInfo);
                    mContext.startService(intent);
                    isDownloadingMap.put(position, false);
                    Log.d("测试", "暂停");
                }
            }
        });

        holder.progressBar.setProgress(0);
        holder.progressBar.setProgress(fileInfo.getFinshed());
        return convertView;
    }

    /**
     * 更新ListView中item的进度条
     */
    public void updateProgress(int fileId,int progress) {
        FileInfo fileInfo=mFileInfoList.get(fileId);
        //因为ListView中的item就是根据文件的id来排序的，所以可以直接用id从mFileInfoList获取相应的FileInfo

        fileInfo.setFinshed(progress);

        //更新ListView
        notifyDataSetChanged();
    }


    private static class ViewHolder {
        TextView tvFileName;
        Button btDown;
        Button btStop;
        ProgressBar progressBar;
    }
}
