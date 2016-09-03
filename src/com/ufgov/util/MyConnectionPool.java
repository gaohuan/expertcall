/**
 * 
 */
package com.ufgov.util;

import java.sql.Connection;     
import java.sql.DatabaseMetaData;     
import java.sql.Driver;     
import java.sql.DriverManager;     
import java.sql.ResultSet;     
import java.sql.SQLException;     
import java.sql.Statement;     
import java.util.Enumeration;     
import java.util.Vector; 

import org.apache.log4j.Logger;

/**
 * @author Administrator
 *
 */
public class MyConnectionPool {     

  private static final Logger logger = Logger.getLogger(MyConnectionPool.class);


  private static final String HOST_NAME = ApplicationContext.singleton().getValueAsString("hostname");

  private static final String PORT = ApplicationContext.singleton().getValueAsString("port");

  private static final String SID = ApplicationContext.singleton().getValueAsString("sid");

  private static final String USER_NAME = ApplicationContext.singleton().getValueAsString("username");

  private static final String PASSWORD = ApplicationContext.singleton().getValueAsString("password");

  private static final String URI = "jdbc:oracle:thin:@${HOST_NAME}:${PORT}:${SID}".replace("${HOST_NAME}", HOST_NAME).replace("${PORT}", PORT).replace("${SID}", SID);

  
//  private static String jdbcDriver = ""; // ���ݿ�����     
//  
//  private static String dbUrl = ""; // ���� URL     
//  
//  private static String dbUsername = ""; // ���ݿ��û���     
//  
//  private static String dbPassword = ""; // ���ݿ��û�����     
  
  private String testTable = "as_compo"; // ���������Ƿ���õĲ��Ա�����Ĭ��û�в��Ա�     
  
  private int initialConnections = 5; // ���ӳصĳ�ʼ��С     
  
  private int incrementalConnections = 5;// ���ӳ��Զ����ӵĴ�С     
  
  private int maxConnections = 15; // ���ӳ����Ĵ�С     
  
  private Vector connections = null; // ������ӳ������ݿ����ӵ����� , ��ʼʱΪ null   
  
  private static class InstanceHolder{
    private final static MyConnectionPool INSTANCE=new MyConnectionPool();
  }
  
  public static MyConnectionPool getInstance(){    
    return InstanceHolder.INSTANCE;
  }
  // ���д�ŵĶ���Ϊ PooledConnection ��     
  
  private MyConnectionPool() {            
  }     
       
   

  
  
  /**   
   * ����һ�����ݿ����ӳأ����ӳ��еĿ������ӵ������������Ա initialConnections �����õ�ֵ   
   */    
  
  public synchronized void createPool() throws Exception {     
  
      // ȷ�����ӳ�û�д���     
  
      // ������ӳؼ��������ˣ��������ӵ����� connections ����Ϊ��     
  
      if (connections != null) {     
  
          return; // ��������������򷵻�     
  
      }     
  
      // ʵ���� JDBC Driver ��ָ����������ʵ��     
  
      Driver driver = (Driver) (Class.forName("oracle.jdbc.driver.OracleDriver").newInstance());     
  
      DriverManager.registerDriver(driver); // ע�� JDBC ��������     
  
      // �����������ӵ����� , ��ʼʱ�� 0 ��Ԫ��     
  
      connections = new Vector();     
  
      // ���� initialConnections �����õ�ֵ���������ӡ�     
  
      createConnections(this.initialConnections);     
  
      logger.info(" ���ݿ����ӳش����ɹ��� ");     
  
  }     
  
  /**   
   *    
   * ������ numConnections ָ����Ŀ�����ݿ����� , ������Щ����   
   *    
   * ���� connections ������   
   *    
   *    
   *    
   * @param numConnections   
   *            Ҫ���������ݿ����ӵ���Ŀ   
   *    
   */    
  
  @SuppressWarnings("unchecked")     
  private void createConnections(int numConnections) throws SQLException {     
  
      // ѭ������ָ����Ŀ�����ݿ�����     
  
      for (int x = 0; x < numConnections; x++) {     
  
          // �Ƿ����ӳ��е����ݿ����ӵ����������ﵽ������ֵ�����Ա maxConnections     
  
          // ָ������� maxConnections Ϊ 0 ��������ʾ��������û�����ơ�     
  
          // ��������������ﵽ��󣬼��˳���     
  
          if (this.maxConnections > 0    
                  && this.connections.size() >= this.maxConnections) {     
  
              break;     
  
          }     
  
          // add a new PooledConnection object to connections vector     
  
          // ����һ�����ӵ����ӳ��У����� connections �У�     
  
          try {     
  
              connections.addElement(new PooledConnection(newConnection()));     
  
          } catch (SQLException e) {     
  
              logger.error(" �������ݿ�����ʧ�ܣ� " + e.getMessage());     
  
              throw new SQLException();     
  
          }     
  
          logger.info(" ���ݿ����Ӽ����� ......");     
  
      }     
  
  }     
  
  /**   
   *    
   * ����һ���µ����ݿ����Ӳ�������   
   *    
   *    
   *    
   * @return ����һ���´��������ݿ�����   
   *    
   */    
  
  private Connection newConnection() throws SQLException {     
  
      // ����һ�����ݿ�����     

    try{
      Driver driver = (Driver) (Class.forName("oracle.jdbc.driver.OracleDriver").newInstance());  
      DriverManager.registerDriver(driver); // ע�� JDBC ��������   
    }catch (ClassNotFoundException e) {
      // TODO: handle exception
      throw new SQLException("�޷���ȡ������",e);
    } catch (InstantiationException e) {
      // TODO Auto-generated catch block
      throw new SQLException("�޷���ȡ������",e);
    } catch (IllegalAccessException e) {
      // TODO Auto-generated catch block
      throw new SQLException("�޷���ȡ������",e);
    }
      Connection conn = DriverManager.getConnection(URI, USER_NAME, PASSWORD);     
  
      return conn;
      // ������ǵ�һ�δ������ݿ����ӣ���������ݿ⣬��ô����ݿ�����֧�ֵ�     
  
      // ���ͻ�������Ŀ     
  
      // connections.size()==0 ��ʾĿǰû�����Ӽ�������     
  /*
      if (connections.size() == 0) {     
  
          DatabaseMetaData metaData = conn.getMetaData();     
  
          int driverMaxConnections = metaData.getMaxConnections();     
  
          // ���ݿⷵ�ص� driverMaxConnections ��Ϊ 0 ����ʾ�����ݿ�û�����     
  
          // �������ƣ������ݿ������������Ʋ�֪��     
  
          // driverMaxConnections Ϊ���ص�һ����������ʾ�����ݿ�����ͻ����ӵ���Ŀ     
  
          // ������ӳ������õ�������������������ݿ������������Ŀ , �������ӳص����     
  
          // ������ĿΪ���ݿ�����������Ŀ     
  
          if (driverMaxConnections > 0    
                  && this.maxConnections > driverMaxConnections) {     
  
              this.maxConnections = driverMaxConnections;     
  
          }     
  
      }     
  
      return conn; // ���ش������µ����ݿ�����     
*/  
  }     
  
  /**   
   *    
   * ͨ������ getFreeConnection() ��������һ�����õ����ݿ����� ,   
   *    
   * �����ǰû�п��õ����ݿ����ӣ����Ҹ�������ݿ����Ӳ��ܴ����������ӳش�С�����ƣ����˺����ȴ�һ���ٳ��Ի�ȡ��   
   *    
   * @return ����һ�����õ����ݿ����Ӷ���   
   *    
   */    
  
  public synchronized Connection getConnection() throws SQLException {     
    
    //�����α�����������Ϊÿ�δ���������
    return newConnection();
    //ԭ������
    /*  // ȷ�����ӳؼ�������     
  
      if (connections == null) {     

          try {
            createPool();
          } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            
            throw new SQLException("�������ݿ����ӳ��쳣\n"+e.getMessage(),e);
            
          }
  
      }     
  
      Connection conn = getFreeConnection(); // ���һ�����õ����ݿ�����     
  
      // ���Ŀǰû�п���ʹ�õ����ӣ������е����Ӷ���ʹ����     
  
      while (conn == null) {     
  
          // ��һ������     
  
          wait(500);     
  
          conn = getFreeConnection(); // �������ԣ�ֱ����ÿ��õ����ӣ����     
  
          // getFreeConnection() ���ص�Ϊ null     
  
          // ���������һ�����Ӻ�Ҳ���ɻ�ÿ�������     
  
      }     
  
      return conn;// ���ػ�õĿ��õ�����     
*/  
  }     
  
  /**   
   *    
   * �����������ӳ����� connections �з���һ�����õĵ����ݿ����ӣ����   
   *    
   * ��ǰû�п��õ����ݿ����ӣ������������ incrementalConnections ����   
   *    
   * ��ֵ�����������ݿ����ӣ����������ӳ��С�   
   *    
   * ������������е������Զ���ʹ���У��򷵻� null   
   *    
   * @return ����һ�����õ����ݿ�����   
   *    
   */    
  
  private Connection getFreeConnection() throws SQLException {     
  
      // �����ӳ��л��һ�����õ����ݿ�����     
  
      Connection conn = findFreeConnection();     
  
      if (conn == null) {     
  
          // ���Ŀǰ���ӳ���û�п��õ�����     
  
          // ����һЩ����     
  
          createConnections(incrementalConnections);     
  
          // ���´ӳ��в����Ƿ��п�������     
  
          conn = findFreeConnection();     
  
          if (conn == null) {     
  
              // ����������Ӻ��Ի�ò������õ����ӣ��򷵻� null     
  
              return null;     
  
          }     
  
      }     
  
      return conn;     
  
  }     
  
  /**   
   *    
   * �������ӳ������е����ӣ�����һ�����õ����ݿ����ӣ�   
   *    
   * ���û�п��õ����ӣ����� null   
   *    
   *    
   *    
   * @return ����һ�����õ����ݿ�����   
   *    
   */    
  
  private Connection findFreeConnection() throws SQLException {     
  
      Connection conn = null;     
  
      PooledConnection pConn = null;     
  
      // ������ӳ����������еĶ���     
  
      Enumeration enumerate = connections.elements();     
  
      // �������еĶ��󣬿��Ƿ��п��õ�����     
  
      while (enumerate.hasMoreElements()) {     
  
          pConn = (PooledConnection) enumerate.nextElement();     
  
          if (!pConn.isBusy()) {     
  
              // ����˶���æ�������������ݿ����Ӳ�������Ϊæ     
  
              conn = pConn.getConnection();     
  
              pConn.setBusy(true);     
  
              // ���Դ������Ƿ����     
  
              if (!testConnection(conn)) {     
  
                  // ��������Ӳ��������ˣ��򴴽�һ���µ����ӣ�     
  
                  // ���滻�˲����õ����Ӷ����������ʧ�ܣ����� null     
  
                  try {     
  
                      conn = newConnection();     
  
                  } catch (SQLException e) {     
  
                      logger.error(" �������ݿ�����ʧ�ܣ� " + e.getMessage());     
  
                      return null;     
  
                  }     
  
                  pConn.setConnection(conn);     
  
              }     
  
              break; // �����ҵ�һ�����õ����ӣ��˳�     
  
          }     
  
      }     
  
      return conn;// �����ҵ����Ŀ�������     
  
  }     
  
  /**   
   * ����һ�������Ƿ���ã���������ã��ص��������� false ������÷��� true   
   *    
   * @param conn   
   *            ��Ҫ���Ե����ݿ�����   
   * @return ���� true ��ʾ�����ӿ��ã� false ��ʾ������   
   */    
  
  private boolean testConnection(Connection conn) {     
  
      try {     
  
          // �жϲ��Ա��Ƿ����     
  
          if (testTable.equals("")) {     
  
              // ������Ա�Ϊ�գ�����ʹ�ô����ӵ� setAutoCommit() ����     
  
              // ���ж����ӷ���ã��˷���ֻ�ڲ������ݿ���ã���������� ,     
  
              // �׳��쳣����ע�⣺ʹ�ò��Ա�ķ������ɿ�     
  
              conn.setAutoCommit(true);     
  
          } else {// �в��Ա��ʱ��ʹ�ò��Ա����     
  
              // check if this connection is valid     
  
              Statement stmt = conn.createStatement();     
  
              ResultSet rs = stmt.executeQuery("select count(*) from "  + testTable);     
  
              rs.next();     
  
              logger.debug(testTable + "����ļ�¼��Ϊ��" + rs.getInt(1));     
  
          }     
  
      } catch (SQLException e) {     
  
          // �����׳��쳣�������Ӽ������ã��ر����������� false;     
          e.printStackTrace();     
               
          closeConnection(conn);     
  
          return false;     
  
      }     
  
      // ���ӿ��ã����� true     
  
      return true;     
  
  }     
  
  /**   
   * �˺�������һ�����ݿ����ӵ����ӳ��У����Ѵ�������Ϊ���С�   
   *    
   * ����ʹ�����ӳػ�õ����ݿ����Ӿ�Ӧ�ڲ�ʹ�ô�����ʱ��������   
   *    
   * @param �践�ص����ӳ��е����Ӷ���   
   */    
  
  public void returnConnection(Connection conn) {     
  
      if(conn==null)return;
      try {
        conn.close();
      } catch (SQLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        logger.error("�ر����ݿ������쳣\n"+e.getMessage(),e);     
      }
      
      //ԭ���Ĵ��벻���ˣ������α곬���������ֱ�ӹر�����
     /* 
      // ȷ�����ӳش��ڣ��������û�д����������ڣ���ֱ�ӷ���     
      if (connections == null) {     
  
          logger.error(" ���ӳز����ڣ��޷����ش����ӵ����ӳ��� !");     
          try {
            conn.close();
          } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            logger.error("�ر����ݿ������쳣\n"+e.getMessage(),e);     
          }
          return;     
  
      }     
  
      PooledConnection pConn = null;     
  
      Enumeration enumerate = connections.elements();     
  
      // �������ӳ��е��������ӣ��ҵ����Ҫ���ص����Ӷ���     
  
      while (enumerate.hasMoreElements()) {     
  
          pConn = (PooledConnection) enumerate.nextElement();     
  
          // ���ҵ����ӳ��е�Ҫ���ص����Ӷ���     
  
          if (conn == pConn.getConnection()) {     
  
              // �ҵ��� , ���ô�����Ϊ����״̬     
  
              pConn.setBusy(false);     
  
              break;     
  
          }     
  
      }   */  
  
  }     
  
  /**   
   * ˢ�����ӳ������е����Ӷ���   
   */    
  
  public synchronized void refreshConnections() throws SQLException {     
  
      // ȷ�����ӳؼ����´���     
  
      if (connections == null) {     
  
          logger.error(" ���ӳز����ڣ��޷�ˢ�� !");     
  
          return;     
  
      }     
  
      PooledConnection pConn = null;     
  
      Enumeration enumerate = connections.elements();     
  
      while (enumerate.hasMoreElements()) {     
  
          // ���һ�����Ӷ���     
  
          pConn = (PooledConnection) enumerate.nextElement();     
  
          // �������æ��� 5 �� ,5 ���ֱ��ˢ��     
  
          if (pConn.isBusy()) {     
  
              wait(5000); // �� 5 ��     
  
          }     
  
          // �رմ����ӣ���һ���µ����Ӵ�������     
  
          closeConnection(pConn.getConnection());     
  
          pConn.setConnection(newConnection());     
  
          pConn.setBusy(false);     
  
      }     
  
  }     
  
  /**   
   * �ر����ӳ������е����ӣ���������ӳء�   
   */    
  
  public synchronized void closeConnectionPool() throws SQLException {     
  
      // ȷ�����ӳش��ڣ���������ڣ�����     
  
      if (connections == null) {     
  
          logger.error(" ���ӳز����ڣ��޷��ر� !");     
  
          return;     
  
      }     
  
      PooledConnection pConn = null;     
  
      Enumeration enumerate = connections.elements();     
  
      while (enumerate.hasMoreElements()) {     
  
          pConn = (PooledConnection) enumerate.nextElement();     
  
          // ���æ���� 5 ��     
  
          if (pConn.isBusy()) {     
  
              wait(5000); // �� 5 ��     
  
          }     
  
          // 5 ���ֱ�ӹر���     
  
          closeConnection(pConn.getConnection());     
  
          // �����ӳ�������ɾ����     
  
          connections.removeElement(pConn);     
  
      }     
  
      // �����ӳ�Ϊ��     
  
      connections = null;     
  
  }     
  
  /**   
   * �ر�һ�����ݿ�����   
   *    
   * @param ��Ҫ�رյ����ݿ�����   
   */    
  
  private void closeConnection(Connection conn) {     
      if(conn==null)return;
      try {     
  
          conn.close();     
  
      } catch (SQLException e) {     
  
          logger.error(" �ر����ݿ����ӳ��� " + e.getMessage(),e);     
  
      }     
  
  }     
  
  /**   
   * ʹ����ȴ������ĺ�����   
   *    
   * @param �����ĺ�����   
   */    
  
  private void wait(int mSeconds) {     
  
      try {     
  
          Thread.sleep(mSeconds);     
  
      } catch (InterruptedException e) {     
  
      }     
  
  }     
  
  /**   
   * �������ӳصĳ�ʼ��С   
   *    
   * @return ��ʼ���ӳ��пɻ�õ���������   
   */    
  
  public int getInitialConnections() {     
  
      return this.initialConnections;     
  
  }     
  
  /**   
   * �������ӳصĳ�ʼ��С   
   *    
   * @param �������ó�ʼ���ӳ������ӵ�����   
   */    
  
  public void setInitialConnections(int initialConnections) {     
  
      this.initialConnections = initialConnections;     
  
  }     
  
  /**   
   * �������ӳ��Զ����ӵĴ�С ��   
   *    
   * @return ���ӳ��Զ����ӵĴ�С   
   */    
  
  public int getIncrementalConnections() {     
  
      return this.incrementalConnections;     
  
  }     
  
  /**   
   * �������ӳ��Զ����ӵĴ�С   
   *    
   * @param ���ӳ��Զ����ӵĴ�С   
   */    
  
  public void setIncrementalConnections(int incrementalConnections) {     
  
      this.incrementalConnections = incrementalConnections;     
  
  }     
  
  /**   
   * �������ӳ������Ŀ�����������   
   *    
   * @return ���ӳ������Ŀ�����������   
   */    
  
  public int getMaxConnections() {     
  
      return this.maxConnections;     
  
  }     
  
  /**   
   * �������ӳ��������õ���������   
   *    
   * @param �������ӳ��������õ���������ֵ   
   */    
  
  public void setMaxConnections(int maxConnections) {     
  
      this.maxConnections = maxConnections;     
  
  }     
  
  /**   
   * ��ȡ�������ݿ�������   
   *    
   * @return �������ݿ�������   
   */    
  
  public String getTestTable() {     
  
      return this.testTable;     
  
  }     
  
  /**   
   * ���ò��Ա������   
   *    
   * @param testTable   
   *            String ���Ա������   
   */    
  
  public void setTestTable(String testTable) {     
  
      this.testTable = testTable;     
  
  }     
  
  /**   
   * �ڲ�ʹ�õ����ڱ������ӳ������Ӷ������   
   *    
   * ��������������Ա��һ�������ݿ�����ӣ���һ����ָʾ�������Ƿ�   
   *    
   * ����ʹ�õı�־��   
   */    
  
  class PooledConnection {     
  
      Connection connection = null;// ���ݿ�����     
  
      boolean busy = false; // �������Ƿ�����ʹ�õı�־��Ĭ��û������ʹ��     
  
      // ���캯��������һ�� Connection ����һ�� PooledConnection ����     
  
      public PooledConnection(Connection connection) {     
  
          this.connection = connection;     
  
      }     
  
      // ���ش˶����е�����     
  
      public Connection getConnection() {     
  
          return connection;     
  
      }     
  
      // ���ô˶���ģ�����     
  
      public void setConnection(Connection connection) {     
  
          this.connection = connection;     
  
      }     
  
      // ��ö��������Ƿ�æ     
  
      public boolean isBusy() {     
  
          return busy;     
  
      }     
  
      // ���ö������������æ     
  
      public void setBusy(boolean busy) {     
  
          this.busy = busy;     
  
      }     
  
  }     
  
} 
