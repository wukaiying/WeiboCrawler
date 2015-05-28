package crawler;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.eventwarning.bean.UserInfo;
import com.eventwarning.bean.WeiboComment;
import com.eventwarning.bean.WeiboData;

import crawler.WBPage;
import dbOptions.DBOptions;



public class WBCrawler {
	public Map<String,WeiboData> weiboList;
	public Map<String,UserInfo> userList;
	
	private static ArrayList<String> keyWords=  new ArrayList<String>();
	static{
		getWords("files//searchKeyWords.txt",keyWords);
	}
	
	public WBCrawler(){
		weiboList = new HashMap<String,WeiboData>();
		userList = new HashMap<String,UserInfo>();
	}
	public static void main(String[] args) throws IOException, InterruptedException{
		WBCrawler aCrawler = new WBCrawler();
		while(true){
			
		DBOptions.connect();
		
		for(int i=0;i<WBCrawler.keyWords.size();i++){
			
			WBPage.getWbSearchResult(WBCrawler.keyWords.get(i), aCrawler.weiboList, aCrawler.userList);
			System.out.println(WBCrawler.getCurTimeStr()+"=====》关键字:"+keyWords.get(i)+",微博数量:"+aCrawler.weiboList.size());
			System.out.println("正在抓取微博所对应的评论");
			Iterator<Entry<String, WeiboData>> it = aCrawler.weiboList.entrySet().iterator();
			while(it.hasNext()){
				Entry<String,WeiboData>data = it.next();
				String curWbID = data.getKey();
				if(data.getValue().commentNum>0){
					WBPage.getWeiboComments(curWbID,data.getValue(), aCrawler.userList);
				}
			}
			System.out.println("关键词:"+keyWords.get(i)+"评论抓取完毕");
			System.out.println("正在存储……");
			
			DBOptions.saveUserInfoList(aCrawler.userList);
			DBOptions.saveWeiboList(aCrawler.weiboList);
		}
		Thread.sleep(60*2000);
		}
	}
	
	
	public static String getCurTimeStr(){
		Timestamp t = new Timestamp(System.currentTimeMillis());
		return t.toString();
	}
	
	
	public static void getWords(String fileName,ArrayList<String> wordList){
    	try{
    		BufferedReader br = new BufferedReader(new FileReader(fileName));
    		String aWord = null;
    		while((aWord = br.readLine())!=null){
    			aWord = aWord.trim();
    			if(aWord!=null&&!wordList.contains(aWord))
    				wordList.add(aWord);
    		}
    		br.close();
    	}catch(Exception e){
    		System.out.println(e.getMessage());
    		System.exit(0);
    	}
    }
}
