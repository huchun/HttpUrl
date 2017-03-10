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

	private int mThreadCount=1;//����������߳�����

	private List<DownloadThread> mDownloadThreadList;//���ڹ�������ͬһ�ļ��Ĳ�ͬ���ֵĶ��߳�
	
	public boolean isPause;

	//��Ϊ����������漰���̱߳Ƚ϶࣬ʹ��ϵͳ���̵߳Ĵ�����������ռ�õ�ʱ�䣬�Լ����ܵ�������ռ���൱�ı����ģ������������õ����̳߳��������Ż�
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
			//�������������ÿ���߳����صĳ���
			int length=mFileInfo.getLength()/mThreadCount;
			for(int i=0;i<mThreadCount;i++) {
				//�����߳���Ϣ
				ThreadInfo threadInfo = new ThreadInfo(i, mFileInfo.getUrl(), length * i, length * (i + 1) - 1, 0);
				if(i==mThreadCount-1) {//mFileInfo.getLength()���ܱ�mThreadCount����
					threadInfo.setEnd(mFileInfo.getLength());
				}
				//������߳���Ϣ�ļ�����ȥ
				list.add(threadInfo);

				mDAO.insertThreadInfo(threadInfo);//ֱ�������ｫ�߳���Ϣ�������ݿ��У��Ӷ�������ԭ��һ�������߳���ȥ���������ٶ����ݿ������
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

		//�ж��߳��Ƿ�ִ�����
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

						//�ۼ������ļ�����ɽ���
						mFinished += len;

						//�ۼ�ÿ���߳���ɵĽ���
						mThreadInfo.setFinished(mThreadInfo.getFinished()+len);

						//��Ϊ�����˶���߳�ͬʱ����ͬһ�ļ�������ˢ��UI��Ƶ�ʻ���UI�ĸ��ر�󣬿��ܻᵼ�½��治��Ӧ
						//������������Խ�ԭ����500��Ϊ1000
						//�������ʱ��̫����Ҳ�����һ���������������ͣ��ʱ�����µ�����ص�ʱ��������п��ܻ����һ�ξ���
						//������Ϊ�п�����û�дﵽ�ٴθ���UI��ʱ���û�н��뵽if����У����պð�������ͣ�ͽ����ݱ���������
						//��ʱ������������������Ӧ�����ݲ�һ��
						if(System.currentTimeMillis()-time>1000) {
							time=System.currentTimeMillis();

							intent.putExtra("finished",mFinished*100/mFileInfo.getLength());
							intent.putExtra("fileId", mFileInfo.getId());
							mContext.sendBroadcast(intent);

							//������������һ�д������������߳���Ϣ����������ֹ��δ������ͣ�͹رճ����ʹ���ļ���Ҫ��ͷ��ʼ���ص�����
							//ʵ���������õģ���Ϊ��download������ÿ���߳���Ҫ���ص��ļ������������ļ����ܳ���/�߳�����������
							//���ԣ�Ҫ��ﵽ��Ҫ��Ч������Ҫ�������ܵ��߼�
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
				//����ļ��Ƿ��������������
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
	 * �ж�����ͬһ�ļ���ͬ���ֵĶ���߳��Ƿ�ִ�����
	 * ��Ϊ����ͬһ�ļ���ͬ���ֵĶ���̶߳��п��ܵ��ø÷���,����Ҫͬ��
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

			//���͹㲥֪ͨUI�ļ���ȫ�������߳��Ѿ�����
			Intent intent=new Intent(DownloadService.ACTION_FINISHED);
			intent.putExtra("fileInfo",mFileInfo);
			mContext.sendBroadcast(intent);
		}
	}
}
