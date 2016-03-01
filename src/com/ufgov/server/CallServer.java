package com.ufgov.server;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.xvolks.jnative.exceptions.NativeException;
import org.xvolks.jnative.pointers.Pointer;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.Discarder;
import com.ufgov.ssm.SSMFactory;
import com.ufgov.tts.TTSFactory;
import com.ufgov.util.ApplicationContext;
import com.ufgov.util.DAOFactory;
import com.ufgov.util.EmCallException;
import com.ufgov.util.ExpertUtil;

/**
 * �����̣߳������д����е�ר�ң������Ͻ��к���
 * @author Administrator
 *
 */
public class CallServer extends Thread {
	private static Logger logger = Logger.getLogger(CallServer.class);

	//��ȡ�ȴ�����绰��ר�ң�������ͬ����߾ܾ���ר��
	public static final String GET_EM_CALL_SERVER_LIST = "SELECT C.* FROM EM_CALL_SERVER_LIST C, ZC_EM_EXPERT_PRO_BILL B WHERE C.EM_BILL_CODE = B.EM_BILL_CODE  AND B.EM_BILL_STATUS = 'SELECTING' AND C.ISCALL < ? AND C.ISCALL >= ? " +
			" and c.em_expert_code not in("+
       " select e.em_expert_code from ZC_EM_EXPERT_EVALUATION e where e.em_bill_code = b.em_bill_code "+//ͬ��μӵĲ��ٲ���绰
       " union "+
       " select distinct vr.em_expert_code from (select r.em_bill_code,r.em_expert_code from em_call_expert_record r where r.call_num = ? or r.call_state = '8') vr where vr.em_bill_code = c.em_bill_code and vr.em_expert_code = c.em_expert_code)";	//���м�¼�ﵽ1�Ρ���ȷ�ܾ��Ĳ��ٲ���绰

	//private static final String GET_EM_CALL_EXPERT_NUM = "SELECT NVL(MAX(D.CALL_NUM),0) AS CALL_NUM,CALL_STATE FROM EM_CALL_EXPERT_RECORD D WHERE D.EM_BILL_CODE =? AND D.EM_EXPERT_CODE =?";
	
	private static final String GET_EM_CALL_EXPERT_NUM = "SELECT M.*,D.CALL_STATE  FROM EM_CALL_EXPERT_RECORD D,(SELECT NVL(MAX(D.CALL_NUM), 0) AS CALL_NUM,EM_BILL_CODE,EM_EXPERT_CODE FROM EM_CALL_EXPERT_RECORD D WHERE D.EM_BILL_CODE = ?  AND D.EM_EXPERT_CODE = ? GROUP BY EM_BILL_CODE,EM_EXPERT_CODE) M  WHERE D.EM_BILL_CODE = ? AND D.EM_EXPERT_CODE = ? AND D.CALL_NUM = M.CALL_NUM";
	
	//����ѡ�񵽵�ר��
  private static final String INSERT_EM_EXPERT_EVALUATION = "INSERT INTO ZC_EM_EXPERT_EVALUATION (EM_BILL_CODE, EM_EXPERT_CODE, EM_EXPERT_TYPE_CODE, EM_NOTICE_STATUS,EM_RESPONSE_STATUS) VALUES (?, ?, ?, ?,?)";

//	private static final String UPDATE_CALL_SERVER_STATE = "update EM_CALL_SERVER_LIST set ISCALL = ISCALL+1 where OBJID=?";

	private static final String UPDATE_EM_CALL_SERVER_LIST_TO_OVER = "update EM_CALL_SERVER_LIST set ISCALL = ? where OBJID=?";
	
	private static final String DELETE_EM_CALL_SERVER_LIST_BY_ID = "delete from  EM_CALL_SERVER_LIST  where OBJID=?";

//	private static final String UPDATE_EM_EXPERT_EVALUATION = "update ZC_EM_EXPERT_EVALUATION set EM_RESPONSE_STATUS =? where EM_EXPERT_CODE = ? and EM_BILL_CODE = ?";

	private static final String UPDATE_EM_CALL_EXPERT_RECORD = "update EM_CALL_EXPERT_RECORD set call_state=? where em_bill_code=? and em_expert_code=? and call_num=?";

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
	 * ��ʼ����
	 */
	private static final String CALL_STATUS_BEGIN = "beging_call";

	/*
	 * ����ʧ��
	 */
	private static final String CALL_STATUS_FAIL = "call_fail";

	/*
	 * ���ɺ��������ļ�ʧ��
	 */
	private static final String CALL_STATUS_MAKE_VOICE_FILE_FAIL = "voice_file_fail";
	
	/*
	 * ��ͣ��ȡ
	 */
	private static final String CALL_STATUS_PAUSE = "select_pause";

	/*
	 * �պ�
	 */
	private static final String CALL_STATUS_NULL_PHONE_NUMBER = "null_phone_number";

	/*
	 * ���˽���
	 */
	private static final String CALL_STATUS_NO_RESPONSE = "no_response";

	/*
	 * �Է���æ
	 */
	private static final String CALL_STATUS_BUSY = "busy";

	/*
	 * ��ʼͨ��
	 */
	private static final String CALL_STATUS_BEGIN_TALKING = "begin_talking";

	/*
	 * ���������ļ�ʧ��
	 */
	private static final String CALL_STATUS_PLAY_VOICE_FILE_FAIL = "play_voice_file_fail";

	/*
	 * ͨ���쳣
	 */
	private static final String CALL_STATUS_SYS_ERROR = "sys_error";

	/*
	 * �һ�
	 */
	private static final String CALL_STATUS_HUANG_UP = "hang_up";

/*
 * �����ļ����Ž���
 */
	private static final String CALL_STATUS_VOICE_FILE_END = "voice_file_end";

	/*
	 * ͨ������
	 */
	private static final String CALL_STATUS_END = "end_call";
	
	/*
	 * ��ȡ����ͨ���쳣
	 */
	private static final String YUYIN_TONGDAO_YICHANG = "yuyin_tongdao_yichang";

	



	/**
	 * ����ר�ҵĴ�����Ĭ���Ǹ�ר�Ҵ�3�ε绰
	 */
	public static int CALL_NUM = new Integer(1);

	private static boolean generalVoiceFlag = false;

	public static Map<String, String> callingMap = new HashMap<String, String>();

	private TTSFactory tsf = null;

	private  String objID="";
	
	private  int threadNum =0; 

	/**
	 * Ψһ��ʶ
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
			logger.error("���ɽ������������ļ�ʧ�ܡ�");
			return false;
		}
		flag = tsf.ttsPlayToFile( DECLINE_FILE_PATH, declineText);
		if (!flag) {
			logger.error("���ɾܾ����������ļ�ʧ�ܡ�");
			return false;
		}
		flag = tsf.ttsPlayToFile( REPLAY_FILE_PATH, replayText);
		if (!flag) {
			logger.error("�����������������ļ�ʧ�ܡ�");
			return false;
		}
		logger.info("��ʼ���������롢�ܾ����롢�������������ļ��ɹ���");

		CallServer.generalVoiceFlag = true;

		return true;
	}

	private boolean initConfig() {
		agreeText = ApplicationContext.singleton().getValueAsString("agreeText");
		declineText = ApplicationContext.singleton().getValueAsString("declineText");
		replayText = ApplicationContext.singleton().getValueAsString("replayText");
		if (isEmpty(agreeText) || isEmpty(declineText) || isEmpty(replayText)) {
			logger.error("��ȡͬ�⡢�ܾ������������ı�ʧ�ܣ����������ļ���");
			return false;
		}
		String callNumStr = ApplicationContext.singleton().getValueAsString("callNum");
		if (isEmpty(callNumStr)) {
			logger.error("��ȡɨ�����ʧ�ܣ�ϵͳ����Ĭ������������");
		} else {
			CALL_NUM = Integer.parseInt(callNumStr.trim());
		}
		logger.info("��ʼ��TTS�����ı���Ϣ�ɹ���");
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
	    logger.error("ϵͳ��ʼ��ʧ��,ϵͳ�˳�.");
			System.exit(0);
		}
		//
		logger.info("��ʼ��ѯ���������");

		while (true) {
			try {
				objID="";
				scan();
				ExpertUtil.compeletCalling(objID);
				sleep(3000);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e.getMessage(), e);
				ExpertUtil.compeletCalling(objID);	
				clearCallServer();
			}
		}

	}

	private void clearCallServer() {
		if (ServiceContext.callServerList.contains(this)) {
			ServiceContext.callServerList.remove(this);
		}
	}

	public void scan(){
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
			updateCallingStatus(CallServer.CALL_STATUS_PAUSE, null,emExpertCode, objID, emBillCode, callNum, 0,expertType);
			return;
		}
		
		
		File f = new File(playFilePath);
		if (!f.exists()) {			
			boolean flag = tsf.ttsPlayToFile( playFilePath,emCallMSG);
			if (!flag) {
				logger.error("���������ļ�ʧ�ܣ�" + playFilePath);
				updateCallingStatus(CallServer.CALL_STATUS_MAKE_VOICE_FILE_FAIL, null,emExpertCode, objID, emBillCode, callNum, 0,expertType);
				throw new EmCallException("�����˳������������ļ�ʧ�ܣ�" + playFilePath);
			}
		}		
		
		logger.info("��" +  callNum + "�κ��е绰:"+ emMobile);		
		logger.info("�߳�"+threadNum+" ��ȡ����==="+emBillCode+" objid===="+objID);
		call(emMobile, playFilePath, emExpertCode, objID, emBillCode, callNum,expertType);	
		}catch(EmCallException e){
		  e.printStackTrace();
		  logger.error(e.getMessage(), e);
		}
	}

	/**
	 * ��ǰ�����Ƿ��ڳ�ȡ״̬
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
			pointer.dispose();
		} else if (rlt == -1) {
			logger.info("��ȡ��������" + getLastErrMsg());
		}
		return key;
	}

	public boolean isRealPersonPickedUp(int ch) throws NativeException,
			IllegalAccessException {
		int rlt = SSMFactory.ssmClearAMDResult(ch);
		if (rlt == -1) {
			logger.info("���AMD�㷨���ʧ�ܡ�" + getLastErrMsg());
			return false;
		}
		rlt = SSMFactory.ssmGetAMDResult(ch);
		logger.info("����AMD�㷨���ʧ�ܡ�" + getLastErrMsg());
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
		try {
			// ��ʼ��绰
			updateCallingStatus(CallServer.CALL_STATUS_BEGIN, key,emExpertCode, objId, emBillCode, isCall, record,expertType);
			// ��ȡ����ͨ��
			int timmer = 0;
			while (ch == -1 && timmer < 10000) {
				ch = SSMFactory.ssmSearchIdleCallOutCh(1, 0);
				Thread.sleep(10);
				timmer += 10;
			}
			if (ch == -1) {
				logger.error("��ȡ����ͨ��ʧ�ܡ�" + getLastErrMsg());
				updateCallingStatus(CallServer.YUYIN_TONGDAO_YICHANG, key,emExpertCode, objId, emBillCode, isCall, record,expertType);
				return;
			}			

			rlt = SSMFactory.ssmSetHangupStopPlayFlag(ch, 1);
			if (rlt == -1) {
				logger.error("���ò���ʧ�ܡ�" + getLastErrMsg());
				updateCallingStatus(CallServer.YUYIN_TONGDAO_YICHANG, key,emExpertCode, objId, emBillCode, isCall, record,expertType);
				return;
			}
			rlt = SSMFactory.SsmSetDtmfStopPlay(ch, true);
			if (rlt == -1) {
				logger.error("���ò���ʧ�ܡ�" + getLastErrMsg());
				updateCallingStatus(CallServer.YUYIN_TONGDAO_YICHANG, key,emExpertCode, objId, emBillCode, isCall, record,expertType);
				return;
			}
			// ժ��
			rlt = SSMFactory.ssmPickup(ch);
			if (rlt == -1) {
				logger.error("ժ��ʧ�ܡ�" + getLastErrMsg());
				SSMFactory.ssmHangup(ch);
				updateCallingStatus(CallServer.YUYIN_TONGDAO_YICHANG, key,emExpertCode, objId, emBillCode, isCall, record,expertType);
				return;
			}
			// ����
			rlt = SSMFactory.ssmAutoDial(ch, emMobile);
			if (rlt == -1) {
				logger.error("����ʧ�ܡ�" + getLastErrMsg());
				SSMFactory.ssmHangup(ch);
				updateCallingStatus(CallServer.CALL_STATUS_FAIL, key,emExpertCode, objId, emBillCode, isCall, record,expertType);
				return;
			}
			// ����ժ��
			timmer = 0;
			int chkAuto = -2;
			// 12�������û�����Ϊ�պţ�DIAL_VOICE�������û�ժ����AutoDial�������
			while (chkAuto != 7 && chkAuto != 12 && timmer < 30000) {
				chkAuto = SSMFactory.ssmChkAutoDial(ch);
				Thread.sleep(200);
				timmer += 200;
			}
			SSMFactory.ssmClearAMDResult(ch);
			//�����30���ʱ����û�в�ͨ�绰����Ϊ����ʧ��					
			if (chkAuto == -1 || timmer == 30000 || chkAuto == 12) {
				logger.error("ר��(���:"+emExpertCode+")�绰����Ϊ�պţ���˶�ר�ҿ��е�ר�ҵ绰���룡");
				SSMFactory.ssmHangup(ch);
				updateCallingStatus(CallServer.CALL_STATUS_NULL_PHONE_NUMBER, key,emExpertCode, objId, emBillCode, isCall, record,expertType);
				return;
			}
			if (chkAuto == 4) {
				logger.info("�Է��û�æ��");
				SSMFactory.ssmHangup(ch);
				updateCallingStatus(CallServer.CALL_STATUS_BUSY, key,emExpertCode, objId, emBillCode, isCall, record,expertType);
				return;
			}
			if (chkAuto != 7) {
				logger.info("����δժ����");
				SSMFactory.ssmHangup(ch);
				updateCallingStatus(CallServer.CALL_STATUS_NO_RESPONSE, key,emExpertCode, objId, emBillCode, isCall, record,expertType);
				return;
			}
			//��ʼͨ��
			updateCallingStatus(CallServer.CALL_STATUS_BEGIN_TALKING, key,emExpertCode, objId, emBillCode, isCall, record,expertType);

			// ���ŵ����ļ��������ļ���������
			sleep(2000);//�ȴ����뿪ʼ����
			//����
			playVoiceFile(ch, playFilePath);
			rlt = -2;
			//��ȡ�����Ƿ�����Լ����������еİ�����Ϣ
			String kk=getKeyByCalling(ch);
			if(kk.equals(CallServer.CALL_STATUS_HUANG_UP)){
				updateCallingStatus(CallServer.CALL_STATUS_HUANG_UP, key,emExpertCode, objId, emBillCode, isCall, record,expertType);
				key = CallServer.CALL_STATUS_HUANG_UP;
			}else if(kk.equals(CallServer.AGREE)||kk.equals(CallServer.DECLINE)||kk.equals(CallServer.PLAY_AGAIN)){
				key=kk;
			}

			// ��հ���������
			rlt = SSMFactory.ssmClearRxDtmfBuf(ch);
			if (rlt == -1) {
				logger.error("��հ���������ʧ�ܡ�" + getLastErrMsg());
				SSMFactory.ssmHangup(ch);
				updateCallingStatus(CallServer.CALL_STATUS_SYS_ERROR, key,emExpertCode, objId, emBillCode, isCall, record,expertType);
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
						updateCallingStatus(CallServer.CALL_STATUS_HUANG_UP, key,emExpertCode, objId, emBillCode, isCall, record,expertType);
					}else if(kk.equals(CallServer.AGREE)||kk.equals(CallServer.DECLINE)||kk.equals(CallServer.PLAY_AGAIN)){
						key=kk;
					}
				} else if (key.equals(AGREE)) {
					playVoiceFile(ch, AGREE_FILE_PATH);
					while (!(SSMFactory.ssmCheckPlay(ch) == 1//��������
							|| SSMFactory.ssmCheckPlay(ch)==4 //����ʱ�Է��һ�
							|| SSMFactory.ssmCheckPlay(ch)==2//����ʱ�Է����˴�����
							)) {
						Thread.sleep(200);
					}
					break;
				} else if (key.equals(DECLINE)) {
					playVoiceFile(ch, DECLINE_FILE_PATH);
					while (!(SSMFactory.ssmCheckPlay(ch) == 1//��������
							|| SSMFactory.ssmCheckPlay(ch)==4 //����ʱ�Է��һ�
							|| SSMFactory.ssmCheckPlay(ch)==2//����ʱ�Է����˴�����
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
				// ��հ���������
				rlt = SSMFactory.ssmClearRxDtmfBuf(ch);
				if (rlt == -1) {
					logger.error("��հ���������ʧ�ܡ�" + getLastErrMsg());
					//SSMFactory.ssmHangup(ch);
					updateCallingStatus(CallServer.CALL_STATUS_SYS_ERROR, key,emExpertCode, objId, emBillCode, isCall, record,expertType);
					return;
				}
				Thread.sleep(200);
				timmer += 200;
			}
			rlt = SSMFactory.ssmHangup(ch);
			if (rlt == -1) {
				logger.error("�һ�ʧ�ܡ�" + getLastErrMsg());
				//return;
			}
			
			if(key.equals(CallServer.AGREE)){
				updateCallingStatus(CallServer.AGREE,key, emExpertCode, objId, emBillCode, isCall,record,expertType);
			}else if(key.equals(CallServer.DECLINE)){
				updateCallingStatus(CallServer.DECLINE,key, emExpertCode, objId, emBillCode, isCall,record,expertType);
			}else{
				updateCallingStatus(CallServer.CALL_STATUS_END,key, emExpertCode, objId, emBillCode, isCall,record,expertType);
			}			
		} catch (Exception e) {		  
      updateCallingStatus(CallServer.CALL_STATUS_SYS_ERROR, key,emExpertCode, objId, emBillCode, isCall, record,expertType);
			try {
        SSMFactory.ssmStopPlay(ch);
        SSMFactory.ssmHangup(ch);
      } catch (Exception e1) {
        throw new EmCallException("ֹͣ�����͹һ�ʧ��"+e1.getMessage(),e1);
      }
			throw new EmCallException(e.getMessage(),e);
		}
	}
	/**
	 * ��ȡ�����Ƿ�����Լ����������еİ�����Ϣ
	 * @param ch ͨ��
	 * @return
	 * @throws InterruptedException
	 * @throws NativeException
	 * @throws IllegalAccessException
	 */
	 
	private String  getKeyByCalling(int ch) throws InterruptedException, NativeException, IllegalAccessException {
		String key="";
		int rlt=-2;
		boolean voiceStartFlag=false;
		// 1:ȫ�����ݲ������
		while (rlt != 1) {
			// �Է��Ѿ��һ��������ļ��˳�
			if (SSMFactory.ssmGetChState(ch) == 7) {
				SSMFactory.ssmHangup(ch);
				key = getKeyStr(ch);	
				return key;
			}
			if(!voiceStartFlag){//��ʼ������5���Ӻ���״̬
				voiceStartFlag=true;
			}

			key = getKeyStr(ch);			
			
			if (key.equals(PLAY_AGAIN) || key.equals(AGREE)
					|| key.equals(DECLINE)) {
				// SSMFactory.ssmHangup(ch);
				rlt = 1;
				break;
			}
			
			rlt = SSMFactory.ssmCheckPlay(ch);
			
			if(rlt==4){//����ʱ�Է��һ���
				rlt=1;
				key=CALL_STATUS_HUANG_UP;
			}
			
			
			SSMFactory.ssmClearRxDtmfBuf(ch);
			//ÿ��0.3����һ��ͨ��״̬
			Thread.sleep(300);
		}
		if(key.equals("")){
			key=CALL_STATUS_VOICE_FILE_END;
		}
		return key;
	}

	/**
	 * ����
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
	 * ����绰�����и��ݲ������Ӧ����������ݿ���Ӧҵ������
	 * 
	 * @param callStatus
	 *            ����绰����״̬
	 * @param key
	 *            ���յ����û�����
	 * @param emExpertCode
	 *            ר�Ҵ���
	 * @param objId
	 * @param emBillCode
	 *            ��ȡ���ݺ�
	 * @param isCall
	 * @param record
	 * @throws Exception
	 */
	void updateCallingStatus(String callStatus, String key,String emExpertCode, String objId, String emBillCode,int isCall, int record,String expertType) {
//		List<String> sqlList = new ArrayList<String>();
//		List<Object[]> paramList = new ArrayList<Object[]>();
		if(callStatus.equals(CallServer.CALL_STATUS_BEGIN)){//��ʼ����		
			//������м�¼
			String sql="insert into EM_CALL_EXPERT_RECORD(em_bill_code,em_expert_code,call_num,call_time,call_state) values(?,?,?,sysdate,?)";
			Object[] params = new Object[] { emBillCode, emExpertCode, isCall, CallServer.CALL_STATUS_BEGIN};
//			sqlList.add(sql);
//			paramList.add(params);
			excuteSql(sql, params);
			
			//���µ�ǰ���ݡ���ǰר�ҵĺ��д���
			sql="update EM_CALL_SERVER_LIST set ISCALL =? where OBJID = ?  ";
			params = new Object[] { isCall, objId};
//			sqlList.add(sql);
//			paramList.add(params);
//      excuteSql(sql, params);
	
		}else if(callStatus.equals(CallServer.YUYIN_TONGDAO_YICHANG)){//��ȡ����ͨ���쳣	
			String sql=CallServer.UPDATE_EM_CALL_EXPERT_RECORD;
			Object[] params = new Object[] { CallServer.YUYIN_TONGDAO_YICHANG,emBillCode, emExpertCode, isCall};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
			
		}else if(callStatus.equals(CallServer.CALL_STATUS_NULL_PHONE_NUMBER)){//�պ�
			String sql=CallServer.UPDATE_EM_CALL_SERVER_LIST_TO_OVER;
			Object[] params = new Object[] {Integer.valueOf(-1),Integer.parseInt(objId)};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
			
			sql=CallServer.UPDATE_EM_CALL_EXPERT_RECORD;
//			params = new Object[] { CallServer.CALL_STATUS_NULL_PHONE_NUMBER,emBillCode, emExpertCode, isCall};
      params = new Object[] { CallServer.CALL_STATUS_NO_RESPONSE,emBillCode, emExpertCode, isCall};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);		
			
		}else if(callStatus.equals(CallServer.CALL_STATUS_NO_RESPONSE)){//���˽���
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
		}else if(callStatus.equals(CallServer.CALL_STATUS_BEGIN_TALKING)){//��ʼͨ��
			String sql=CallServer.UPDATE_EM_CALL_EXPERT_RECORD;
			Object[] params = new Object[] { CallServer.CALL_STATUS_BEGIN_TALKING,emBillCode, emExpertCode, isCall};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
		}else if(callStatus.equals(CallServer.CALL_STATUS_HUANG_UP)){//�һ�
			String sql=CallServer.UPDATE_EM_CALL_EXPERT_RECORD;
			Object[] params = new Object[] { CallServer.CALL_STATUS_HUANG_UP,emBillCode, emExpertCode, isCall};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
		}else if(callStatus.equals(CallServer.CALL_STATUS_SYS_ERROR)){//�����쳣
			String sql=CallServer.UPDATE_EM_CALL_EXPERT_RECORD;
			Object[] params = new Object[] { CallServer.CALL_STATUS_SYS_ERROR,emBillCode, emExpertCode, isCall};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
		}else if(callStatus.equals(CallServer.CALL_STATUS_FAIL)){//����ʧ��
			String sql=CallServer.UPDATE_EM_CALL_EXPERT_RECORD;
			Object[] params = new Object[] { CallServer.CALL_STATUS_FAIL,emBillCode, emExpertCode, isCall};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
		}else if(callStatus.equals(CallServer.AGREE)){// ר�Ҳμ�����
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
		}else if(callStatus.equals(CallServer.DECLINE)){// ר�Ҳ��μ�����
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
	/*	else if(isCall==CALL_NUM){// �ﵽ���д���������Ӧ��
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
		else if(callStatus.equals(CallServer.CALL_STATUS_END)){// ���н���,û�л�ȡ������Ϣ
			String sql=CallServer.UPDATE_EM_CALL_EXPERT_RECORD;
			Object[] params = new Object[] {CallServer.CALL_STATUS_NO_RESPONSE,emBillCode, emExpertCode,  isCall};
//			sqlList.add(sql);
//			paramList.add(params);
      excuteSql(sql, params);
      
      sql=CallServer.UPDATE_EM_CALL_SERVER_LIST_TO_OVER;
      params = new Object[] {Integer.valueOf(-1),Integer.parseInt(objId)};
      excuteSql(sql, params);
		}else if(callStatus.equals(CallServer.CALL_STATUS_MAKE_VOICE_FILE_FAIL)){// ���������ļ�ʧ��
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
			
		}else if(callStatus.equals(CallServer.CALL_STATUS_PAUSE)){// ��ͣ��ȡ
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
			
		}else if(callStatus.equals(CallServer.CALL_STATUS_BUSY)){// �û�æ
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
		// ��������
//		excuteSql(sqlList, paramList);
			
	}
	
	private void excuteSql(String sql,Object[] params){
    try {
      DAOFactory df=new DAOFactory();
      df.executeUpdate(sql, params);
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
			throw new EmCallException("���²���״̬�Ľű���ƥ��");
		} 
			for (int i = 0; i < sqlList.size(); i++) {
				String sql = sqlList.get(i);
				Object[] params = paramList.get(i);
	      DAOFactory df=new DAOFactory();
				df.executeUpdate(sql, params);
			} 
	}
}
