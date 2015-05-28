package com.eventwarning.bean;

import java.util.Map;

public class EventDynamic {
	public int dynamicID;
	private Event event;
	public String updateTime;
	public int weiboNum;
	public int repostNum;
	public int commentNum;
	public int uniqueUserNum;
	public int vUserNum;
	public int postiveNum; //����
	public int neutralNum; //����
	public int negativeNum; //����
	
	public EventDynamic(Map row){
		this.dynamicID = Integer.parseInt(row.get("dynamicID").toString());
		this.event = null;
		this.updateTime = row.get("updateTime").toString();
		this.weiboNum = Integer.parseInt(row.get("weiboNum").toString());
		this.repostNum = Integer.parseInt(row.get("repostNum").toString());
		this.commentNum = Integer.parseInt(row.get("commentNum").toString());
		this.uniqueUserNum = Integer.parseInt(row.get("uniqueUserNum").toString());
		this.vUserNum = Integer.parseInt(row.get("vUserNum").toString());
		String []sentimentNum = row.get("sentimentNum").toString().split(",");
		this.postiveNum = Integer.parseInt(sentimentNum[0]);
		this.neutralNum = Integer.parseInt(sentimentNum[1]);
		this.negativeNum = Integer.parseInt(sentimentNum[2]);
	}
	
	public Event getEvent(){
		if(this.event == null)
			; //�����ݿ���벢���� event����
		return this.event;
	}

}
