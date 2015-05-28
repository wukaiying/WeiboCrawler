package com.eventwarning.bean;

import java.util.Map;

public class KeyWord {
	public int keywordID;
	public String keyword;
	public int count;
	public int eventID;
	public String wordType;
	
	public KeyWord(Map row){//�����ݿ�������ݣ�ͨ�������н����¼�����
		this.keywordID = Integer.parseInt(row.get("keywordID").toString());
		this.keyword = row.get("keyword").toString();
		this.count = Integer.parseInt(row.get("count").toString());
		this.eventID = Integer.parseInt(row.get("eventID").toString());
		this.wordType = row.get("wordType").toString();
	}
}
