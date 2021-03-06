package com.ufgov.server;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.xvolks.jnative.exceptions.NativeException;
import org.xvolks.jnative.pointers.Pointer;

import com.ufgov.ssm.SSMFactory;
import com.ufgov.tts.TTSFactory;
import com.ufgov.util.ApplicationContext;
import com.ufgov.util.DAOFactory;
import com.ufgov.util.EmCallException;
import com.ufgov.util.ExpertUtil;

/**
 * 呼叫线程，发现有待呼叫的专家，则马上进行呼叫
 * @author Administrator
 *
 */
public class CallServer extends Thread {
	private static Logger logger = Logger.getLogger(CallServer.class);

	//获取等待拨打电话的专家，不包括同意或者拒绝的专家，财政局还是用这种代码
	/* public static final String GET_EM_CALL_SERVER_LIST_for_cz = "SELECT C.* FROM EM_CALL_SERVER_LIST C, ZC_EM_EXPERT_PRO_BILL B WHERE C.EM_BILL_CODE = B.EM_BILL_CODE  AND B.EM_BILL_STATUS = 'SELECTING' AND C.ISCALL < ? AND C.ISCALL >= ? " +
			" and c.em_expert_code not in("+
       " select e.em_expert_code from ZC_EM_EXPERT_EVALUATION e where e.em_bill_code = b.em_bill_code "+//同意参加的不再拨打电话
       " union "+
       " select distinct vr.em_expert_code from (select r.em_bill_code,r.em_expert_code from em_call_expert_record r where r.call_num = ? or r.call_state = '8') vr where vr.em_bill_code = c.em_bill_code and vr.em_expert_code = c.em_expert_code " +  //呼叫记录达到1次、明确拒绝的不再拨打电话
     " union "+//专家同意参加和当前单据的评标时间是同一天的其他项目，不再拨打电话
     " select e.em_expert_code from ZC_EM_EXPERT_EVALUATION e,ZC_EM_EXPERT_PRO_BILL pb "+
         " where e.em_bill_code=pb.em_bill_code and e.em_bill_code != b.em_bill_code and e.em_response_status='9' and e.em_expert_code=c.em_expert_code "+
         " and TRUNC(b.em_tenders_time)-TRUNC(pb.em_tenders_time)=0 " +//同一天的其他项目
      " )  ";*/
	 public static final String GET_EM_CALL_SERVER_LIST_for_cz = "SELECT C.* FROM EM_CALL_SERVER_LIST C, ZC_EM_EXPERT_PRO_BILL B WHERE C.EM_BILL_CODE = B.EM_BILL_CODE  AND B.EM_BILL_STATUS = 'SELECTING' AND C.ISCALL < ? AND C.ISCALL >= ? " ;
    
      //获取等待拨打电话的专家，不包括同意或者拒绝的专家，财政局还是用这种代码,支持多电话卡抽取
     /* public static final String GET_EM_CALL_SERVER_LIST2_for_cz = "SELECT C.* FROM EM_CALL_SERVER_LIST C, ZC_EM_EXPERT_PRO_BILL B WHERE C.EM_BILL_CODE = B.EM_BILL_CODE AND B.EM_CO_CODE=?  AND B.EM_BILL_STATUS = 'SELECTING' AND C.ISCALL < ? AND C.ISCALL >= ? " +
      " and c.em_expert_code not in("+
       " select e.em_expert_code from ZC_EM_EXPERT_EVALUATION e where e.em_bill_code = b.em_bill_code "+//同意参加的不再拨打电话
       " union "+
       " select distinct vr.em_expert_code from (select r.em_bill_code,r.em_expert_code from em_call_expert_record r where r.call_num = ? or r.call_state = '8') vr where vr.em_bill_code = c.em_bill_code and vr.em_expert_code = c.em_expert_code " +  //呼叫记录达到1次、明确拒绝的不再拨打电话
     " union "+//专家同意参加和当前单据的评标时间是同一天的其他项目，不再拨打电话
     " select e.em_expert_code from ZC_EM_EXPERT_EVALUATION e,ZC_EM_EXPERT_PRO_BILL pb "+
         " where e.em_bill_code=pb.em_bill_code and e.em_bill_code != b.em_bill_code and e.em_response_status='9' and e.em_expert_code=c.em_expert_code "+
         " and TRUNC(b.em_tenders_time)-TRUNC(pb.em_tenders_time)=0 " +//同一天的其他项目
      " )  ";*/
	 public static final String GET_EM_CALL_SERVER_LIST2_for_cz = "SELECT C.* FROM EM_CALL_SERVER_LIST C, ZC_EM_EXPERT_PRO_BILL B WHERE C.EM_BILL_CODE = B.EM_BILL_CODE AND B.EM_CO_CODE=?  AND B.EM_BILL_STATUS = 'SELECTING' AND C.ISCALL < ? AND C.ISCALL >= ? " ;
    
	//代理机构改成用视图获取专家电话信息，这样可以不同的时候，调整这个视图的电话号码，如前面加9等,财政局还是用上面的代码
	/*public static final String GET_EM_CALL_SERVER_LIST = "SELECT C.* FROM V_EM_CALL_SERVER_LIST C, ZC_EM_EXPERT_PRO_BILL B WHERE C.EM_BILL_CODE = B.EM_BILL_CODE  AND B.EM_BILL_STATUS = 'SELECTING' AND C.ISCALL < ? AND C.ISCALL >= ? " +
    " and c.em_expert_code not in("+
     " select e.em_expert_code from ZC_EM_EXPERT_EVALUATION e where e.em_bill_code = b.em_bill_code "+//同意参加的不再拨打电话
     " union "+
     " select distinct vr.em_expert_code from (select r.em_bill_code,r.em_expert_code from em_call_expert_record r where r.call_num = ? or r.call_state = '8') vr where vr.em_bill_code = c.em_bill_code and vr.em_expert_code = c.em_expert_code " +  //呼叫记录达到1次、明确拒绝的不再拨打电话
     " union "+//专家同意参加和当前单据的评标时间是同一天的其他项目，不再拨打电话
     " select e.em_expert_code from ZC_EM_EXPERT_EVALUATION e,ZC_EM_EXPERT_PRO_BILL pb "+
         " where e.em_bill_code=pb.em_bill_code and e.em_bill_code != b.em_bill_code and e.em_response_status='9' and e.em_expert_code=c.em_expert_code "+
         " and TRUNC(b.em_tenders_time)-TRUNC(pb.em_tenders_time)=0 " +//同一天的其他项目
      " )  ";*/
	 public static final String GET_EM_CALL_SERVER_LIST = "SELECT C.* FROM V_EM_CALL_SERVER_LIST C, ZC_EM_EXPERT_PRO_BILL B WHERE C.EM_BILL_CODE = B.EM_BILL_CODE  AND B.EM_BILL_STATUS = 'SELECTING' AND C.ISCALL < ? AND C.ISCALL >= ? " ;
	    
//代理机构改成用视图获取专家电话信息，这样可以不同的时候，调整这个视图的电话号码，如前面加9等,财政局还是用上面的代码, 支持多电话卡抽取
 /* public static final String GET_EM_CALL_SERVER_LIST2 = "SELECT C.* FROM V_EM_CALL_SERVER_LIST C, ZC_EM_EXPERT_PRO_BILL B WHERE C.EM_BILL_CODE = B.EM_BILL_CODE AND B.EM_CO_CODE=?  AND B.EM_BILL_STATUS = 'SELECTING' AND C.ISCALL < ? AND C.ISCALL >= ? " +
    " and c.em_expert_code not in("+
     " select e.em_expert_code from ZC_EM_EXPERT_EVALUATION e where e.em_bill_code = b.em_bill_code "+//同意参加的不再拨打电话
     " union "+
     " select distinct vr.em_expert_code from (select r.em_bill_code,r.em_expert_code from em_call_expert_record r where r.call_num = ? or r.call_state = '8') vr where vr.em_bill_code = c.em_bill_code and vr.em_expert_code = c.em_expert_code " +  //呼叫记录达到1次、明确拒绝的不再拨打电话
     " union "+//专家同意参加和当前单据的评标时间是同一天的其他项目，不再拨打电话
     " select e.em_expert_code from ZC_EM_EXPERT_EVALUATION e,ZC_EM_EXPERT_PRO_BILL pb "+
         " where e.em_bill_code=pb.em_bill_code and e.em_bill_code != b.em_bill_code and e.em_response_status='9' and e.em_expert_code=c.em_expert_code "+
         " and TRUNC(b.em_tenders_time)-TRUNC(pb.em_tenders_time)=0 " +//同一天的其他项目
      " )  ";*/
	 public static final String GET_EM_CALL_SERVER_LIST2 = "SELECT C.* FROM V_EM_CALL_SERVER_LIST C, ZC_EM_EXPERT_PRO_BILL B WHERE C.EM_BILL_CODE = B.EM_BILL_CODE AND B.EM_CO_CODE=?  AND B.EM_BILL_STATUS = 'SELECTING' AND C.ISCALL < ? AND C.ISCALL >= ? " ;
	    

	//private static final String GET_EM_CALL_EXPERT_NUM = "SELECT NVL(MAX(D.CALL_NUM),0) AS CALL_NUM,CALL_STATE FROM EM_CALL_EXPERT_RECORD D WHERE D.EM_BILL_CODE =? AND D.EM_EXPERT_CODE =?";
	
	private static final String GET_EM_CALL_EXPERT_NUM = "SELECT M.*,D.CALL_STATE  FROM EM_CALL_EXPERT_RECORD D,(SELECT NVL(MAX(D.CALL_NUM), 0) AS CALL_NUM,EM_BILL_CODE,EM_EXPERT_CODE FROM EM_CALL_EXPERT_RECORD D WHERE D.EM_BILL_CODE = ?  AND D.EM_EXPERT_CODE = ? GROUP BY EM_BILL_CODE,EM_EXPERT_CODE) M  WHERE D.EM_BILL_CODE = ? AND D.EM_EXPERT_CODE = ? AND D.CALL_NUM = M.CALL_NUM";
	
	//插入选择到的专家
  private static final String INSERT_EM_EXPERT_EVALUATION = "INSERT INTO ZC_EM_EXPERT_EVALUATION (EM_BILL_CODE, EM_EXPERT_CODE, EM_EXPERT_TYPE_CODE, EM_NOTICE_STATUS,EM_RESPONSE_STATUS) VALUES (?, ?, ?, ?,?)";

//	private static final String UPDATE_CALL_SERVER_STATE = "update EM_CALL_SERVER_LIST set ISCALL = ISCALL+1 where OBJID=?";

	private static final String UPDATE_EM_CALL_SERVER_LIST_TO_OVER = "update EM_CALL_SERVER_LIST set ISCALL = ? where OBJID=?";
	
	private static final String DELETE_EM_CALL_SERVER_LIST_BY_ID = "delete from  EM_CALL_SERVER_LIST  where OBJID=?";

//	private static final String UPDATE_EM_EXPERT_EVALUATION = "update ZC_EM_EXPERT_EVALUATION set EM_RESPONSE_STATUS =? where EM_EXPERT_CODE = ? and EM_BILL_CODE = ?";

	private static final String UPDATE_EM_CALL_EXPERT_RECORD = "update EM_CALL_EXPERT_RECORD set call_state=? where em_bill_code=? and em_expert_code=? and call_num=?  ";

	public static final String VOICE_DIR = System.getProperty("user.dir")
			+ File.separator + "voices";

	public static final String AGREE_FILE_PATH = VOICE_DIR + File.separator
			+ "agree.wav";

	public static final String DECLINE_FILE_PATH = VOICE_DIR + File.separator
			+ "decline.wav";

	public static final String REPLAY_FILE_PATH = VOICE_DIR + File.separator
			+ "replay.wav";

	public static String agreeText, declineText, replayText;

	private static final String AGREE = "1", DECLINE = "2", PLAY_AGAIN = "0";

	private static final String CALLING = "CALLING", NOT_CALLING = "NOT_CALLING";
	

	/*
	 * 开始呼叫
	 */
	private static final String CALL_STATUS_BEGIN = "beging_call";

	/*
	 * 呼叫失败
	 */
	private static final String CALL_STATUS_FAIL = "call_fail";

	/*
	 * 生成呼叫语音文件失败
	 */
	private static final String CALL_STATUS_MAKE_VOICE_FILE_FAIL = "voice_file_fail";
	
	/*
	 * 暂停抽取
	 */
	private static final String CALL_STATUS_PAUSE = "select_pause";

	/*
	 * 空号
	 */
	private static final String CALL_STATUS_NULL_PHONE_NUMBER = "null_phone_number";
	

  /*
   * 没有在线路上检测到拨号音
   */
  private static final String CALL_STATUS_NO_SIGNAL = "NO_SIGNAL";

	/*
	 * 无人接听
	 */
	private static final String CALL_STATUS_NO_RESPONSE = "no_response";

	/*
	 * 对方正忙
	 */
	private static final String CALL_STATUS_BUSY = "busy";

	/*
	 * 开始通话
	 */
	private static final String CALL_STATUS_BEGIN_TALKING = "begin_talking";

	/*
	 * 播放语音文件失败
	 */
	private static final String CALL_STATUS_PLAY_VOICE_FILE_FAIL = "play_voice_file_fail";

	/*
	 * 通话异常
	 */
	private static final String CALL_STATUS_SYS_ERROR = "sys_error";

	/*
	 * 挂机
	 */
	private static final String CALL_STATUS_HUANG_UP = "hang_up";

/*
 * 语音文件播放结束
 */
	private static final String CALL_STATUS_VOICE_FILE_END = "voice_file_end";

	/*
	 * 通话结束
	 */
	private static final String CALL_STATUS_END = "end_call";
	
	/*
	 * 获取语音通道异常
	 */
	private static final String YUYIN_TONGDAO_YICHANG = "yuyin_tongdao_yichang";

	



	/**
	 * 呼叫专家的次数，默认是给专家打3次电话
	 */
	public static int CALL_NUM = new Integer(1);

	private static boolean generalVoiceFlag = false;

	public static Map<String, String> callingMap = new HashMap<String, String>();

	private TTSFactory tsf = null;

	private  String objID="";
	
	private  int threadNum =0; 
	
	private int waitTime=5000;//出现拨不出去时,等候5秒再拨,如果拨够6次,就停止拨打
	
	private int waitTimes=6;
	

	/**
	 * 唯一标识
	 */
	private long dinstinctFlag=1L;
	
	public CallServer(){
	  dinstinctFlag=System.currentTimeMillis();
	}
	@Override
  public int hashCode() {
	  Long ll=Long.valueOf(dinstinctFlag);
    return ll.hashCode();
  }
  @Override
  public boolean equals(Object obj) {
    if(obj instanceof CallServer){
      CallServer temp=(CallServer)obj;
      if(temp.dinstinctFlag==dinstinctFlag){
        return true;
      }
    }
    return false;
  }
  public boolean init() {
		boolean flag = initConfig();
		if (!flag) {
			return false;
		}
		flag = initPlayFiles();
		if (!flag) {
			return false;
		}
		return true;
	}

	public TTSFactory getTsf() {
		return tsf;
	}

	public void setTsf(TTSFactory tsf) {
		this.tsf = tsf;
	}

	public void setthreadNum(int threadNum) {
		this.threadNum= threadNum;
	}
	
	private boolean initPlayFiles() {
		if (CallServer.generalVoiceFlag) {
			return true;
		}
		File dir = new File(VOICE_DIR);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		// TTSFactory tsf = new TTSFactory();
		boolean flag = tsf.ttsPlayToFile( AGREE_FILE_PATH,
				agreeText);
		if (!flag) {
			logger.error("生成接受邀请语音文件失败。");
			return false;
		}
		flag = tsf.ttsPlayToFile( DECLINE_FILE_PATH, declineText);
		if (!flag) {
			logger.error("生成拒绝邀请语音文件失败。");
			return false;
		}
		flag = tsf.ttsPlayToFile( REPLAY_FILE_PATH, replayText);
		if (!flag) {
			logger.error("生成重听邀请语音文件失败。");
			return false;
		}
		logger.info("初始化接受邀请、拒绝邀请、重听邀请语音文件成功。");

		CallServer.generalVoiceFlag = true;

		return true;
	}

	private boolean initConfig() {
		agreeText = ApplicationContext.singleton().getValueAsString("agreeText");
		declineText = ApplicationContext.singleton().getValueAsString("declineText");
		replayText = ApplicationContext.singleton().getValueAsString("replayText");
		if (isEmpty(agreeText) || isEmpty(declineText) || isEmpty(replayText)) {
			logger.error("读取同意、拒绝、重听配置文本失败，请检查配置文件。");
			return false;
		}
		String callNumStr = ApplicationContext.singleton().getValueAsString("callNum");
		if (isEmpty(callNumStr)) {
			logger.error("获取扫描次数失败，系统将以默认配置启动。");
		} else {
			CALL_NUM = Integer.parseInt(callNumStr.trim());
		}
		logger.info("初始化TTS配置文本信息成功。");
		return true;
	}

	private static boolean isEmpty(String text) {
		if (text == null || "".equals(text.trim())) {
			return true;
		}
		return false;
	}

	public void run() {
		boolean flag = init();
		if (!flag) {
	    logger.error("系统初始化失败,系统退出.");
			System.exit(0);
		}
		//
		logger.info("开始轮询呼叫任务表。");

		while (true) {
			try {
				objID="";
				scan();
				ExpertUtil.compeletCalling(objID);
				sleep(3000);
			} catch (Exception e) {
			  logger.error("呼叫专家方法 scan出现异常\n"+e.getMessage(),e);
				e.printStackTrace();
				//目前出现呼叫失败，就不往下抽取了。估计问题出在这里，暂时把它注释掉，运行一段时间观察一下
//				ExpertUtil.compeletCalling(objID); 
			}
		}

	}

	private void clearCallServer() {
		if (ServiceContext.callServerList.contains(this)) {
			ServiceContext.callServerList.remove(this);
		}
	}

	public void scan() throws EmCallException {
		try{
  		Map<String, String> expertWaitingForCall = ExpertUtil.getWaitingCallExpert();
  		if (expertWaitingForCall == null || expertWaitingForCall.size() == 0) {
  			return;
  		}
  		
  		objID = expertWaitingForCall.get("OBJID");
  		String emExpertCode = expertWaitingForCall.get("EM_EXPERT_CODE");
  		String emMobile = expertWaitingForCall.get("EM_MOBILE");
  		String emCallMSG = expertWaitingForCall.get("EM_CALL_MSG");
  		String playFilePath = VOICE_DIR + File.separator + objID + ".wav";
  		String emBillCode = expertWaitingForCall.get("EM_BILL_CODE");
  		String isCall = expertWaitingForCall.get("ISCALL");
  		String expertType=expertWaitingForCall.get("EM_EXPERT_TYPE_CODE");
  
  		int callNum=Integer.parseInt(isCall);
  		callNum+=1;
  		
  		if(!isSelecting(emBillCode)){
  			updateCallingStatus(CallServer.CALL_STATUS_PAUSE, null,emExpertCode, objID, emBillCode, callNum, 0,expertType,"pause");
  			return;
  		}
  		
  		if(agree(emBillCode,emExpertCode)){
  		  logger.info("专家"+emExpertCode+"已经被邀请，不再拨打电话.");
  		  updateCallingStatus(CallServer.CALL_STATUS_NO_RESPONSE, null,emExpertCode, objID, emBillCode, callNum, 0,expertType,"已经被邀请，不再拨打电话.");
  		  return;
  		}
  		
  		
  		
  		
  		File f = new File(playFilePath);
  		if (!f.exists()) {			
  			boolean flag = tsf.ttsPlayToFile( playFilePath,emCallMSG);
  			if (!flag) {
  			  String errorMsg="生成语音文件失败：" + playFilePath;
  				logger.error(errorMsg);
  				updateCallingStatus(CallServer.CALL_STATUS_MAKE_VOICE_FILE_FAIL, null,emExpertCode, objID, emBillCode, callNum, 0,expertType,errorMsg);
  				throw new EmCallException("呼叫退出，生成语音文件失败：" + playFilePath);
  			}
  		}		
  		
  //		logger.info("第" +  callNum + "次呼叫电话:"+ emMobile);		
  //		logger.info("线程"+threadNum+" 抽取单号==="+emBillCode+" objid===="+objID);
  		if(haveSelectOneWithSameUnit(emExpertCode,emBillCode)){
  		      String sql=CallServer.UPDATE_EM_CALL_SERVER_LIST_TO_OVER;
  		      Object[] params = new Object[] {Integer.valueOf(-8),Integer.parseInt(objID)};
  		      excuteSql(sql, params);
  
  		       sql="insert into EM_CALL_EXPERT_RECORD(em_bill_code,em_expert_code,call_num,call_time,call_state) values(?,?,?,sysdate,?)";
  		       params = new Object[] { emBillCode, emExpertCode, isCall, Integer.valueOf(8)};
  		       excuteSql(sql, params);
  		      return;
    		}else{
    		  call(emMobile, playFilePath, emExpertCode, objID, emBillCode, callNum,expertType);	
    		}
  		}catch(EmCallException e){
  		  e.printStackTrace();
  		  logger.error(e.getMessage(), e);
  		  throw e;
  		}
	}
	/**
	 * 是否已经被邀请，不再拨打电话.
	 * @param emBillCode
	 * @param emExpertCode
	 * @return
	 */
private boolean agree(String emBillCode, String emExpertCode) {
  String sql="select e.*"
                 +"  from ZC_EM_EXPERT_EVALUATION e,"
                        +"  ZC_EM_EXPERT_PRO_BILL   b,"
                        +"  ZC_EM_EXPERT_PRO_BILL   b2"
                  +"  where e.em_response_status = '9'"
                    +"  and e.em_bill_code = b.em_bill_code"
                    +"  and e.em_bill_code != b2.em_bill_code"
                    +"  and e.em_expert_code = ?"
                    +"  and b2.em_bill_code=?"
                    +"  and TRUNC(b.em_tenders_time) -TRUNC(b2.em_tenders_time) = 0";
  Object[] params = new Object[] {emExpertCode,emBillCode};
  try {
    DAOFactory df=new DAOFactory();
    List<Map<String, String>> rows=df.queryToListMap(sql, params); 
    logger.debug("查看专家是否被同一天的项目邀请了");
    logger.debug(sql);
    logger.debug(emExpertCode);
    logger.debug(emBillCode); 
    
    if(rows!=null && rows.size()>0){
      logger.debug("已被邀请了");
      return true;
    }
  } catch (EmCallException e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
    logger.error(e.getMessage(), e);
  }
  logger.debug("没有被邀请");
  return false;
  }
/**
 * 专家所在单位是否已经有专家被抽中
 * @param emExpertCode
 * @param emBillCode
 * @return
 */
	private boolean haveSelectOneWithSameUnit(String emExpertCode, String emBillCode) {
	  
	  String sql="select c1.em_expert_code " +
  " from zc_em_b_expert c1 " +
  " where c1.em_expert_code=? and c1.em_unit_name in  " +
  " (select distinct e.em_unit_name " +
  " from zc_em_b_expert e, ZC_EM_EXPERT_EVALUATION b " +
  " where e.em_expert_code = b.em_expert_code " +
  " and b.em_response_status = '9'  " +
  " and b.em_bill_code = ? " +
  " and b.em_expert_code != ?)";
	  
	  Object[] params = new Object[] {emExpertCode,emBillCode,emExpertCode};
	  
	  try {
      DAOFactory df=new DAOFactory();
      List<Map<String, String>> rows=df.queryToListMap(sql, params); 
      logger.debug(sql);
      logger.debug(emExpertCode);
      logger.debug(emBillCode);
      logger.debug(emExpertCode);
      
      if(rows!=null && rows.size()>0){
        logger.debug("rows.size()="+rows.size());
        return true;
      }
    } catch (EmCallException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      logger.error(e.getMessage(), e);
    }
    return false;
  }
  /**
	 * 当前单据是否处于抽取状态
	 * @param emBillCode
	 * @return
	 * @throws SQLException 
	 */
	private boolean isSelecting(String emBillCode) throws EmCallException {
		// TCJLODO Auto-generated method stub
		String status=getBillStatus(emBillCode);
		if("SELECTING".equals(status)){
			return true;
		}
		return false;
	}
	
	private String getBillStatus(String emBillCode) throws EmCallException{
		String status=null;
		String sql="select b.em_bill_status from zc_em_expert_pro_bill b where b.em_bill_code=?";
		Object[] params=new Object[]{emBillCode};
    DAOFactory df=new DAOFactory();
		Map<String, String> rt=df.queryToColumnMap(sql,params);
		if(rt==null){
			return status;
		}
		status=rt.get("EM_BILL_STATUS");
		return status;
	}

	private boolean pauseSelecting(String emBillCode) throws EmCallException {
		String status=getBillStatus(emBillCode);
		if("SELECT_PAUSE".equals(status)){
			return true;
		}
		return false;
	}

	public String getLastErrMsg() throws NativeException,
			IllegalAccessException {
		Pointer pointer = SSMFactory.creatPointer(40);
		SSMFactory.ssmGetLastErrMsg(pointer);
		String errMeg = pointer.getAsString();
		pointer.dispose();
		return errMeg;
	}

	public String getKeyStr(int ch) throws NativeException,	IllegalAccessException {
		Pointer pointer = SSMFactory.creatPointer(40);
		String key = "";
		int rlt = SSMFactory.ssmGet1stDtmfClr(ch, pointer);
		if (rlt == 1) {
			key = pointer.getAsString();
			logger.debug("get the key:"+key);
			pointer.dispose();
		} else if (rlt == -1) {
			logger.info("获取按键错误。" + getLastErrMsg());
		}
		return key;
	}

	public boolean isRealPersonPickedUp(int ch) throws NativeException,
			IllegalAccessException {
		int rlt = SSMFactory.ssmClearAMDResult(ch);
		if (rlt == -1) {
			logger.info("清除AMD算法结果失败。" + getLastErrMsg());
			return false;
		}
		rlt = SSMFactory.ssmGetAMDResult(ch);
		logger.info("调用AMD算法结果失败。" + getLastErrMsg());
		if (rlt == 0) {
			return true;
		}
		return false;
	}

	public void call(String emMobile, String playFilePath, String emExpertCode,	String objId, String emBillCode, int isCall,String expertType) throws EmCallException {
		String key = "";
		int record = 0;
		int rlt = -2;
		int ch = -1;
		String errorMsg="";
		try {
			// 开始打电话
			updateCallingStatus(CallServer.CALL_STATUS_BEGIN, key,emExpertCode, objId, emBillCode, isCall, record,expertType,null);
			// 获取空闲通道
			int timmer = 0;
			while (ch == -1 && timmer < 10000) {
				ch = SSMFactory.ssmSearchIdleCallOutCh(1, 0);
//				logger.info("ch=" + ch);
				Thread.sleep(10);
				timmer += 10;
			}
			if (ch == -1) {
			  errorMsg="获取空闲通道失败。" + getLastErrMsg();
				logger.error(errorMsg);
				updateCallingStatus(CallServer.YUYIN_TONGDAO_YICHANG, key,emExpertCode, objId, emBillCode, isCall, record,expertType,errorMsg);
				return;
			}			

			rlt = SSMFactory.ssmSetHangupStopPlayFlag(ch, 1);
			if (rlt == -1) {
        errorMsg="设置放音任务是否因驱动程序状态机检测到对端挂机而终止失败。" + getLastErrMsg();
				logger.error(errorMsg);
				updateCallingStatus(CallServer.YUYIN_TONGDAO_YICHANG, key,emExpertCode, objId, emBillCode, isCall, record,expertType,errorMsg);
				return;
			}
			rlt = SSMFactory.SsmSetDtmfStopPlay(ch, true);
			if (rlt == -1) {
        errorMsg="设置放音任务是否因DTMF检测器检测到DTMF字符而终止失败。" + getLastErrMsg();
				logger.error(errorMsg);
				updateCallingStatus(CallServer.CALL_STATUS_FAIL, key,emExpertCode, objId, emBillCode, isCall, record,expertType,errorMsg);
				return;
			}
			// 摘机
			rlt = SSMFactory.ssmPickup(ch);
			if (rlt == -1) {
        errorMsg="在通道上执行摘机操作失败。" + getLastErrMsg();
				logger.error(errorMsg);
				SSMFactory.ssmHangup(ch);
				updateCallingStatus(CallServer.CALL_STATUS_FAIL, key,emExpertCode, objId, emBillCode, isCall, record,expertType,errorMsg);
				return;
			}
			// 呼叫

      timmer = 0;
      rlt=-1;
      while(timmer<30000 && rlt==-1){
        
  			rlt = SSMFactory.ssmAutoDial(ch, emMobile);  			

  			if(rlt==-1){
          Thread.sleep(5000);
          timmer += 5000;  		
          errorMsg="向驱动程序提交AutoDial任务，发起一次去话呼叫失败。等待5秒";
          logger.error(errorMsg);	  
  			} 
      }
      if (rlt == -1) {
        errorMsg="向驱动程序提交AutoDial任务，发起一次去话呼叫失败。" + getLastErrMsg();
        logger.error(errorMsg);
        SSMFactory.ssmHangup(ch);
        updateCallingStatus(CallServer.CALL_STATUS_FAIL, key,emExpertCode, objId, emBillCode, isCall, record,expertType,errorMsg);
        return;
      }
			// 被叫摘机
			timmer = 0;
			int chkAuto = -2;
			// 12：被叫用户号码为空号，DIAL_VOICE：被叫用户摘机，AutoDial任务完成
			while (chkAuto != 7 && chkAuto != 12 && timmer < 30000) {
				chkAuto = SSMFactory.ssmChkAutoDial(ch);
				Thread.sleep(200);
				timmer += 200;
			}
			SSMFactory.ssmClearAMDResult(ch);
			//如果在30秒的时间内没有拨通电话，认为呼叫失败					
			if (chkAuto == -1 || timmer == 30000) {
			  errorMsg="专家(编号:"+emExpertCode+")无法拨通，chkAuto="+chkAuto+",timmer="+timmer;
				logger.error(errorMsg);
				SSMFactory.ssmHangup(ch);
				updateCallingStatus(CallServer.CALL_STATUS_NO_RESPONSE, key,emExpertCode, objId, emBillCode, isCall, record,expertType,errorMsg);
				return;
			}
      //如果在30秒的时间内没有拨通电话，认为呼叫失败         
      if (chkAuto == 3) {
        errorMsg="语音卡线路没有检测到信号，请检查电话线插入口是否正确！chkAuto="+chkAuto;
        logger.error(errorMsg);
        SSMFactory.ssmHangup(ch);
        updateCallingStatus(CallServer.CALL_STATUS_NO_SIGNAL, key,emExpertCode, objId, emBillCode, isCall, record,expertType,errorMsg);
        return;
      }
			if (chkAuto == 4) {
			  errorMsg="对方用户忙！chkAuto="+chkAuto;
				logger.info(errorMsg);
				SSMFactory.ssmHangup(ch);
				updateCallingStatus(CallServer.CALL_STATUS_BUSY, key,emExpertCode, objId, emBillCode, isCall, record,expertType,errorMsg);
				return;
			}
			if(chkAuto==5){ 
			  errorMsg="拨号完成后，线路上先是出现了回铃音，然后保持静默,无法通话，结束！chkAuto="+chkAuto;
      logger.info(errorMsg);
      SSMFactory.ssmHangup(ch);
      updateCallingStatus(CallServer.CALL_STATUS_NO_RESPONSE, key,emExpertCode, objId, emBillCode, isCall, record,expertType,errorMsg);
      return;			  
			}
      if(chkAuto==6){ 
        errorMsg="拨号完成后，线路上没有检测到回铃音，一直保持静默,无法通话，结束！chkAuto="+chkAuto;
      logger.info(errorMsg);
      SSMFactory.ssmHangup(ch);
      updateCallingStatus(CallServer.CALL_STATUS_NO_RESPONSE, key,emExpertCode, objId, emBillCode, isCall, record,expertType,errorMsg);
      return;       
      }
      if(chkAuto==10){ 
        errorMsg="被叫用户在指定时间内没有摘机，AutoDial失败，结束！chkAuto="+chkAuto;
      logger.info(errorMsg);
      SSMFactory.ssmHangup(ch);
      updateCallingStatus(CallServer.CALL_STATUS_NO_RESPONSE, key,emExpertCode, objId, emBillCode, isCall, record,expertType,errorMsg);
      return;       
      }
      if(chkAuto==11){ 
        errorMsg="AutoDial任务失败，结束！chkAuto="+chkAuto;
      logger.info(errorMsg);
      SSMFactory.ssmHangup(ch);
      updateCallingStatus(CallServer.CALL_STATUS_NO_RESPONSE, key,emExpertCode, objId, emBillCode, isCall, record,expertType,errorMsg);
      return;       
      }
      if(chkAuto==12){ 
        errorMsg="被叫用户号码为空号，AutoDial任务失败，结束！chkAuto="+chkAuto;
      logger.info(errorMsg);
      SSMFactory.ssmHangup(ch);
      updateCallingStatus(CallServer.CALL_STATUS_NO_RESPONSE, key,emExpertCode, objId, emBillCode, isCall, record,expertType,errorMsg);
      return;       
      }
      if(chkAuto==13 || chkAuto==14){ 
        errorMsg="IP卡SIP通道收到消息，结束！chkAuto="+chkAuto;
      logger.info(errorMsg);
      SSMFactory.ssmHangup(ch);
      updateCallingStatus(CallServer.CALL_STATUS_NO_RESPONSE, key,emExpertCode, objId, emBillCode, isCall, record,expertType,errorMsg);
      return;       
      }
			if (chkAuto != 7 && chkAuto!=8 && chkAuto!=9) {
			  errorMsg="呼叫异常，返回结果为:"+chkAuto;
				logger.info(errorMsg);
				SSMFactory.ssmHangup(ch);
				updateCallingStatus(CallServer.CALL_STATUS_NO_RESPONSE, key,emExpertCode, objId, emBillCode, isCall, record,expertType,errorMsg);
				return;
			}
			//开始通话
			updateCallingStatus(CallServer.CALL_STATUS_BEGIN_TALKING, key,emExpertCode, objId, emBillCode, isCall, record,expertType,null);

			// 播放单个文件，启动文件播放任务
			sleep(2000);//等待两秒开始放音
			//放音
			playVoiceFile(ch, playFilePath);
			rlt = -2;
			//获取放音是否结束以及放音过程中的按键信息
			String kk=getKeyByCalling(ch);
			if(kk.equals(CallServer.CALL_STATUS_HUANG_UP)){
				updateCallingStatus(CallServer.CALL_STATUS_HUANG_UP, key,emExpertCode, objId, emBillCode, isCall, record,expertType,null);
				key = CallServer.CALL_STATUS_HUANG_UP;
			}else if(kk.equals(CallServer.AGREE)||kk.equals(CallServer.DECLINE)||kk.equals(CallServer.PLAY_AGAIN)){
				key=kk;
			}

			// 清空按键缓冲区
			rlt = SSMFactory.ssmClearRxDtmfBuf(ch);
			if (rlt == -1) {
			  errorMsg="清空按键缓冲区失败。" + getLastErrMsg();
				logger.error(errorMsg);
				SSMFactory.ssmHangup(ch);
				updateCallingStatus(CallServer.CALL_STATUS_SYS_ERROR, key,emExpertCode, objId, emBillCode, isCall, record,expertType,errorMsg);
				return;
			}
			timmer = 0;
			while (timmer < 30000 && SSMFactory.ssmGetChState(ch) != 7 && SSMFactory.ssmCheckPlay(ch)!=4) {
				//
				if (key.equals(PLAY_AGAIN)) {
					timmer=-10000;
					playVoiceFile(ch, playFilePath);	
					kk=getKeyByCalling(ch);
					if(kk.equals(CallServer.CALL_STATUS_HUANG_UP)){
						updateCallingStatus(CallServer.CALL_STATUS_HUANG_UP, key,emExpertCode, objId, emBillCode, isCall, record,expertType,null);
					}else if(kk.equals(CallServer.AGREE)||kk.equals(CallServer.DECLINE)||kk.equals(CallServer.PLAY_AGAIN)){
						key=kk;
					}
				} else if (key.equals(AGREE)) {
					playVoiceFile(ch, AGREE_FILE_PATH);
					while (!(SSMFactory.ssmCheckPlay(ch) == 1//放音结束
							|| SSMFactory.ssmCheckPlay(ch)==4 //放音时对方挂机
							|| SSMFactory.ssmCheckPlay(ch)==2//放音时对方按了触发键
							)) {
						Thread.sleep(200);
					}
					break;
				} else if (key.equals(DECLINE)) {
					playVoiceFile(ch, DECLINE_FILE_PATH);
					while (!(SSMFactory.ssmCheckPlay(ch) == 1//放音结束
							|| SSMFactory.ssmCheckPlay(ch)==4 //放音时对方挂机
							|| SSMFactory.ssmCheckPlay(ch)==2//放音时对方按了触发键
							)) {
						Thread.sleep(200);
					}
					break;
			    }  else if (key.equals(CallServer.CALL_STATUS_HUANG_UP)){
					break;
		        }	
				if(key.equals("")){
					key = getKeyStr(ch);
				}
				// 清空按键缓冲区
				rlt = SSMFactory.ssmClearRxDtmfBuf(ch);
				if (rlt == -1) {
				  errorMsg="清空按键缓冲区失败。" + getLastErrMsg();
					logger.error(errorMsg);
					//SSMFactory.ssmHangup(ch);
					updateCallingStatus(CallServer.CALL_STATUS_SYS_ERROR, key,emExpertCode, objId, emBillCode, isCall, record,expertType,errorMsg);
					return;
				}
				Thread.sleep(200);
				timmer += 200;
			}
			rlt = SSMFactory.ssmHangup(ch);
			if (rlt == -1) {
				logger.error("挂机失败。" + getLastErrMsg());
				//return;
			}
			
			if(key.equals(CallServer.AGREE)){
        logger.error("同意。key=" + CallServer.AGREE);
				updateCallingStatus(CallServer.AGREE,key, emExpertCode, objId, emBillCode, isCall,record,expertType,null);
			}else if(key.equals(CallServer.DECLINE)){
        logger.error("拒绝。key=" + CallServer.DECLINE);
				updateCallingStatus(CallServer.DECLINE,key, emExpertCode, objId, emBillCode, isCall,record,expertType,null);
			}else{
				updateCallingStatus(CallServer.CALL_STATUS_END,key, emExpertCode, objId, emBillCode, isCall,record,expertType,null);
			}			
		} catch (Exception e) {		  
      updateCallingStatus(CallServer.CALL_STATUS_SYS_ERROR, key,emExpertCode, objId, emBillCode, isCall, record,expertType,null);
			try {
        SSMFactory.ssmStopPlay(ch);
        SSMFactory.ssmHangup(ch);
      } catch (Exception e1) {
        throw new EmCallException("停止放音和挂机失败"+e1.getMessage(),e1);
      }
			throw new EmCallException(e.getMessage(),e);
		}
	}
	/**
	 * 获取放音是否结束以及放音过程中的按键信息
	 * @param ch 通道
	 * @return
	 * @throws InterruptedException
	 * @throws NativeException
	 * @throws IllegalAccessException
	 */
	 
	private String  getKeyByCalling(int ch) throws InterruptedException, NativeException, IllegalAccessException {
		String key="";
		int rlt=-2;
		boolean voiceStartFlag=false;
		// 1:全部数据播放完毕
		while (rlt != 1) {
			// 对方已经挂机，播放文件退出
			if (SSMFactory.ssmGetChState(ch) == 7) {
				SSMFactory.ssmHangup(ch);
				key = getKeyStr(ch);	
				return key;
			}
			if(!voiceStartFlag){//开始放音，5秒钟后检测状态
				voiceStartFlag=true;
			}

			key = getKeyStr(ch);			
			logger.debug("=====================key="+key);
			if (key.equals(PLAY_AGAIN) || key.equals(AGREE)
					|| key.equals(DECLINE)) {
				// SSMFactory.ssmHangup(ch);
				rlt = 1;
				break;
			}
			
			rlt = SSMFactory.ssmCheckPlay(ch);

			logger.debug("=====================rlt="+rlt);
			if(rlt==4){//放音时对方挂机了
				rlt=1;
				key=CALL_STATUS_HUANG_UP;
			}
			
			
			SSMFactory.ssmClearRxDtmfBuf(ch);
			//每隔1秒检查一次通话状态
			Thread.sleep(1000);
		}
		if(key.equals("")){
			key=CALL_STATUS_VOICE_FILE_END;
		}
		return key;
	}

	/**
	 * 放音
	 * @param ch
	 * @param playFilePath
	 * @return
	 */
	private void playVoiceFile(int ch, String playFilePath) {
		try {
			int rlt = SSMFactory.ssmPlayFile(ch, playFilePath, 6, 0,
					Integer.MAX_VALUE);
		} catch (NativeException e) {
			// TCJLODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TCJLODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	/**
	 * 拨打电话过程中根据拨打和相应情况更新数据库相应业务数据
	 * 
	 * @param callStatus
	 *            拨打电话过程状态
	 * @param key
	 *            接收到的用户按键
	 * @param emExpertCode
	 *            专家代码
	 * @param objId
	 * @param emBillCode
	 *            抽取单据号
	 * @param isCall
	 * @param record
	 * @param errorMsg 
	 * @throws Exception
	 */
	void updateCallingStatus(String callStatus, String key,String emExpertCode, String objId, String emBillCode,int isCall, int record,String expertType, String errorMsg) {
//		List<String> sqlList = new ArrayList<String>();
//		List<Object[]> paramList = new ArrayList<Object[]>();
		if(callStatus.equals(CallServer.CALL_STATUS_BEGIN)){//开始呼叫		
			//插入呼叫记录
			String sql="insert into EM_CALL_EXPERT_RECORD(em_bill_code,em_expert_code,call_num,call_time,call_state) values(?,?,?,sysdate,?)";
			Object[] params = new Object[] { emBillCode, emExpertCode, isCall, CallServer.CALL_STATUS_BEGIN};
//			sqlList.add(sql);
//			paramList.add(params);
			excuteSql(sql, params);
			
			//更新当前单据、当前专家的呼叫次数
			sql="update EM_CALL_SERVER_LIST set ISCALL =? where OBJID = ?  ";
			params = new Object[] { isCall, objId};
//			sqlList.add(sql);
//			paramList.add(params);
//      excuteSql(sql, params);
	
		}else if(callStatus.equals(CallServer.YUYIN_TONGDAO_YICHANG)){//获取语音通道异常	
			String sql=CallServer.UPDATE_EM_CALL_EXPERT_RECORD;
			Object[] params = new Object[] { CallServer.YUYIN_TONGDAO_YICHANG,emBillCode, emExpertCode, isCall};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
      
      sql=CallServer.UPDATE_EM_CALL_SERVER_LIST_TO_OVER;
      params = new Object[] {Integer.valueOf(-1),Integer.parseInt(objId)};
//      sqlList.add(sql);
//      paramList.add(params);
      excuteSql(sql, params);
			
		}else if(callStatus.equals(CallServer.CALL_STATUS_NULL_PHONE_NUMBER)){//空号
			String sql=CallServer.UPDATE_EM_CALL_SERVER_LIST_TO_OVER;
			Object[] params = new Object[] {Integer.valueOf(-1),Integer.parseInt(objId)};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
			
			sql=CallServer.UPDATE_EM_CALL_EXPERT_RECORD;
//			params = new Object[] { CallServer.CALL_STATUS_NULL_PHONE_NUMBER,emBillCode, emExpertCode, isCall};
      params = new Object[] { CallServer.CALL_STATUS_NULL_PHONE_NUMBER,emBillCode, emExpertCode, isCall};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);		
			
		}else if(callStatus.equals(CallServer.CALL_STATUS_NO_RESPONSE)){//无人接听
			String sql=CallServer.UPDATE_EM_CALL_EXPERT_RECORD;
			Object[] params = new Object[] { CallServer.CALL_STATUS_NO_RESPONSE,emBillCode, emExpertCode, isCall};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);

       sql=CallServer.UPDATE_EM_CALL_SERVER_LIST_TO_OVER;
       params = new Object[] {Integer.valueOf(-1),Integer.parseInt(objId)};
//      sqlList.add(sql);
//      paramList.add(params);
      excuteSql(sql, params);
		}else if(callStatus.equals(CallServer.CALL_STATUS_BEGIN_TALKING)){//开始通话
			String sql=CallServer.UPDATE_EM_CALL_EXPERT_RECORD;
			Object[] params = new Object[] { CallServer.CALL_STATUS_BEGIN_TALKING,emBillCode, emExpertCode, isCall};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
		}else if(callStatus.equals(CallServer.CALL_STATUS_HUANG_UP)){//挂机
			String sql=CallServer.UPDATE_EM_CALL_EXPERT_RECORD;
			Object[] params = new Object[] { CallServer.CALL_STATUS_HUANG_UP,emBillCode, emExpertCode, isCall};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
		}else if(callStatus.equals(CallServer.CALL_STATUS_SYS_ERROR)){//呼叫异常
			String sql=CallServer.UPDATE_EM_CALL_EXPERT_RECORD;
			Object[] params = new Object[] { CallServer.CALL_STATUS_SYS_ERROR,emBillCode, emExpertCode, isCall};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
      sql=CallServer.UPDATE_EM_CALL_SERVER_LIST_TO_OVER;
      params = new Object[] {Integer.valueOf(-1),Integer.parseInt(objId)};
//      sqlList.add(sql);
//      paramList.add(params);
      excuteSql(sql, params);
		}else if(callStatus.equals(CallServer.CALL_STATUS_FAIL)){//呼叫失败
			String sql=CallServer.UPDATE_EM_CALL_EXPERT_RECORD;
			Object[] params = new Object[] { CallServer.CALL_STATUS_FAIL,emBillCode, emExpertCode, isCall};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
      
      sql=CallServer.UPDATE_EM_CALL_SERVER_LIST_TO_OVER;
      params = new Object[] {Integer.valueOf(-1),Integer.parseInt(objId)};
//      sqlList.add(sql);
//      paramList.add(params);
      excuteSql(sql, params);
		}else if(callStatus.equals(CallServer.AGREE)){// 专家参加评审
			String sql=CallServer.INSERT_EM_EXPERT_EVALUATION;
			Object[] params = new Object[] {emBillCode, emExpertCode,expertType, "1", "9"};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
			
			sql=CallServer.UPDATE_EM_CALL_SERVER_LIST_TO_OVER;
			params = new Object[] {Integer.valueOf(-9),Integer.parseInt(objId)};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
			
			sql=UPDATE_EM_CALL_EXPERT_RECORD;
			params = new Object[] {"9",emBillCode, emExpertCode, isCall};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
		}else if(callStatus.equals(CallServer.DECLINE)){// 专家不参加评审
			String sql=CallServer.UPDATE_EM_CALL_SERVER_LIST_TO_OVER;
			Object[] params = new Object[] {Integer.valueOf(-8),Integer.parseInt(objId)};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
			
			sql=CallServer.UPDATE_EM_CALL_EXPERT_RECORD;
			params = new Object[] {"8",emBillCode, emExpertCode, isCall};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
		}
	/*	else if(isCall==CALL_NUM){// 达到呼叫次数，无人应答
			String sql=CallServer.UPDATE_EM_CALL_EXPERT_RECORD;
			Object[] params = new Object[] {CallServer.CALL_STATUS_NO_RESPONSE,emBillCode, emExpertCode, isCall};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
      
      sql=CallServer.UPDATE_EM_CALL_SERVER_LIST_TO_FAIL;
      params = new Object[] {Integer.parseInt(objId)};
//      sqlList.add(sql);
//      paramList.add(params);
      excuteSql(sql, params);
		}*/
		else if(callStatus.equals(CallServer.CALL_STATUS_END)){// 呼叫结束,没有获取按键信息
			String sql=CallServer.UPDATE_EM_CALL_EXPERT_RECORD;
			Object[] params = new Object[] {CallServer.CALL_STATUS_NO_RESPONSE,emBillCode, emExpertCode,  isCall};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
      
      sql=CallServer.UPDATE_EM_CALL_SERVER_LIST_TO_OVER;
      params = new Object[] {Integer.valueOf(-1),Integer.parseInt(objId)};
      excuteSql(sql, params);
		}else if(callStatus.equals(CallServer.CALL_STATUS_MAKE_VOICE_FILE_FAIL)){// 生成语音文件失败
			String sql=CallServer.UPDATE_EM_CALL_EXPERT_RECORD;
			Object[] params = new Object[] {CallServer.CALL_STATUS_MAKE_VOICE_FILE_FAIL,emBillCode, emExpertCode,  isCall};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
			
			sql=CallServer.UPDATE_EM_CALL_SERVER_LIST_TO_OVER;
			params = new Object[] {Integer.valueOf(-1),Integer.parseInt(objId)};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
			
		}else if(callStatus.equals(CallServer.CALL_STATUS_PAUSE)){// 暂停抽取
			String sql=CallServer.UPDATE_EM_CALL_EXPERT_RECORD;
			Object[] params = new Object[] {CallServer.CALL_STATUS_PAUSE,emBillCode, emExpertCode,  isCall};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
			
			/*sql=CallServer.DELETE_EM_CALL_SERVER_LIST_BY_ID;
			params = new Object[] {Integer.parseInt(objId)};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);*/
			
		}else if(callStatus.equals(CallServer.CALL_STATUS_BUSY)){// 用户忙
			String sql=CallServer.UPDATE_EM_CALL_EXPERT_RECORD;
			Object[] params = new Object[] {CallServer.CALL_STATUS_BUSY,emBillCode, emExpertCode,  isCall};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
      
      sql=CallServer.UPDATE_EM_CALL_SERVER_LIST_TO_OVER;
      params = new Object[] {Integer.valueOf(-1),Integer.parseInt(objId)};
//      sqlList.add(sql);
//      paramList.add(params);
      excuteSql(sql, params);
		}else{
			String sql=CallServer.UPDATE_EM_CALL_EXPERT_RECORD;
			Object[] params = new Object[] {CallServer.CALL_STATUS_END,emBillCode, emExpertCode,  isCall};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
		}
		// 更新数据
//		excuteSql(sqlList, paramList);
			
	}
	
	private void excuteSql(String sql,Object[] params){
    try {
      DAOFactory df=new DAOFactory();
      df.executeUpdate(sql, params);
      logger.debug(sql);
      if(params!=null){
        for(int i=0;i<params.length;i++){
          logger.debug(params[i]);
        }
      }
    } catch (EmCallException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      logger.error(e.getMessage(), e);
    }
	}

	private void excuteSql(List<String> sqlList, List<Object[]> paramList) throws EmCallException {
		// TCJLODO Auto-generated method stub
		if (sqlList == null || sqlList.size() == 0) {
			return;
		}
		if (paramList == null || sqlList.size() != paramList.size()) {
			throw new EmCallException("更新拨打状态的脚本不匹配");
		} 
			for (int i = 0; i < sqlList.size(); i++) {
				String sql = sqlList.get(i);
				Object[] params = paramList.get(i);
	      DAOFactory df=new DAOFactory();
				df.executeUpdate(sql, params);
	      logger.debug(sql);
	      if(params!=null){
	        for(int j=0;j<params.length;j++){
	          logger.debug(params[j]);
	        }
	      }
			} 
	}
}
