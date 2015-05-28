package crawler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.eventwarning.bean.UserInfo;
import com.eventwarning.bean.WeiboComment;
import com.eventwarning.bean.WeiboData;

import dbOptions.DBOptions;


public class WBPage {
	public static Map<String,String> cookies;			//cookies
	public static String hAccept = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8";
	public static String hAcceptEncoding = "gzip, deflate, sdch";
	private static String userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.89 Safari/537.36";
	public static Document curDoc;
	private static boolean isValidDoc;
	private static ArrayList<String> users;
	private static int curUserIndex = 0;
	static{
		cookies = new HashMap<String,String>();
		users = new ArrayList<String>();
		getUsers();
	}
	
	//获得搜索结果
	public static void getWbSearchResult(String keyWord, Map<String,WeiboData> wbList,Map<String,UserInfo> userList){
		wbList = wbList==null?new HashMap<String,WeiboData>():wbList;
		userList = userList==null?new HashMap<String,UserInfo>():userList;
		
		int curPage = 1, totalPage = -1;
		if(cookies.isEmpty())wbLogin();
		do{
			String url = "http://weibo.cn/search/mblog?keyword="+keyWord+"&page="+curPage;
			getWbPage(url,"搜索结果");
			if(!isValidDoc)continue;
			
			if(totalPage==-1){
				Elements ele = curDoc.getElementsByAttributeValue("name", "mp");
				if(!ele.isEmpty())totalPage = Integer.valueOf(ele.val());				//获取搜索页面数量
				//totalPage = totalPage>5?5:totalPage;
			}
			
			Elements weiboElements = curDoc.getElementsByAttributeValueStarting("id", "M_");	//获得本页微博列表
			for(int i=0;i<weiboElements.size();i++){
				parseWeiboElement(weiboElements.get(i),wbList,userList);
			}
			curPage++;
		}while(curPage<=totalPage);		
	}
	
	
	//根据weibo.cn的搜索结果页面，解析微博和用户信息，并存入wbList和userList
	private static void parseWeiboElement(Element aWeiboElements,Map<String,WeiboData> wbList,Map<String,UserInfo> userList){
		String weiboID = aWeiboElements.id();		//获取微博id
		weiboID = weiboID.replaceAll("M_", "");
		
		Elements userTag = aWeiboElements.getElementsByAttributeValue("class","nk");		//获取用户id和用户名
		String href = userTag.attr("href");
		String userID = getUserIDFromHref(href);
		String userName = userTag.text();
		
		if(!wbList.containsKey(weiboID)){		//如果微博不存在，则插入新数据
			Element contentTag = aWeiboElements.getElementsByAttributeValue("class", "ctt").get(0);	//获取微博内容
			
			Elements repostTag = aWeiboElements.getElementsMatchingOwnText("^转发\\[[0-9]+\\]");	//转发标签
			int repostNum = getIntFromStr(repostTag.text());
			
			Element commentTag = repostTag.get(0).nextElementSibling();				//评论标签
			int commentNum = getIntFromStr(commentTag.text());
			
			Elements createTimeTag = aWeiboElements.getElementsByAttributeValue("class", "ct");	//时间标签
			HashMap<String,String> newWeibo = new HashMap<String,String>();
			newWeibo.put("weiboID", weiboID);
			newWeibo.put("userID", userID);
			newWeibo.put("eventID", String.valueOf(null));
			newWeibo.put("content", contentTag.text());
			newWeibo.put("repostNum", String.valueOf(repostNum));
			newWeibo.put("commentNum", String.valueOf(commentNum));
			newWeibo.put("createAt", getTimeFromStr(createTimeTag.text()));
			newWeibo.put("collectAt", getNowTimeStr());
			newWeibo.put("sentiment", String.valueOf(null));
			wbList.put(weiboID,new WeiboData(newWeibo));
		}
		
		if(!userList.containsKey(userID)){  	//如果用户id不存在，则插入用户
			boolean isV=false;			//判断是否加v
			Elements vTag = aWeiboElements.getElementsByAttributeValue("alt", "V");
			if(vTag.size()>0)isV = true;
			
			boolean isMember = false;	//是否为微博会员
			Elements menberTag = aWeiboElements.getElementsByAttributeValue("alt", "M");
			if(menberTag.size()>0)isMember = true;
			
			boolean isInsider = false;	//是否为微博达人
			Elements insiderTag = aWeiboElements.getElementsByAttributeValue("alt", "达人");
			if(insiderTag.size()>0)isInsider = true;
			HashMap<String,String> newUser = new HashMap<String,String>();
			newUser.put("userID",userID );
			newUser.put("userName",userName );
			newUser.put("isV", String.valueOf(isV));
			newUser.put("isMember", String.valueOf(isMember));
			newUser.put("isInsider", String.valueOf(isInsider));
			userList.put(userID,new UserInfo(newUser));
		}
		
	}
	
	public static void getWeiboComments(String aWeiboID,WeiboData aWeibo,Map<String,UserInfo> userList){
		aWeibo.commentList = aWeibo.commentList==null?new ArrayList<WeiboComment>():aWeibo.commentList;
		List<String> commentIDList = new ArrayList<String>();		//评论id表，防止重复
		int curPage = 1, totalPage = -1;
		if(cookies.isEmpty())wbLogin();		//如果cookie为空，登录
		do{
			String latestTime = "";
			String url = "http://weibo.cn/comment/"+aWeiboID+"?page="+curPage;
			getWbPage(url,"评论列表");		//获得评论列表
			if(!isValidDoc)continue;	//评论不存在或者链接有错误		
			
			if(totalPage==-1){
				Elements ele = curDoc.getElementsByAttributeValue("name", "mp");
				if(!ele.isEmpty())totalPage = Integer.valueOf(ele.val());				//获取搜索页面数量
				//totalPage = totalPage>5?5:totalPage;
				if(totalPage>1){
					latestTime = DBOptions.getLatestComment(aWeiboID);
				}
			}
			
			Elements commentsInPage = curDoc.getElementsByAttributeValueStarting("id", "C_");	//获得本页微博列表
			for(int i=0;i<commentsInPage.size();i++){
				pareseCommentElement(commentsInPage.get(i),aWeiboID,aWeibo.commentList,commentIDList,userList);
				
			}
			if(!aWeibo.commentList.isEmpty()){//判断最新的评论在数据库里是否存在，若存在，则不再抓取
				String curLatestTime = aWeibo.commentList.get(aWeibo.commentList.size()-1).createAt;
				if(latestTime!=""&&timeCompare(curLatestTime,latestTime)<0)break;			
			}
			curPage++;
		}while(curPage<=totalPage);		
	}

	private static void pareseCommentElement(Element aComment,String aWeiboID,List<WeiboComment> commentList,List<String> commentIDList,Map<String,UserInfo> userList){
		String commentID = aComment.id().replaceAll("C_", "");
		Elements hotTag = aComment.getElementsByAttributeValue("class", "kt");		//如果是热门回复，则跳过
		if(hotTag.isEmpty()&&!commentIDList.contains(commentID)){		//跳过热门评论，热门评论会打乱时间排序
			commentIDList.add(commentID);
			
			Element contentTag = aComment.getElementsByAttributeValue("class", "ctt").get(0);	//获取微博内容
			
			Element userTag = aComment.child(0);		//获取用户id标签
			String href = userTag.attr("href");
			String userID = getUserIDFromHref(href);
			String userName = userTag.text();
			
			Elements timeTag = aComment.getElementsByAttributeValue("class", "ct");	//时间标签
			if(!userList.containsKey(userID)){
				boolean isV = false;
				Elements vTag = aComment.getElementsByAttributeValue("alt", "V");
				if(vTag.size()>1)isV = true;
				
				boolean isMember = false;
				Elements menmberTag = aComment.getElementsByAttributeValue("alt", "M");
				if(menmberTag.size()>1)isMember = true;
				
				boolean isInsider = false;
				Elements insiderTag = aComment.getElementsByAttributeValue("alt", "达人");
				if(insiderTag.size()>1)isInsider = true;
				
				HashMap<String,String> newUser = new HashMap<String,String>();
				newUser.put("userID", userID);
				newUser.put("userName", userName);
				newUser.put("fansNum", String.valueOf(null));
				newUser.put("isV", String.valueOf(isV));
				newUser.put("isMember", String.valueOf(isMember));
				newUser.put("isInsider", String.valueOf(isInsider));
				newUser.put("weiboNum", String.valueOf(null));
				newUser.put("registerTime", String.valueOf(null));
				userList.put(userID, new UserInfo(newUser));
			}
			
			
			HashMap<String,String> newComment = new HashMap<String,String>();
			newComment.put("weiboID", aWeiboID);
			newComment.put("commentID", commentID);
			newComment.put("userID", userID);
			newComment.put("content", contentTag.text());
			newComment.put("createAt", getTimeFromStr(timeTag.text()));
			newComment.put("collectAt", getNowTimeStr());
			newComment.put("sentiment", String.valueOf(null));
			commentList.add(new WeiboComment(newComment));
		}		
	}
	
	
	private static String getNowTimeStr(){
		Timestamp nowTimeStamp = new Timestamp(System.currentTimeMillis());
		return nowTimeStamp.toString();
	}
	
	//根据链接，获取用户id
	private static String getUserIDFromHref(String aHref){
		if(aHref.isEmpty())return "";
		String[] results = aHref.split("/");
		String userId = results[results.length-1];
		int index = userId.indexOf("?gid");			//是否包含?gid:/u/2103414073?gid=10001
		if(index>0)
			userId = userId.substring(0, index);
		index = userId.indexOf("?vt");
		if(index>0)
			userId = userId.substring(0, index);
		return userId;
	}
	
	//根据微博字符串文本，获得格式化的字符串
	private static String getTimeFromStr(String wbStr){			
		StringBuilder timeStr=new StringBuilder();
		
		int startIndex = wbStr.indexOf("来自");
		if(startIndex>0)wbStr = wbStr.substring(0, startIndex-1);
		
		Pattern pattern = Pattern.compile("\\d+");  //匹配时间字符串中的数字
	    Matcher matcher = pattern.matcher(wbStr);
	    ArrayList<String> nums = new ArrayList<String>();
	    while (matcher.find()) {  
	        nums.add(matcher.group(0));  
	    }
	    
		if(wbStr.contains("分钟前")){		//针对不同的字符串格式返回时间。
			int min = Integer.valueOf(nums.get(0));
			Timestamp nowTimeStamp = new Timestamp(System.currentTimeMillis());
			Timestamp createAt = new Timestamp(nowTimeStamp.getTime()-min*60*1000);
			timeStr.append(createAt.toString());
		}else if(wbStr.contains("今天")){
			Date nowDate = new Date(System.currentTimeMillis());
			timeStr.append(nowDate.toString()+" "+nums.get(0)+":"+nums.get(1)+":00");
		}else if(nums.size()==4){
			Date curDay = new Date(System.currentTimeMillis());
			String curDayStr = curDay.toString();
			String[] exps = curDayStr.split("-");
			timeStr.append(exps[0]+"-"+nums.get(0)+"-"+nums.get(1)+" "+nums.get(2)+":"+nums.get(3)+":00");
		}else if(nums.size()==6){
			timeStr.append(nums.get(0)+"-"+nums.get(1)+"-"+nums.get(2)+" "+nums.get(3)+":"+nums.get(4)+":"+nums.get(5));
		}
		return timeStr.toString();
	}
	
	
	//从字符串中获得数字
	private static int getIntFromStr(String aStr){
		String regEx="[^0-9]";
		Pattern p = Pattern.compile(regEx);     
		Matcher m = p.matcher(aStr);
		return Integer.valueOf(m.replaceAll("").trim());
	}
	
	//获得微博页面
	public static void getWbPage(String url,String rightTitle){
		isValidDoc = false;
		int tryCount = 5;
		do{			
			getPage(url);		//获取网页
			if(curDoc.title().equals(rightTitle))isValidDoc = true;		//正确的网页
			else if(curDoc.title().equals("微博")&&curDoc.getElementsByAttributeValue("class", "c").get(0).text().equals("如果没有自动跳转,请点击这里.")){	//不存在的页面，说明网址错误~
				isValidDoc = false;
				break;
			}
			if(!isValidDoc&&!isValidCookie()){		//错误网页且cookie不可用
				wbLogin();
			}
		}while(!isValidDoc&&tryCount-->0);
	}
	
	//使用get的方法，并发送cookie，从微薄获取页面，若发生错误，则间断一定时间继续获取
	private static void getPage(String aUrl){
		Response curRespose = null;
		boolean exceptionHappened = false;
		do{
			try {
				curRespose = Jsoup.connect(aUrl)
					.userAgent(userAgent)			//随机使用user agent
					.header("Accept", hAccept)
					.header("Accept-Encoding", hAcceptEncoding)
					.timeout(5000)
					.cookies(cookies)
					.execute();
				curDoc = curRespose.parse();
				exceptionHappened = false;
			}catch(MalformedURLException|UnsupportedMimeTypeException|SocketTimeoutException e){
				e.printStackTrace();
				System.out.println("exception url:"+aUrl+" ,time:"+getCurTimeStr());
				exceptionHappened = true;
			}catch(HttpStatusException he){
				System.out.println("Status:"+curRespose.statusCode());
				System.out.println("exception url:"+aUrl);
				he.printStackTrace();
				exceptionHappened = true;
			}catch (IOException ie) {
				// TODO 自动生成的 catch 块
				ie.printStackTrace();
				exceptionHappened = true;
			}
			if(exceptionHappened){		//如果发生异常，则间断1.5s
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					// TODO 自动生成的 catch 块
					e.printStackTrace();
				}
			}
		}while(exceptionHappened);
	}
	
	
	//使用get的方法，并发送cookie，从微薄获取页面
	public static void weiboPOSTPage(String aUrl,String rightTitle,Map<String,String> postData) throws IOException, InterruptedException{
		do{
			if(cookies.size()<=0){
				wbLogin();
			}
			Document doc = Jsoup.connect(aUrl)
					.userAgent(userAgent)
					.header("Accept", hAccept)
					.header("Accept-Encoding", hAcceptEncoding)
					.data(postData)
					.cookies(cookies)
					.post();
		
			curDoc = doc;
		}while(!isValidDoc(rightTitle));
	}
	
	//微博登陆
	public static void wbLogin(){
		do{
			String vkStr =getLoginVK();
			String curUser = users.get(curUserIndex);
			String[] user_pswd = curUser.split(" ");
			String[] vals = vkStr.split("_");
		
			Response curRespose = null;
			boolean exceptionHappened = false;
			do{
				try {
					curRespose = Jsoup.connect("http://login.weibo.cn/login/")	//根据请求头，获取响应头
							.userAgent(userAgent)
							.header("Accept", hAccept)
							.header("Accept-Encoding", hAcceptEncoding)
							.header("vt", "4")
							.data("mobile",user_pswd[0])
							.data("password_"+vals[0],user_pswd[1])
							.data("remember","on")
							.data("tryCount","")
							.data("vk",vkStr)
							.data("submit","登陆")
							.timeout(5000)
							.method(Connection.Method.POST)
							.execute();
					exceptionHappened = false;
				}catch(MalformedURLException|UnsupportedMimeTypeException|SocketTimeoutException e){
					e.printStackTrace();
					exceptionHappened = true;
					System.out.println("url:http://login.weibo.cn/login/ ,time:"+getCurTimeStr());
				}catch(HttpStatusException he){
					he.printStackTrace();
					exceptionHappened = true;
					System.out.println("Status:"+curRespose.statusCode()+"==>url:http://login.weibo.cn/login/");
				}catch (IOException ie) {
					// TODO 自动生成的 catch 块
					ie.printStackTrace();
					exceptionHappened = true;
					System.out.println("url:http://login.weibo.cn/login/");
				}
				if(exceptionHappened)
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO 自动生成的 catch 块
						e.printStackTrace();
						System.out.println("sleep failed in functio login! At:"+getCurTimeStr());
					}
			}while(exceptionHappened);
			cookies = curRespose.cookies();
			curUserIndex++;		//更换user索引，以便下次登陆
			curUserIndex = curUserIndex>=users.size()?0:curUserIndex;
		}while(!isValidCookie());
		System.out.println("当前登录用户索引"+curUserIndex);
	}
	
	//根据消息页面，判断cookie是否有效
	private static boolean isValidCookie(){
		String testUrl = "http://weibo.cn/msg/";
		boolean validDoc = false;
		int tryCount = 3;
		while((tryCount--)>0&&!validDoc){
			getPage(testUrl);
			if(curDoc.title().equals("消息首页"))validDoc = true;
		}
		return validDoc;
	}
	
	

	private static String getCurTimeStr(){
		Timestamp t = new Timestamp(System.currentTimeMillis());
		return t.toString();
	}
	
	//判断获取的页面是否正常，若不正常，重新登录
	private static boolean isValidDoc(String rightTitle){	
		if(curDoc.title().trim().equals(rightTitle))return true;
		try {
			Thread.sleep(2000);
			wbLogin();
		} catch (InterruptedException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
		return false;
	}
	
	
	//获得微博登陆所需要的VK值
	private static String getLoginVK(){
		while(!isValidDoc){
			getWbPage("http://login.weibo.cn/login/","微博");
		}
		Elements vks = curDoc.select("input[name=vk]");
		if(vks.size()>0)return vks.get(0).val();
		return "";
	}
	
	private static void getUsers(){
		try{
    		BufferedReader br = new BufferedReader(new FileReader("files\\weiboUsers.txt"));
    		String aWord = null;
    		while((aWord = br.readLine())!=null){
    			aWord = aWord.trim();
    			if(aWord!=null){
    				String[] name_pswd = aWord.split(" ");
    				if(name_pswd.length==2){
    					users.add(aWord);
    				}
    			}
    		}
    		br.close();
    	}catch(Exception e){
    		System.out.println(e.getMessage());
    		System.exit(0);
    	}
	}
	
	//比较两个事件字符串的早晚~，time1>time2返回1
	private static int timeCompare(String time1, String time2) {
        int result = 1;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        try {
            c1.setTime(df.parse(time1));
            c2.setTime(df.parse(time2));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        result = c1.compareTo(c2);
        return result;
    }
	
}
