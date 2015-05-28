package com.eventwarning.bean;

import java.util.List;
import java.util.Map;

public class Event {
	public int eventID;
	public String startTime;
	public String location;
	public String category;
	private WeiboData centuralWeibo;
	private List<WeiboData> weiboList;
	private List<KeyWord> keywordList;
	public String centuralTF;
	
	public Event(){ }
	public Event(Map row){ //�����ݿ�������ݣ�ͨ�������н����¼�����
		this.eventID = Integer.parseInt(row.get("eventID").toString());
		this.startTime = row.get("startTime").toString();
		this.location = row.get("location").toString();
		this.category = row.get("category").toString();
		this.centuralWeibo = null;
		this.weiboList = null;
		this.keywordList = null;
		this.centuralTF = row.get("centuralTF").toString();
	}
	
	public WeiboData getCenturalWeibo(){
		if(this.centuralWeibo==null)
			; //�����ݿ��ȡ������΢������
		return this.centuralWeibo;
	}
	public List<WeiboData> getWeiboList(){
		if(this.weiboList==null)
			; //�����ݿ��ȡ������΢������
		return this.weiboList;
	}
	public List<KeyWord> getKeyWordList(){
		if(this.keywordList==null)
			; //�����ݿ��ȡ������΢������
		return this.keywordList;
	}
}
