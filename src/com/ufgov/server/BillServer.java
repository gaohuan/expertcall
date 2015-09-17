package com.ufgov.server;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.ufgov.util.ApplicationContext;
import com.ufgov.util.DAOFactory;

/**
 * ����ӽ���ר�ҳ�ȡ�����յ����������ȡ��ר�ҷ�������б��У��������߳̽��к��С�
 * @author Administrator
 *
 */
public class BillServer extends Thread {
  private static Logger logger = Logger.getLogger(BillServer.class);

  private static final String EM_BILL_PRO_STATUS_COMPLETE_SELECTION = "SELECT_FINISH";
  
  private static final String EM_BILL_PRO_STATUS_COMPLETE_FAIL = "SELECT_FAIL";

  private static final int EM_BILL_SERVER_STATUS_COMPLETE_SELECTION = 4;
  
  private static final int EM_BILL_SERVER_STATUS_COMPLETE_FAIL = 8;
  
 //�����Ҫ��ȡ�ĳ�ȡ��
  private static final String GET_BILL_SERVER_LIST_BY_STATUS = "SELECT * FROM EM_BILL_SERVER_LIST WHERE EM_STATUS = ?";

  //���³�ȡ��¼��״̬
  private static final String UPDATE_BILL_SERVER_STATUS = "UPDATE EM_BILL_SERVER_LIST SET EM_STATUS = ? WHERE EM_BILL_CODE = ?";

  //����ר�ҳ�ȡ����״̬
  private static final String UPDATE_EM_EXPERT_PRO_BILL_STATUS = "UPDATE ZC_EM_EXPERT_PRO_BILL SET EM_BILL_STATUS = ? WHERE EM_BILL_CODE = ?";

  //private static final String sqlUpdateDone = "update em_bill_server_list set ISCALL=? where em_bill_code=?";

  //  private static final String sqlInsertCall = "insert into EM_CALL_SERVER_LIST(OBJID,EM_EXPERT_CODE,EM_MOBILE,EM_CALL_MSG,ISCALL,EM_BILL_CODE,EM_PHONE_MSG)"
  //    + "values((select nvl(max(objid),0)+1 from EM_CALL_SERVER_LIST),?,?,?,?,?,?)";

  // ��ȡ��ǰ�����϶�Ӧ����ר�ң��Ѿ�����绰�ġ����˵�ר�Ҳ���ѡ��Χ�� ר���б� em_type_code like 'xxxx%' ֧��ѡȡ�����ȡ 
  private static final String GET_EXPERT_LIST = "SELECT * FROM ( SELECT * FROM ZC_EM_B_EXPERT WHERE EM_EXPERT_CODE NOT IN (SELECT EM_EXPERT_CODE  FROM EM_EXPERT_BILL_FILTER WHERE EM_BILL_CODE = ?) " +
  		"AND EM_EXPERT_CODE NOT IN (SELECT EM_EXPERT_CODE  FROM ZC_EM_EXPERT_EVALUATION  WHERE EM_BILL_CODE = ?) " +
		"AND EM_EXPERT_CODE NOT IN (SELECT L.EM_EXPERT_CODE FROM EM_CALL_SERVER_LIST L  WHERE L.EM_BILL_CODE=? AND L.EM_EXPERT_TYPE_CODE=?) " + 
  		"AND EM_EXPERT_CODE IN (SELECT EM_EXPERT_CODE FROM ZC_Em_Expert_Type_Join WHERE em_type_code like ?||'%') AND EM_EXP_STATUS='enable' ORDER BY dbms_random.VALUE ) WHERE rownum < 100";

  //��ǰ���ݵ�ר����𡢳�ȡ������������Ϣ��������Ϣ  
  private static final String GET_EVALUATION_CONDITION_LIST = "SELECT EC.EM_EXPERT_TYPE_CODE,EC.EXPERT_NUM,B.EM_CALL_INFO,B.EM_MSG_INFO FROM EM_EVALUATION_CONDITION EC, ZC_EM_EXPERT_PRO_BILL B WHERE EC.EM_BILL_CODE = B.EM_BILL_CODE AND B.EM_BILL_CODE = ?";

  //��ȡ��ǰ��ȡ������Ҫ��ȡ��ר������
  private static final String GET_EXPERT_NUM_FOR_SELECTION = "SELECT (B.NUM - A.NUM) NUM FROM (SELECT NVL(COUNT(*), 0) NUM FROM ZC_EM_EXPERT_EVALUATION WHERE EM_BILL_CODE = ? AND EM_RESPONSE_STATUS = ?) A, (SELECT NVL(SUM(EC.EXPERT_NUM), 0) NUM FROM EM_EVALUATION_CONDITION EC WHERE EC.EM_BILL_CODE = ?) B";

  //�Ѿ���ȡ��ר��,����ͬ��μӵ�ר��
  private static final String GET_SELECTED_EXPERT_NUM = "SELECT NVL(COUNT(EM_EXPERT_CODE), 0) NUM FROM ZC_EM_EXPERT_EVALUATION WHERE EM_BILL_CODE = ? AND EM_EXPERT_TYPE_CODE = ? AND EM_RESPONSE_STATUS='9'";

  //����ѡ�񵽵�ר�ң�����֪ͨ״̬Ϊ0,�ȴ�����绰
//  private static final String INSERT_EM_EXPERT_EVALUATION = "INSERT INTO ZC_EM_EXPERT_EVALUATION (EM_BILL_CODE, EM_EXPERT_CODE, EM_EXPERT_TYPE_CODE, EM_NOTICE_STATUS) VALUES (?, ?, ?, ?)";

  //���������绰��ר�Ҽ�¼����iscall��0������δ����
  private static final String INSERT_EM_CALL_SERVER_LIST = "INSERT INTO EM_CALL_SERVER_LIST (OBJID, EM_EXPERT_CODE, EM_MOBILE, ISCALL, EM_BILL_CODE, EM_CALL_MSG, EM_PHONE_MSG,em_expert_type_code) VALUES ((SELECT NVL(MAX(OBJID), 0) + 1 FROM EM_CALL_SERVER_LIST),?,?,?,?,?,?,?)";
  
  private static final String GET_CALLING_EXPERT_NUM = "SELECT COUNT(*) CALLING_NUM FROM EM_CALL_SERVER_LIST L WHERE L.ISCALL <>'-1' AND L.EM_BILL_CODE =? GROUP BY EM_BILL_CODE ";

  public void run() {
	  int scanIntervalTime=10000;
	String scanIntervalTimeStr=ApplicationContext.singleton().getValueAsString("scanIntervalTime");
	if(scanIntervalTimeStr!=null){
		scanIntervalTime=Integer.parseInt(scanIntervalTimeStr);
	}
    while (true) {
      try {
        scan();
        Thread.sleep(scanIntervalTime);
      } catch (Exception e) {
        logger.error("ɨ��ר�ҳ�ȡ�����쳣��" + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  private void scan() throws Exception {
    Object[] params = new Object[] { new Integer(0) };//��ȡ�ȴ���ȡ�ĳ�ȡ��
    List<Map<String, String>> scanList = DAOFactory.queryToListMap(GET_BILL_SERVER_LIST_BY_STATUS, params);
    if (scanList==null || scanList.size() < 1){
    	logger.info("��ǰ�޳�ȡ����" );
    	return;
    }
    for (Map<String, String> m : scanList) {
    	String emBillCode=m.get("EM_BILL_CODE");
    	if(existCallingRecord(emBillCode)){//���ڲ���绰�ĵ��ݲ�����ר�ҳ�ȡ������绰�ˣ��ڽ������ݼ�飬���Ƿ��Ƿ�Ҫ�ڳ�ȡ
    		continue;
    	}
    	selectExperts(emBillCode);
    }
  }
  
/**
 * �Ƿ���ڵȴ����м�¼
 * @param emBillCode
 * @return
 */
  private boolean existCallingRecord(String emBillCode){
		try {
		    Object[] params = new Object[] { emBillCode };
		    String sql="SELECT COUNT(*) AS SUM FROM EM_CALL_SERVER_LIST C WHERE C.ISCALL >=0 AND C.ISCALL<3 AND C.EM_BILL_CODE=?";
			Map<String, String> billMap = DAOFactory.queryToColumnMap(sql, params);
			if(billMap!=null){
				String sumStr=billMap.get("SUM");
				int sum=Integer.parseInt(sumStr==null?"0":sumStr);
				if(sum>0){
					return true;
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  return false;
  }
  /**
   * �Ƿ����ڳ�ȡ�ĳ�ȡ�������س�ȡ����״̬
   * @param emBillCode
   * @return
   */
private boolean isSelectingBill(String emBillCode){
	try {
	    Object[] params = new Object[] { emBillCode };
	    String sql="SELECT B.EM_BILL_STATUS FROM ZC_EM_EXPERT_PRO_BILL  B WHERE B.EM_BILL_CODE=?";
		Map<String, String> billMap = DAOFactory.queryToColumnMap(sql, params);
		if(billMap!=null){
			String billStatus=billMap.get("EM_BILL_STATUS");
			if("SELECTING".equals(billStatus)){
				return true;
			}
		}
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	return false;
}
  
private void selectExperts(String emBillCode) throws Exception {
    Object[] params = new Object[] { emBillCode, "9", emBillCode };//��ȡ��ǰ��ȡ������Ҫ��ȡ��ר���������������Ѿ�ͬ������ר�� EM_RESPONSE_STATUS,��Ӧ�����9-�μ� 8-���μ�
    Map<String, String> expertNumMap = DAOFactory.queryToColumnMap(GET_EXPERT_NUM_FOR_SELECTION, params);
    //��ȡ��ǰ������Ҫ��ȡר�ҵ�����
    int needExpertNum = Integer.parseInt(expertNumMap.get("NUM"));
    List failTypeLst=new ArrayList();
    List selectedExpertLst=new ArrayList<String>();
    Connection conn = null;
    try {
      conn = DAOFactory.getConnection();
      conn.setAutoCommit(false);
      if (needExpertNum > 0) {
        params = new Object[] { emBillCode };
        //��ǰ���ݵ�ר����𡢳�ȡ������������Ϣ��������Ϣ   
        List<Map<String, String>> ecList = DAOFactory.queryToListMap(GET_EVALUATION_CONDITION_LIST, params);
        //ר��������������� 
        for (Map<String, String> mcMap : ecList) {
          String emExpertTypeCode = mcMap.get("EM_EXPERT_TYPE_CODE");
          int needExpertNumWithType = Integer.parseInt(mcMap.get("EXPERT_NUM")==null?"0":mcMap.get("EXPERT_NUM"));//��Ӧר����������ר������
          
          String emCallInfo = mcMap.get("EM_CALL_INFO");
          String emMsgInfo = mcMap.get("EM_MSG_INFO");
          params = new Object[] { emBillCode, emExpertTypeCode };
          Map<String, String> senMap = DAOFactory.queryToColumnMap(GET_SELECTED_EXPERT_NUM, params);
          
          int selectedExpertNumWithType = Integer.parseInt(senMap.get("NUM")==null?"0":senMap.get("NUM"));//�������Ѿ�ѡ�񵽵�ר��
          if (needExpertNumWithType > selectedExpertNumWithType) {
            params = new Object[] { emBillCode, emBillCode, emBillCode,emExpertTypeCode,emExpertTypeCode };
            List<Map<String, String>> expertList = DAOFactory.queryToListMap(GET_EXPERT_LIST, params);
            //
            if(expertList==null && expertList.size()==0){//û���ѵ�ר�ң�˵��ר�Ҷ���ȡ���ˣ���û���ҵ�ר��
            	failTypeLst.add(emExpertTypeCode);
            }else{
                int j = 0;
                for (int i = 0; i < expertList.size(); i++) {
                  String expertCode = expertList.get(i).get("EM_EXPERT_CODE");
                  String emMobile = expertList.get(i).get("EM_MOBILE");
                  
                  if(selectedExpertLst.contains(expertCode)){
                	  continue;
                  }
                  
//                  params = new Object[] { emBillCode, expertCode, emExpertTypeCode, "0" };
//                  DAOFactory.executeUpdate(conn, INSERT_EM_EXPERT_EVALUATION, params);//����ѡ�񵽵�ר�ң���֪ͨ״̬Ϊ0,�ȴ�����绰              
                  
                  params = new Object[] { expertCode, emMobile, 0, emBillCode, emCallInfo, emMsgInfo,emExpertTypeCode};
                  DAOFactory.executeUpdate(conn, INSERT_EM_CALL_SERVER_LIST, params);//����ѡ�񵽵�ר�ҵ����б��У���iscall��0������δ����            
                  j++;
                  if (j >= needExpertNumWithType - selectedExpertNumWithType) {
                      //�����Ҫ��ȡ��ר������=(��Ҫ������-�Ѿ���ȡ����)
                    break;
                  }
                }
                if (j < needExpertNumWithType - selectedExpertNumWithType ) {
                	failTypeLst.add(emExpertTypeCode);
                }            	
            }
          }
        }
      } else {
        params = new Object[] { EM_BILL_PRO_STATUS_COMPLETE_SELECTION, emBillCode };
        DAOFactory.executeUpdate(conn, UPDATE_EM_EXPERT_PRO_BILL_STATUS, params);
        params = new Object[] { EM_BILL_SERVER_STATUS_COMPLETE_SELECTION, emBillCode };
        DAOFactory.executeUpdate(conn, UPDATE_BILL_SERVER_STATUS, params);
      }
      conn.commit();
      conn.setAutoCommit(true);
    } catch (Exception e) {
      conn.rollback();
      logger.info(e.getMessage());
      throw e;
    } finally {
      DAOFactory.closeConnection(conn, null, null);
    }
    

    //�����ȡ����ר�Ҳ������ҵ绰�������ˣ�����µ�ǰ��ȡ����״̬Ϊ��ȡʧ��
    if(failTypeLst.size()>0 && !existCallingRecord(emBillCode)){
    	conn=DAOFactory.getConnection();
    	conn.setAutoCommit(false);
        params = new Object[] { EM_BILL_PRO_STATUS_COMPLETE_FAIL, emBillCode };
        DAOFactory.executeUpdate(conn, UPDATE_EM_EXPERT_PRO_BILL_STATUS, params);
        params = new Object[] { EM_BILL_SERVER_STATUS_COMPLETE_FAIL, emBillCode };
        DAOFactory.executeUpdate(conn, UPDATE_BILL_SERVER_STATUS, params);
        conn.commit();
        conn.setAutoCommit(true);
        DAOFactory.closeConnection(conn, null, null);
    }
    
  }

}
