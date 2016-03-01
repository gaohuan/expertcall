package com.ufgov.server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.xvolks.jnative.exceptions.NativeException;
import org.xvolks.jnative.pointers.Pointer;

import com.ufgov.ssm.SSMFactory;
import com.ufgov.tts.TTSFactory;
import com.ufgov.util.ApplicationContext;

public class ServiceContext {

  private static Logger logger = Logger.getLogger(ServiceContext.class);

  public static List<CallServer> callServerList = new ArrayList<CallServer>();
  
  public static void main(String[] args) {
	 
    ServiceContext serviceContext = new ServiceContext();
    serviceContext.init();
    
    new BillServer().start();
    
    //�Ⲧ�绰������
	final int linesNum = Integer.parseInt(ApplicationContext.singleton().getValueAsString("callThread"));
	
	Thread th = new Thread() {
		public void run() {
			TTSFactory tsf = new TTSFactory();
			tsf.init();			
			try {
				for (int i = 0; i < linesNum; i++) {					
					CallServer call = new CallServer();
					call.setTsf(tsf);
					call.setthreadNum(i);
					call.start();
          callServerList.add(call);
					sleep(30000);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				logger.error("�������н��̴���!\n" + e.getMessage(), e);				
			}finally{
			}  
		}
	};
	th.start();
  }

  public void init() {
    try {
      initSsmCard();
    } catch (Exception e) {
      logger.info("��������ʼ��ʧ�ܡ�" + e.getMessage());
      System.exit(0);
    }
//    boolean flag = CallServer.init();
//    if (!flag) {
//      logger.error("��������ϵͳ��ʼ��ʧ�ܡ�");
//      System.exit(0);
//    }
    //������й����в����������ļ�
    clearWavs(new File(CallServer.VOICE_DIR));
    logger.info("ϵͳ��ʼ����ϡ�");
  }

  private boolean clearWavs(File dir) {
		if (dir==null || !dir.exists()) {
			return false;			
		}
		if (dir.isDirectory()) {
            String[] children = dir.list();
            //�ݹ�ɾ��Ŀ¼�е���Ŀ¼��
            for (int i=0; i<children.length; i++) {
                boolean success = clearWavs(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // Ŀ¼��ʱΪ�գ�����ɾ��
        return dir.delete();
}

  private void initSsmCard() throws NativeException, IllegalAccessException {
    System.loadLibrary("SHP_A3");
    String shConfig = ApplicationContext.singleton().getValueAsString("shConfig");
    String shIndex = ApplicationContext.singleton().getValueAsString("shIndex");
    int initState = SSMFactory.ssmStartCti(shConfig, shIndex);
    if (initState == 0) {
      logger.info("��������ʼ���忨�ɹ���");
    } else if (initState == -2) {
      logger.info("���Ѿ�������һ��������ʵ������رա�");
      System.exit(0);
    } else if (initState == -1) {
      logger.info("��������ʼ��ʧ�ܡ�" + getLastErrMsg());
      System.exit(0);
    }
  }

  public String getLastErrMsg() throws NativeException, IllegalAccessException {
    Pointer pointer = SSMFactory.creatPointer(40);
    SSMFactory.ssmGetLastErrMsg(pointer);
    String errMeg = pointer.getAsString();
    pointer.dispose();
    return errMeg;
  }
}
