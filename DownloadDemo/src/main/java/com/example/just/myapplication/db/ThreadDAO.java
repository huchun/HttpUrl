package com.example.just.myapplication.db;

import com.example.just.myapplication.entities.ThreadInfo;
import java.util.List;

public interface ThreadDAO {
	public void insertThreadInfo(ThreadInfo threadInfo);
	public void deleteThreadInfo(String url);
	public void updateThreadInfo(String url, int threadId, int finished);
	public List<ThreadInfo> getThreads(String url);
	public boolean isExists(String url, int threadId);
}
