package com.example.just.myapplication.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.just.myapplication.entities.ThreadInfo;

import java.util.ArrayList;
import java.util.List;

public class ThreadDAOImpl implements ThreadDAO {
	private DBHelper mHelper;
	
	public ThreadDAOImpl(Context context) {
		mHelper=DBHelper.getInstance(context);
	}

	@Override
	public synchronized void insertThreadInfo(ThreadInfo threadInfo) {
		SQLiteDatabase db=mHelper.getWritableDatabase();
		db.execSQL(
				"insert into thread_info(thread_id,url,start,end,finished) values(?,?,?,?,?)",
				new Object[] { threadInfo.getId(), threadInfo.getUrl(),
						threadInfo.getStart(), threadInfo.getEnd(),
						threadInfo.getFinished() });
		db.close();
	}

	@Override
	public synchronized void deleteThreadInfo(String url) {
		SQLiteDatabase db=mHelper.getWritableDatabase();
		db.execSQL("delete from thread_info where url = ?",
				new Object[]{url});
		db.close();
	}

	@Override
	public synchronized void updateThreadInfo(String url, int threadId,int finished) {
		SQLiteDatabase db=mHelper.getWritableDatabase();
		db.execSQL("update thread_info set finished = ? where url = ? and thread_id = ?",
				new Object[]{finished,url,threadId});
		db.close();
	}

	@Override
	public List<ThreadInfo> getThreads(String url) {
		List<ThreadInfo> list=null;
		SQLiteDatabase db=mHelper.getReadableDatabase();
		Cursor cursor=db.rawQuery("select * from thread_info where url = ?",new String[]{url});
		if (cursor != null) {
			list=new ArrayList<ThreadInfo>();
			while (cursor.moveToNext()) {
				ThreadInfo temp = new ThreadInfo();
				temp.setId(cursor.getInt(cursor.getColumnIndex("thread_id")));
				temp.setUrl(cursor.getString(cursor.getColumnIndex("url")));
				temp.setStart(cursor.getInt(cursor.getColumnIndex("start")));
				temp.setEnd(cursor.getInt(cursor.getColumnIndex("end")));
				temp.setFinished(cursor.getInt(cursor
						.getColumnIndex("finished")));
				list.add(temp);
			}
			cursor.close();
		}
		db.close();
		return list;
	}

	@Override
	public boolean isExists(String url, int threadId) {
		SQLiteDatabase db=mHelper.getReadableDatabase();
		Cursor cursor=db.rawQuery("select * from thread_info where url = ? and thread_id = ?",new String[]{url,""+threadId});
		if(cursor!=null) Log.d("测试","cursor!=null "+cursor.moveToNext());
		boolean exists=false;
		if(cursor!=null) {
			exists=cursor.moveToNext();
		}
		cursor.close();
		db.close();//可能是由于对数据库的操作未作同步操作
		if(db!=null) Log.d("测试","db!=null");
		return exists;
	}
}
