package com.example.just.myapplication.entities;

import java.io.Serializable;

/**
 * 文件实体类
 * 将该类序列化后（即实现Serializable接口）
 * 就可以在intent中进行传递
 * F:\AndroidWorkspace\DownloadDemo\bin
 *
 */
public class FileInfo implements Serializable{
	private int id;
	private String url;
	private String fileName;
	/**
	 * 文件的大小
	 */
	private int length;
	/**
	 * 文件的下载进度
	 */
	private int finshed;

	public FileInfo() {
	}

	public FileInfo(int id, String url, String fileName) {
		this.id = id;
		this.url = url;
		this.fileName = fileName;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getFinshed() {
		return finshed;
	}

	public void setFinshed(int progress) {
		this.finshed = progress;
	}

	@Override
	public String toString() {
		return "FileInfo [id=" + id + ", url=" + url + ", fileName=" + fileName
				+ ", length=" + length + ", progress=" + finshed + "]";
	}
}
