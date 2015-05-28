package dbOptions;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import com.eventwarning.bean.UserInfo;
import com.eventwarning.bean.WeiboComment;
import com.eventwarning.bean.WeiboData;


//此类用于获取数据库数据
public class DBOptions {
	private static Connection conn;			//数据库连接
	private static Statement st;
	private static String db_userName = "root";
	private static String db_password = "123456";
	
	//保存微博列表
	public static int saveWeiboList(Map<String,WeiboData> weiboList){
		int rowEffected = 0;
		String sql = "insert into userInfo values(?,?,?,?,?,?,?,?,?) on duplicate key update commentNum=?, repostNum=?";
		if(conn!=null&&weiboList!=null&&!weiboList.isEmpty()){
			try{
				PreparedStatement prest = conn.prepareStatement(sql);
				Iterator<Entry<String, WeiboData>> it = weiboList.entrySet().iterator();
				while(it.hasNext()){
					Entry<String,WeiboData>data = it.next();
					WeiboData newWeiboData = data.getValue();
					prest.setString(1,newWeiboData.weiboID);
					prest.setString(2,newWeiboData.userID);
					prest.setInt(3,newWeiboData.eventID);
					prest.setString(4,newWeiboData.content);
					prest.setInt(5,newWeiboData.commentNum);
					prest.setInt(6,newWeiboData.repostNum);
					prest.setString(7,newWeiboData.createAt);
					prest.setString(8,newWeiboData.collectAt);
					prest.setDouble(9,newWeiboData.sentiment);
					prest.setInt(10,newWeiboData.commentNum);
					prest.setInt(11,newWeiboData.repostNum);
					prest.addBatch();
					
					rowEffected += saveCommentList(newWeiboData.commentList);		//保存评论列表
				}
				int [] effected = prest.executeBatch();
				for(int i=0;i<effected.length;i++)
					rowEffected+=effected[i];
				conn.commit();
				
			}catch(SQLException e){
				System.out.println("插入数据失败!sql:"+sql);
				System.out.print(e.getMessage());
			}
		}
		return rowEffected;
	}
	
	
	//插入用户信息，若已存在，则更新信息
	public static int saveUserInfoList(Map<String,UserInfo> userList){
		int rowEffected = 0;
		String sql = 
				"insert into userInfo values(?,?,?,?,?,?,?,?) on duplicate key update fansNum=?, isV=?, isInsider=?, isMember=?,weiboNum=?";
		if(conn!=null&&userList.size()!=0){
			try{
				PreparedStatement prest = conn.prepareStatement(sql);
				
				Iterator<Entry<String, UserInfo>> it = userList.entrySet().iterator();
				while(it.hasNext()){
					Entry<String,UserInfo>data = it.next();
					UserInfo aUser = data.getValue();
					
					prest.setString(1,aUser.userID);
					prest.setString(2,aUser.userName);
					prest.setInt(3,aUser.fansNum);
					prest.setBoolean(4,aUser.isV);
					prest.setBoolean(5,aUser.isMember);
					prest.setBoolean(6,aUser.isInsider);
					prest.setInt(7,aUser.weiboNum);
					prest.setString(8,aUser.registerTime);
					
					prest.setInt(9,aUser.fansNum);
					prest.setBoolean(10,aUser.isV);
					prest.setBoolean(11,aUser.isMember);
					prest.setBoolean(12,aUser.isInsider);
					prest.addBatch();
				}
				int [] effected = prest.executeBatch();
				for(int i=0;i<effected.length;i++)
					rowEffected+=effected[i];
				conn.commit();
			}catch(SQLException e){
				System.out.println("插入数据失败!sql:"+sql);
				System.out.print(e.getMessage());
			}
		}
		return rowEffected;
	}
	
	//插入评论列表，若已存在，则更新评论信息
	public static int saveCommentList(List<WeiboComment> commentList){
		int rowEffected = 0;
		String sql = "insert into weibocomment values(?,?,?,?,?,?,?) on duplicate key update userID=?";
		if(conn!=null&&commentList.size()!=0){
			try{
				PreparedStatement prest = conn.prepareStatement(sql);
				int listSize = commentList.size();
				for(int i=0;i<listSize;i++){
					WeiboComment aComm = commentList.get(i);
					prest.setString(1,aComm.commentID);
					prest.setString(2,aComm.weiboID);
					prest.setString(3,aComm.userID);
					prest.setString(4,aComm.content);
					prest.setString(5,aComm.createAt);
					prest.setString(6,aComm.collectAt);
					prest.setDouble(7,aComm.sentiment);
					prest.setString(8,aComm.userID);
					prest.addBatch();
				}
				int [] effected = prest.executeBatch();
				for(int i=0;i<effected.length;i++)
					rowEffected+=effected[i];
				conn.commit();
			}catch(SQLException e){
				System.out.println("插入数据失败!sql:"+sql);
				System.out.print(e.getMessage());
			}
		}
		return rowEffected;
	}
	
	//插入一条微博id和其对应的关键词
	public static int insertWBWord(String aWbId,String keyWord){
		int rowEffected = 0;
		String sql = "insert into tb_wb_word values(?,?) on duplicate key update keyWord=?";
		if(conn!=null){
			try{
				PreparedStatement prest = conn.prepareStatement(sql);
				prest.setString(1, aWbId);
				prest.setString(2, keyWord);
				
				prest.setString(3, keyWord);
				rowEffected = prest.executeUpdate();
				conn.commit();			//提交
			}catch(SQLException e){
				System.out.println("插入数据失败");
				System.out.print(e.getMessage());
			}
		}
		return rowEffected;
	}

	public static int getCommentNumById(String wbId){
		int count = 0;
		String sql = "select count(*) from tb_comment where weiboId=?";
		if(conn!=null){
			try{
				PreparedStatement prest = conn.prepareStatement(sql);
				prest.setString(1, wbId);

				ResultSet r = prest.executeQuery();
				if(r.next()){
					count = r.getInt(1);
				}
				conn.commit();			//提交
			}catch(SQLException e){
				System.out.println("插入数据失败");
				System.out.print(e.getMessage());
			}
		}
		return count;
	}

	
	//使用jdbc获取数据库连接。
	public static void connect(){
		conn=null;
		Connection con = null;
		try{
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection("jdbc:mysql://localhost:3306/fooddata?useUnicode=true&characterEncoding=utf8&autoReconnect=true&zeroDateTimeBehavior=convertToNull&transformedBitIsBoolean=true",db_userName,db_password);
			con.setAutoCommit(false);
		}catch(Exception e){
			System.out.println("数据库连接失败"+e.getMessage());
			e.printStackTrace();
		}
		conn = con;
	}

	//设置评论情感标签
	public static int setLabels(ArrayList<Integer> ids,ArrayList<Integer> labels){
		if(ids.size()!=labels.size()||ids.size()==0)return 0;
		StringBuilder sql = new StringBuilder("update tb_weibocomment set label = case pk_id");
		for(int i=0;i<ids.size();i++){
			sql.append(" when "+ids.get(i)+" then "+labels.get(i));
		}
		sql.append(" end where pk_id in(");
		for(int i=0;i<ids.size();i++){
			if(i==0)sql.append(ids.get(i));
			else sql.append(","+ids.get(i));
		}
		sql.append(")");
		if(conn!=null){
			try{
				st = (Statement) conn.createStatement();
				st.executeUpdate(sql.toString());
			}catch(SQLException e){
				System.out.print("查询数据库失败");
			}
		}else return 0;
		return 1;
	}


	public static String getLatestComment(String aWeiboID) {
		// TODO 自动生成的方法存根
		return null;
	}
}
