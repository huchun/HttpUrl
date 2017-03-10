package com.example.just.myapplication.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
	private static final String DB_NAME="download.db";
	private static final int VERSION=1;

	private static final String TABLE_CREATE="create table thread_info(_id integer primary key autoincrement," +
			"thread_id integer,url text,start integer,end integer,finished integer)";

	private static final String TABLE_DROP="drop table if exits thread_info";

	private static DBHelper mInstance;

	private DBHelper(Context context) {
		super(context, DB_NAME, null, VERSION);
	}

	public static DBHelper getInstance(Context context) {
		if(mInstance==null) {
			synchronized (DBHelper.class) {
				if(mInstance==null) {
					mInstance=new DBHelper(context);
				}
			}
		}

		return mInstance;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(TABLE_DROP);
		db.execSQL(TABLE_CREATE);
	}
}
