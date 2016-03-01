/**
 * 
 */
package com.ufgov.second;

/**
 * ר�ҳ�ȡ������
 * @author Administrator
 *
 */
public interface IExpertCallConstant {
  /**
   * ��ȡ��״̬����ȡ��
   */
  public static final String BILL_STATUS_SELECTING="0";
  /**
   * ��ȡ��״̬����ȡ�ɹ�
   */
  public static final String BILL_STATUS_SUCCESS="4";
  /**
   * ��ȡ��״̬��ר�Ҳ���
   */
  public static final String BILL_STATUS_NO_ENOUGH_EXPERTS="8";
  /**
   * ��ȡ��״̬:��ͣ
   */
  public static final String BILL_STATUS_PAUSED="6";
  
  // �����Ҫ��ȡ�ĳ�ȡ��
  public static final String GET_BILL_SERVER_LIST_BY_STATUS = "SELECT * FROM EM_BILL_SERVER_LIST WHERE EM_STATUS = ?";
  
  public static final String GET_SELECTING_AND_PAUSED_BILL = "SELECT * FROM EM_BILL_SERVER_LIST WHERE EM_STATUS IN('0','6')";
  
//��ȡ��ǰ��ȡ������Ҫ��ȡ��ר������
  public static final String GET_EXPERT_NUM_FOR_SELECTION = "SELECT (B.NUM - A.NUM) NUM FROM (SELECT NVL(COUNT(*), 0) NUM FROM ZC_EM_EXPERT_EVALUATION WHERE EM_BILL_CODE = ? AND EM_RESPONSE_STATUS = ?) A, (SELECT NVL(SUM(EC.EXPERT_NUM), 0) NUM FROM EM_EVALUATION_CONDITION EC WHERE EC.EM_BILL_CODE = ?) B";

  // ��ǰ���ݵ�ר����𡢳�ȡ������������Ϣ��������Ϣ
  public static final String GET_EVALUATION_CONDITION_LIST = "SELECT EC.EM_EXPERT_TYPE_CODE,EC.EXPERT_NUM,B.EM_CALL_INFO,B.EM_MSG_INFO FROM EM_EVALUATION_CONDITION EC, ZC_EM_EXPERT_PRO_BILL B WHERE EC.EM_BILL_CODE = B.EM_BILL_CODE AND B.EM_BILL_CODE = ?";


  // �Ѿ���ȡ��ר��,ֻ����ͬ��μӵ�ר��
  public static final String GET_SELECTED_EXPERT_NUM = "SELECT NVL(COUNT(EM_EXPERT_CODE), 0) NUM FROM ZC_EM_EXPERT_EVALUATION WHERE EM_BILL_CODE = ? AND EM_EXPERT_TYPE_CODE = ? AND EM_RESPONSE_STATUS='9'";

  // ��ȡ��ǰ�����϶�Ӧ����ר�ң��Ѿ�����绰�ġ����˵�ר�Ҳ���ѡ��Χ�� ר���б� em_type_code like 'xxxx%'
  // ֧��ѡȡ�����ȡ
  public static final String GET_EXPERT_LIST = "SELECT * FROM ( SELECT * FROM ZC_EM_B_EXPERT WHERE EM_EXPERT_CODE NOT IN (SELECT EM_EXPERT_CODE  FROM EM_EXPERT_BILL_FILTER WHERE EM_BILL_CODE = ?) "
    + "AND EM_UNIT_NAME NOT IN (SELECT UNIT_NAME FROM EM_EXPERT_BILL_FILTER_UNIT WHERE EM_BILL_CODE =?) "
    + "AND EM_EXPERT_CODE NOT IN (SELECT EM_EXPERT_CODE  FROM ZC_EM_EXPERT_EVALUATION  WHERE EM_BILL_CODE = ?) "
    + "AND EM_EXPERT_CODE NOT IN (SELECT L.EM_EXPERT_CODE FROM EM_CALL_SERVER_LIST L  WHERE L.EM_BILL_CODE=? AND L.EM_EXPERT_TYPE_CODE=?) "
    + "AND EM_EXPERT_CODE IN (SELECT EM_EXPERT_CODE FROM ZC_Em_Expert_Type_Join WHERE em_type_code like ?||'%') AND EM_EXP_STATUS='enable' ORDER BY dbms_random.VALUE ) WHERE rownum < 100";

}
