package com.example.just.myapplication.services;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.http.HttpStatus;
import android.content.Context;
import android.content.Intent;
import com.example.just.myapplication.db.ThreadDAO;
import com.example.just.myapplication.db.ThreadDAOImpl;
import com.example.just.myapplication.entities.FileInfo;
import com.example.just.myapplication.entities.ThreadInfo;

public class DownloadTask {
	private Context mContext;
	private FileInfo mFileInfo;
	
	private ThreadDAO mDAO;
	
	private int mFinished;

	private int mThreadCount=1;//下载任务的线程数量

	private List<DownloadThread> mDownloadThreadList;//用于管理下载同一文件的不同部分的多线程
	
	public boolean isPause;

	//因为这个程序中涉及的线程比较多，使得系统对线程的创建、销毁所占用的时间，以及性能的消耗是占了相当的比例的，所以在这里用到了线程池来进行优化
	public static ExecutorService sExecutorService= Executors.newCachedThreadPool();
	
	public DownloadTask(Context context, FileInfo fileInfo,int threadCount) {
		super();
		this.mContext = context;
		this.mFileInfo = fileInfo;
		mDAO=new ThreadDAOImpl(context);
		isPause=false;
		mThreadCount=threadCount;
	}

	public String toString() {
		return mFileInfo.toString()+mThreadCount;
	}
	
	public void download() {
		List<ThreadInfo> list=mDAO.getThreads(mFileInfo.getUrl());
		if(list==null||list.size()==0) {
			//获得下载任务中每个线程下载的长度
			int length=mFileInfo.getLength()/mThreadCount;
			for(int i=0;i<mThreadCount;i++) {
				//创建线程信息
				ThreadInfo threadInfo = new ThreadInfo(i, mFileInfo.getUrl(), length * i, length * (i + 1) - 1, 0);
				if(i==mThreadCount-1) {//mFileInfo.getLength()不能被mThreadCount整除
					threadInfo.setEnd(mFileInfo.getLength());
				}
				//添加至线程信息的集合中去
				list.add(threadInfo);

				mDAO.insertThreadInfo(threadInfo);//直接在这里将线程信息插入数据库中，从而避免像原来一样在子线程中去操作，减少对数据库的锁定
			}
		}
		mDownloadThreadList=new ArrayList<DownloadThread>();
		for (ThreadInfo info:list) {
			DownloadThread thread=new DownloadThread(info);

			DownloadTask.sExecutorService.execute(thread);

			mDownloadThreadList.add(thread);
		}
	}

	class DownloadThread extends Thread {
		private ThreadInfo mThreadInfo;

		//判断线程是否执行完毕
		public boolean isFinishedForThread=false;

		public DownloadThread(ThreadInfo ThreadInfo) {
			mThreadInfo=ThreadInfo;
		}
		
		public void run() {
			HttpURLConnection connection=null;
			RandomAccessFile raf=null;
			InputStream input=null;
			try {
				URL url=new URL(mThreadInfo.getUrl());
				connection=(HttpURLConnection) url.openConnection();
				connection.setConnectTimeout(3000);
				connection.setRequestMethod("GET");

				int start=mThreadInfo.getFinished();

				connection.setRequestProperty("Range", "bytes="+start+"-"+mThreadInfo.getEnd());

				File file=new File(DownloadService.DOWNLOAD_PATH,mFileInfo.getFileName());
				raf=new RandomAccessFile(file, "rwd");
				raf.seek(start);

				Intent intent=new Intent(DownloadService.ACTION_UPDATE);

				mFinished=start;

				if(connection.getResponseCode()==HttpStatus.SC_PARTIAL_CONTENT) {

					input=connection.getInputStream();
					byte[] buffer=new byte[1024*4];
					int len=-1;

					long time=System.currentTimeMillis();

					while((len=input.read(buffer))!=-1) {
						raf.write(buffer,0,len);

						//累加整个文件的完成进度
						mFinished += len;

						//累加每个线程完成的进度
						mThreadInfo.setFinished(mThreadInfo.getFinished()+len);

						//因为开启了多个线程同时下载同一文件，所以刷新UI的频率会变大，UI的负载变大，可能会导致界面不响应
						//所在在这里可以将原来的500改为1000
						//但是相隔时间太长，也会造成一种情况，当按下暂停的时候，重新点击下载的时候进度条有可能会回退一段距离
						//这是因为有可能在没有达到再次更新UI的时间就没有进入到if语句中，而刚好按下了暂停就将数据保存了起来
						//此时保存的数据与进度条对应点数据不一致
						if(System.currentTimeMillis()-time>1000) {
							time=System.currentTimeMillis();

							intent.putExtra("finished",mFinished*100/mFileInfo.getLength());
							intent.putExtra("fileId", mFileInfo.getId());
							mContext.sendBroadcast(intent);

							//如果在这里添加一行代码用来更新线程信息的数据来防止在未按下暂停就关闭程序而使得文件需要从头开始下载的问题
							//实际上是无用的，因为在download方法中每个线程需要下载的文件长度总是由文件的总长度/线程数量决定的
							//所以，要想达到需要的效果，需要更加严密的逻辑
						}

						if(isPause) {
							mDAO.updateThreadInfo(mThreadInfo.getUrl(), mThreadInfo.getId(), mThreadInfo.getFinished());
							raf.close();
							input.close();
							connection.disconnect();
							return;
						}
					}
				}

				isFinishedForThread=true;
				//检查文件是否完整的下载完成
				checkAllThreadFinished();
			} catch (Exception e) {
			} finally {
				try {
					raf.close();
					input.close();
					connection.disconnect();
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * 判断下载同一文件不同部分的多个线程是否都执行完毕
	 * 因为下载同一文件不同部分的多个线程都有可能调用该方法,所以要同步
	 */
	private synchronized void checkAllThreadFinished() {
		boolean allFinished=true;

		for(DownloadThread thread:mDownloadThreadList) {
			if(!thread.isFinishedForThread) {
				allFinished=false;
				break;
			}
		}

		if(allFinished) {
			mDAO.deleteThreadInfo(mFileInfo.getUrl());

			//发送广播通知UI文件的全部下载线程已经结束
			Intent intent=new Intent(DownloadService.ACTION_FINISHED);
			intent.putExtra("fileInfo",mFileInfo);
			mContext.sendBroadcast(intent);
		}
	}
}
