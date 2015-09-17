package com.ufgov.tts;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.log4j.Logger;

import com.sinovoice.jTTS.jTTS_JavaFastSyn;

public class TTSFactory {
	private static Logger logger = Logger.getLogger(TTSFactory.class);
	
	jTTS_JavaFastSyn jTTS;
	
	public void init(){
		jTTS = new jTTS_JavaFastSyn();
	    
	    //����log
	    jTTS.jTTS_SetLog("./Test.log", "INFO", 1, 1);	    
	    
	}
	
  
  public  boolean ttsPlayToFile(String filePath, String text) {
	  
	boolean flag = true;	  
	
	if(text==null || text.trim().length()==0){
		logger.error("�ϳ��������ı�����Ϊ��");
		return false;
	}

    //���ò���
    int nRet = jTTS.jTTS_SetParam("FILE", "xiaokun", "PCM16K16B", "DEFAULT",
                                   "GB", "common", "SYNC",
                                   6, 6, 6, 
                                   "NULL", "NULL", "NULL",
                                   "NULL", "FLAT",
                                   0, 0, "REPEAT"
                                   );
    if (nRet == 0)
    {
        

        byte byText[] = text.getBytes();    

       //�ϳɵ��ļ�
       nRet = jTTS.jTTS_PlayToFile(byText, "TEXT", "GB",
    		                      filePath, "PCM8K8B", "DEFAULT",
                                  "SYNC", "XiaoKun", "common",
                                  -1, -1, -1,
                                  "PUNC_OFF", "DIGIT_AUTO_TELEGRAM", "ENG_LETTER",
                                  "TAG_JTTS", "FLAT",
                                  0, 70, "NULL"
                                  );
    }
    
    if(nRet != 0)
    {
    	StringBuffer sb=new StringBuffer();
    	sb.append("�����ϳ�ʧ��,������룺").append(nRet).append(",����ݽ�ͨ���������ֲ��кϳɺ���jTTS_PlayToFile()�ķ���ֵ����ԭ��");
    	logger.error(sb.toString());
    	return false;
    }else{
    	logger.info("�ϳ������ɹ�");
    	return true;
    }
    
  }
  
}