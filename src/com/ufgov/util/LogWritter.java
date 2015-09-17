package com.ufgov.util;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

/**
 * added by mengw 20100730 ��־��¼�齨��Ĭ����c�̵�system.log  ��
 * */

public class LogWritter {
  public static Logger log = null;
  static {
    if (log == null) {
      log = Logger.getLogger("LogWritter.class");
      SimpleLayout layout = new SimpleLayout();
      FileAppender appender = null;
      try {
        appender = new FileAppender(layout, "c:/system.log", false);
      } catch (Exception e) {
      }
      log.addAppender(appender);

    }
  }

  public static void println(String str) {
    System.out.println(str);
    log.info(str);
  }

  public static void println(String str, int code) {

    System.out.println("�߳�" + code + "��־��" + str);
    log.info("�߳�" + code + "��־��" + str);
  }

}
