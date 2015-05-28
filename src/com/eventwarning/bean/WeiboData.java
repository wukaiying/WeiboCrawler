package com.eventwarning.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dbOptions.DBOptions;

public class WeiboData {
	public String weiboID;
	public String userID;
	public int eventID;
	public String content;
	public int commentNum;
	public int repostNum;
	public String createAt;
	public String collectAt;
	public List<WeiboComment> commentList;
	public double sentiment;
	
	public WeiboData(Map row){
		this.weiboID = row.get("weiboID").toString();
		this.userID = row.get("userID").toString();
		if(row.get("eventID")!=null)this.eventID = Integer.valueOf(row.get("eventID").toString());
		else this.eventID = -1;
		this.content = row.get("content").toString();
		this.commentNum = Integer.parseInt(row.get("commentNum").toString());
		this.repostNum = Integer.parseInt(row.get("repostNum").toString());
		this.createAt = row.get("createAt").toString();
		this.collectAt = row.get("collectAt").toString();
		this.commentList = null;
		if(row.get("sentiment")!=null)this.sentiment = Double.parseDouble(row.get("sentiment").toString());
		else this.sentiment = 0;
	}
	
	
	
	public WeiboData(Map row,List<WeiboComment> aCommentList){
		this.weiboID = row.get("weiboID").toString();
		this.userID = row.get("userID").toString();
		if(row.get("eventID")!=null)this.eventID = Integer.valueOf(row.get("eventID").toString());
		else this.eventID = -1;
		this.content = row.get("content").toString();
		this.commentNum = Integer.parseInt(row.get("commentNum").toString());
		this.repostNum = Integer.parseInt(row.get("repostNum").toString());
		this.createAt = row.get("createAt").toString();
		this.collectAt = row.get("collectAt").toString();
		this.commentList = aCommentList;
		if(row.get("sentiment")!=null)this.sentiment = Double.parseDouble(row.get("sentiment").toString());
		else this.sentiment = 0;
	}
	
	public int getCommentList(){
		this.commentList = new ArrayList<WeiboComment>();
		//DBOptions.getCommentsByWBId(this.weiboID, this.commentList);
			return 0;
	}
	
	public static String GetWeiboUrl(String weiboID){
		return null;
	}

}
