/**
 * 
 */
package com.ufgov.second;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * ר�Ҷ����� 1���洢�Ⱥ򲦴�绰��ר�� 2.ָ��ר�����ȡ���Ĺ�ϵ 3.ͨ��ͬ���ķ�ʽ���������������ר�� 4.ͨ��ͬ���ķ�ʽ���Ӷ������ȡר��
 * @author Administrator
 */
public class ExpertQueue {
  private static Logger logger = Logger.getLogger(ExpertQueue.class);

  /**
   * ��ȡ���б� �����ȡ�����
   */
//  private List<String> emBillLst = new ArrayList<String>();

  /**
   * �Ⱥ����
   */
  private List<ExpertCallInfo> waitingExpertLst = new ArrayList<ExpertCallInfo>();

  /**
   * ͨ������
   */
  private List<ExpertCallInfo> callingExpertLst = new ArrayList<ExpertCallInfo>();

  /**
   * �ӵȺ�����л�ȡһ��ר�ң����Ѹ�ר��ת��ͨ��������
   * @param billCode ��ȡ����ţ����ָ�������ֵ�����ȡ������ݵĵȴ�ר�ң����û��ָ��(Ϊnull),����û���ҹ���Ӧ��ר�ң����ȡ��ͷ��һ���Ⱥ��ר��
   * @return
   */
  public synchronized ExpertCallInfo pop(String billCode) {
    while(waitingExpertLst.size()==0){
      try {
        wait();
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
//        e.printStackTrace();
        logger.error("��ȡ�ȴ���ר���쳣��"+e.getMessage(),e);
      }
    }
    //��ȡһ���ȴ���ר��
    if (billCode == null) {
      ExpertCallInfo expert = waitingExpertLst.get(0);
      callingExpertLst.add(expert);
      waitingExpertLst.remove(0);
      return expert;
    } else {
      for (int i = 0; i < waitingExpertLst.size(); i++) {
        ExpertCallInfo expert = waitingExpertLst.get(i);
        if (expert.getBillCode().equals(billCode)) {
          callingExpertLst.add(expert);
          waitingExpertLst.remove(i);
          return expert;
        }
      }
      ExpertCallInfo expert = waitingExpertLst.get(0);
      callingExpertLst.add(expert);
      waitingExpertLst.remove(0);
      return expert;
    }
  }
  /**
   * ���Ⱥ���������һ��ר��
   * @param expert
   */
  public synchronized void push(ExpertCallInfo expert) {
    if(expert==null){
      return;
    }
    if(waitingExpertLst.contains(expert)){
      return;
    }
    waitingExpertLst.add(expert);
    notifyAll();
  }
  /**
   * ���ͨ����ר�ң�������ͨ�ˡ�δͨ�ġ��պŵȣ�,����ר�Ҵ�ͨ��������ɾ��
   * @param expert
   */
  public synchronized void completeCalling(ExpertCallInfo expert) {
    if(expert==null){
      return;
    }
    for(int i=0;i<callingExpertLst.size();i++){
      ExpertCallInfo e=callingExpertLst.get(i);
      if(e.getObjid().equals(expert.getObjid())){
        callingExpertLst.remove(i);
      }
    }
  }
  
  /**
   * �Ƿ����ĳ��ȡ����ר�Ҵ����ڵȺ���л���ͨ��������
   * @param billCode
   * @return
   */
  private synchronized boolean existsBill(String billCode) {
    if(billCode==null){
      return false;
    }
    for (int i = 0; i < waitingExpertLst.size(); i++) {
      ExpertCallInfo expert = waitingExpertLst.get(i);
      if(billCode.equals(expert.getBillCode())){
        return true;
      }
    }
    for(int i=0;i<callingExpertLst.size();i++){
      ExpertCallInfo e=callingExpertLst.get(i);
      if(billCode.equals(e.getBillCode())){
        return true;
      }
    }
    return false;
  }
  
  public synchronized void removeWaitingExperts(String billCode) {
    for (int i = waitingExpertLst.size()-1; i >=0; i--) {
      ExpertCallInfo expert = waitingExpertLst.get(i);
      if(expert.getBillCode().equals(billCode)){
        waitingExpertLst.remove(i);
      }
    }
  }

}
