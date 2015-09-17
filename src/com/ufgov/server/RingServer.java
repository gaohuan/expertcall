package com.ufgov.server;

import java.util.Random;

import org.xvolks.jnative.JNative;
import org.xvolks.jnative.Type;

import com.ufgov.util.ApplicationContext;
import com.ufgov.util.StackUtil;

public class RingServer extends Thread {
  /**
   * ����ɨ���߳�
   */
  private int randomNum = 0;

  public void run() {

    randomNum = new Random().nextInt(1000);

    while (true) {
      try {
        //LogWritter.println("*********************��������绰��ʼ*********************", randomNum);
        scan();
        //LogWritter.println("*********************��������绰����*********************", randomNum);
        Thread.sleep(3000);
      } catch (Exception e) {
        //LogWritter.println("*********************��������绰����*********************", randomNum);
        try {
          Thread.sleep(3000);
        } catch (InterruptedException e1) {
        }
        e.printStackTrace();
        //LogWritter.println(e.getMessage(), randomNum);
      }
    }
  }

  public void scan() throws Exception {
    int num = Integer.parseInt(ApplicationContext.singleton().getValueAsString("ringThread"));
    JNative n = null;
    String ports[] = ApplicationContext.singleton().getValueAsString("port").split(",");
    int port = -1;

    for (int ch = 0; ch < num; ch++) {
      //�鿴�Ƿ�������
      //LogWritter.println("*********************��������ģ��" + ch + "*********************", randomNum);
      n = new JNative("SHP_A3", "SsmGetChState");
      n.setRetVal(Type.INT);
      n.setParameter(0, Type.INT, ch + "");
      n.invoke();

      if (n.getRetVal().equals("2")) {//������
        //�ж��Ƿ��б���߳��Ѿ����������ģ��
        if (StackUtil.stackRing.contains(ch))
          continue;
        StackUtil.pushRing(ch);

        for (int i = 0; i < ports.length; i++) {
          n = new JNative("SHP_A3", "SsmGetChState");
          n.setRetVal(Type.INT);
          n.setParameter(0, Type.INT, ports[i]);
          n.invoke();
          if (n.getRetVal().equals("0"))
            port = Integer.parseInt(ports[i]);
        }
        //LogWritter.println("*********************����ģ��" + ch + "�����壬ѡ�������ϯ" + port + "*********************",
        // randomNum);
        //���޿�����ϯʱ֪ͨ�Է���æ�Ժ��ٲ� modified by mengw 20100906
        if (port == -1) {
          n = new JNative("SHP_A3", "SsmPickup");
          n.setRetVal(Type.INT);
          n.setParameter(0, Type.INT, ch + "");
          n.invoke();

          n = new JNative("PlayText", "LoadHZK");
          n.setRetVal(Type.INT);
          n.invoke();

          n = new JNative("PlayText", "PlayText");
          n.setRetVal(Type.INT);
          n.setParameter(0, Type.INT, ch + "");
          n.setParameter(1, Type.STRING, "��������ĵ绰��æ�����Ժ��ٲ�");
          n.invoke();
          Thread.sleep(10000);

          n = new JNative("SHP_A3", "SsmHangup");
          n.setRetVal(Type.INT);
          n.setParameter(0, Type.INT, ch + "");
          n.invoke();
          //�߳̽����ͷ���Դ modified by mengw 20100906
          if (StackUtil.stackRing.contains(ch))
            StackUtil.removeRing(ch);
          return;
        }
        //LogWritter.println("*********************����ģ��" + ch + "�����壬������ϯ*********************", randomNum);
        n = new JNative("SHP_A3", "SsmStartRing");
        n.setRetVal(Type.INT);
        n.setParameter(0, Type.INT, port + "");
        n.invoke();
        //���������������ϯͨ��״̬
        int sign = 0;
        while (true) {
          //����ͨ���һ����˳�
          n = new JNative("SHP_A3", "SsmGetChState");
          n.setRetVal(Type.INT);
          n.setParameter(0, Type.INT, ch + "");
          n.invoke();
          if (n.getRetVal().equals("0") || n.getRetVal().equals("7")) {
            n = new JNative("SHP_A3", "SsmStopRing");
            n.setRetVal(Type.INT);
            n.setParameter(0, Type.INT, port + "");
            n.invoke();

            n = new JNative("SHP_A3", "SsmHangup");
            n.setRetVal(Type.INT);
            n.setParameter(0, Type.INT, ch + "");
            n.invoke();
            // LogWritter.println("*********************����ͨ���ҶϽ���*********************", randomNum);
            break;

          }
          //��ϯ���˽���30�볬ʱ
          if (sign >= 30000) {
            n = new JNative("SHP_A3", "SsmStopRing");
            n.setRetVal(Type.INT);
            n.setParameter(0, Type.INT, port + "");
            n.invoke();

            n = new JNative("SHP_A3", "SsmHangup");
            n.setRetVal(Type.INT);
            n.setParameter(0, Type.INT, ch + "");
            n.invoke();
            //LogWritter.println("*********************������ϯ��ʱ����*********************", randomNum);
            break;
          }
          n = new JNative("SHP_A3", "SsmGetChState");
          n.setRetVal(Type.INT);
          n.setParameter(0, Type.INT, port + "");
          n.invoke();
          if (n.getRetVal().equals("1")) {
            //��ϯͨ����ժ��
            n = new JNative("SHP_A3", "SsmGetChState");
            n.setRetVal(Type.INT);
            n.setParameter(0, Type.INT, ch + "");
            n.invoke();
            if (n.getRetVal().equals("2")) {
              //����ͨ��ժ��
              n = new JNative("SHP_A3", "SsmPickup");
              n.setRetVal(Type.INT);
              n.setParameter(0, Type.INT, ch + "");
              n.invoke();
              //����ͨ������ϯͨ������
              // LogWritter.println("*********************����ͨ������ϯͨ������*********************", randomNum);
              n = new JNative("SHP_A3", "SsmTalkWith");
              n.setRetVal(Type.INT);
              n.setParameter(0, Type.INT, ch + "");
              n.setParameter(1, Type.INT, port + "");
              n.invoke();

              while (true) {
                //��ȡ���߹һ�״̬
                n = new JNative("SHP_A3", "SsmGetChState");
                n.setRetVal(Type.INT);
                n.setParameter(0, Type.INT, ch + "");
                n.invoke();
                if (n.getRetVal().equals("7") || n.getRetVal().equals("0")) {
                  n = new JNative("SHP_A3", "SsmHangup");
                  n.setRetVal(Type.INT);
                  n.setParameter(0, Type.INT, port + "");
                  n.invoke();
                  //LogWritter.println("*********************����ͨ���һ�ͨ������*********************", randomNum);
                  break;
                }
                //��ȡ��ϯ�һ�״̬
                n = new JNative("SHP_A3", "SsmGetChState");
                n.setRetVal(Type.INT);
                n.setParameter(0, Type.INT, port + "");
                n.invoke();
                if (n.getRetVal().equals("0") || n.getRetVal().equals("7")) {
                  n = new JNative("SHP_A3", "SsmHangup");
                  n.setRetVal(Type.INT);
                  n.setParameter(0, Type.INT, ch + "");
                  n.invoke();
                  //LogWritter.println("*********************��ϯͨ���һ�ͨ������*********************", randomNum);
                  break;
                }
                Thread.sleep(10);
              }
              n = new JNative("SHP_A3", "SsmStopRing");
              n.setRetVal(Type.INT);
              n.setParameter(0, Type.INT, port + "");
              n.invoke();
              break;
            } else {
              n = new JNative("SHP_A3", "SsmStopRing");
              n.setRetVal(Type.INT);
              n.setParameter(0, Type.INT, port + "");
              n.invoke();
              break;
            }

          }
          sign += 10;
          Thread.sleep(10);
        }
        //�߳̽����ͷ���Դmodified by mengw 20100906
        if (StackUtil.stackRing.contains(ch))
          StackUtil.removeRing(ch);
      }
    }
  }
}
