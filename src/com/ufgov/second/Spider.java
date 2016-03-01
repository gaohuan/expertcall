/**
 * 
 */
package com.ufgov.second;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.ufgov.server.BillServer;
import com.ufgov.util.DAOFactory;
import com.ufgov.util.EmCallException;

/**
 * ר��������
 * 1������Ƿ��г�ȡ����
 * 1.1����г�ȡ�����ж�ר�ҵȺ�������Ƿ��а����������ȡ��
 * 1.1.1 ���û�У� ���ר�ҳ�ȡ���
 * 1.1.1.1 ���ר���Ѿ����ˣ�����³�ȡ����Ϊ��ȡ���
 * 1.1.1.2 ���ר�Ҳ������ӿ��л�ȡ����������ר��
 * 1.1.1.2.1 �鵽��ר�ң����뵽ר�ҵȺ����
 * 1.1.1.2.2 û�г鵽ר�ң�˵��û�з���������ר�ң����³�ȡ����״̬Ϊ��ȡ���ɹ���ȱʧר��
 * 1.1.2 ����У�˵��������ݵ�ר���Ѿ���ȡ�����ˣ��ڵȺ��绰
 * 1��2����г�ȡ�������Ҳ�������������ר�ң�����³�ȡ��״̬����ʾʧ��
 * 2������Ƿ�����ͣ����
 * 2.1 �������ͣ�ĵ��ݣ����������ר�ҵȺ��������ר�ҵȺ���ȡ���Ⱥ��ר��
 *  * 
 * @author Administrator
 *
 */
public class Spider implements Runnable {
  private static Logger logger = Logger.getLogger(Spider.class);

  private ExpertQueue queue;
  
  public Spider(ExpertQueue queue){
    this.queue=queue;
  }
  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    while(true){
      //��ȡ��ȡ��
      List<Map<String, String>> billLst=scan();
      if(billLst==null || billLst.size()==0){
        logger.info("û�г�ȡ��");
      }else{
        try{
        for(int i=0;i<billLst.size();i++){
          Map<String, String> billMap=billLst.get(i);
          if(IExpertCallConstant.BILL_STATUS_SELECTING.equals(billMap.get("EM_STATUS"))){//��ȡ�еĵ���
            //��ȡ��Ӧ��ר��
            List<ExpertCallInfo> expertLst=getExperts(billMap.get("EM_BILL_CODE"));
            if(expertLst==null || expertLst.size()==0){
              //���³�ȡ��״̬
              updateBillStatus(billMap.get("EM_BILL_CODE"));
            }else{
              //��ӵ��Ⱥ��б���
              for (ExpertCallInfo expertCallInfo : expertLst) {
                queue.push(expertCallInfo);
              }
            }
          }else if(IExpertCallConstant.BILL_STATUS_PAUSED.equals(billMap.get("EM_STATUS"))){//��ͣ�ĵ���
            queue.removeWaitingExperts(billMap.get("EM_BILL_CODE"));
          }
        }
        }catch(EmCallException ex){
          
        }
      }
      try {
        Thread.sleep(10000);//���10���ټ���
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block  
        logger.error("������ȡ���쳣��"+e.getMessage(),e);
      }
    }
  }

  /**
   * ���³�ȡ����״̬
   * �������ͣ״̬,�򲻸���
   * ����ǳ�ȡ״̬�����ѯ���ݵ�ר�ҳ�ȡ����Ͳ��������������״̬����Ϊ ��ȡ��ɻ���ר�Ҳ���
   * @param billCode
   */
  private void updateBillStatus(String billCode) {}
  /**
   * ���ݵ��ݺŻ�ȡ��Ҫ��ר��
   * @param billCode
   * @return
   * @throws EmCallException 
   */
  private List<ExpertCallInfo> getExperts(String billCode) throws EmCallException {
    // EM_RESPONSE_STATUS,��Ӧ�����9-�μ�
    // 8-���μ�
    Object[] params = new Object[] { billCode, "9", billCode };// ��ȡ��ǰ��ȡ������Ҫ��ȡ��ר���������������Ѿ�ͬ������ר��
    DAOFactory df=new DAOFactory();
    Map<String, String> expertNumMap = df.queryToColumnMap(IExpertCallConstant.GET_EXPERT_NUM_FOR_SELECTION, params);
    // ��ȡ��ǰ������Ҫ��ȡר�ҵ�����
    int needExpertNum = Integer.parseInt(expertNumMap.get("NUM"));
    List failTypeLst = new ArrayList();
    List selectedExpertLst = new ArrayList<String>();
    if (needExpertNum > 0) {
      params = new Object[] { billCode };
      // ��ǰ���ݵ�ר����𡢳�ȡ������������Ϣ��������Ϣ
      List<Map<String, String>> ecList = df.queryToListMap(IExpertCallConstant.GET_EVALUATION_CONDITION_LIST, params);
      // ר���������������
      for (Map<String, String> mcMap : ecList) {
        String emExpertTypeCode = mcMap.get("EM_EXPERT_TYPE_CODE");
        int needExpertNumWithType = Integer.parseInt(mcMap.get("EXPERT_NUM") == null ? "0" : mcMap.get("EXPERT_NUM"));// ��Ӧר����������ר������

        String emCallInfo = mcMap.get("EM_CALL_INFO");
        String emMsgInfo = mcMap.get("EM_MSG_INFO");
        params = new Object[] { billCode, emExpertTypeCode };
        Map<String, String> senMap = df.queryToColumnMap(IExpertCallConstant.GET_SELECTED_EXPERT_NUM, params);//�Ѿ���ȡ��ר��,ֻ����ͬ��μӵ�ר��

        int selectedExpertNumWithType = Integer.parseInt(senMap.get("NUM") == null ? "0" : senMap.get("NUM"));// �������Ѿ�ѡ�񵽵�ר��
        logger.info("=============needExpertNumWithType="+needExpertNumWithType);
        logger.info("=============selectedExpertNumWithType="+selectedExpertNumWithType);
        if (needExpertNumWithType > selectedExpertNumWithType) {
          params = new Object[] { billCode, billCode, billCode, billCode, emExpertTypeCode, emExpertTypeCode };
          List<Map<String, String>> expertList = df.queryToListMap(IExpertCallConstant.GET_EXPERT_LIST, params);
          //
          if (expertList == null && expertList.size() == 0) {// û���ѵ�ר�ң�˵��ר�Ҷ���ȡ���ˣ���û���ҵ�ר��
            failTypeLst.add(emExpertTypeCode);
          } else {
            int j = 0;
            for (int i = 0; i < expertList.size(); i++) {
              String expertCode = expertList.get(i).get("EM_EXPERT_CODE");
              String emMobile = expertList.get(i).get("EM_MOBILE");

              if (selectedExpertLst.contains(expertCode)) {
                continue;
              }

              // params = new Object[] { emBillCode, expertCode,
              // emExpertTypeCode, "0" };
              // df.executeUpdate(conn,
              // INSERT_EM_EXPERT_EVALUATION,
              // params);//����ѡ�񵽵�ר�ң���֪ͨ״̬Ϊ0,�ȴ�����绰

             /* params = new Object[] { expertCode, emMobile, 0, emBillCode, emCallInfo, emMsgInfo, emExpertTypeCode };
              df.executeUpdate(INSERT_EM_CALL_SERVER_LIST, params);// ����ѡ�񵽵�ר�ҵ����б��У���iscall��0������δ����
              logger.info("============="+INSERT_EM_CALL_SERVER_LIST);
              StringBuffer sb=new StringBuffer("=============params=");
              sb.append(expertCode).append(",").append(emBillCode).append(",0,").append(emBillCode).append(",").append(emCallInfo).append(",").append(emMsgInfo).append(",").append(emExpertTypeCode);
              
              logger.info(sb.toString());   */           
              j++;
              logger.info("=============j="+j);
              if (j >= needExpertNumWithType - selectedExpertNumWithType) {
                // �����Ҫ��ȡ��ר������=(��Ҫ������-�Ѿ���ȡ����)
                break;
              }
            }
            if (j < needExpertNumWithType - selectedExpertNumWithType) {
              failTypeLst.add(emExpertTypeCode);
            }
          }
        }
      }
    } else {
      /*params = new Object[] { EM_BILL_PRO_STATUS_COMPLETE_SELECTION, emBillCode };
      df.executeUpdate(UPDATE_EM_EXPERT_PRO_BILL_STATUS, params);
      params = new Object[] { EM_BILL_SERVER_STATUS_COMPLETE_SELECTION, emBillCode };
      df.executeUpdate(UPDATE_BILL_SERVER_STATUS, params);*/
    }
    
    return null;
  }
  private List<Map<String, String>> scan() {
    Object[] params = new Object[] {};// ��ȡ�ȴ���ȡ����ͣ�еĳ�ȡ��
    List<Map<String, String>> scanList=null;
    try {
      DAOFactory df=new DAOFactory();
      scanList = df.queryToListMap(IExpertCallConstant.GET_SELECTING_AND_PAUSED_BILL, params);     
    } catch (EmCallException e) {
//      e.printStackTrace();
      logger.error("ɨ��ר�ҳ�ȡ���쳣��\n" + e.getMessage(), e);
    }
    return scanList;
  }

}
