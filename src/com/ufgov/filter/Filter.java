package com.ufgov.filter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ��ȡר�ҹ����� added by mengw 20100813
 * */
public class Filter {

  public boolean dofilter(String emExpertCode, String billCode, String emCatalogueCode, String emYear,
    Connection conn) {
    //Ĭ�Ϸ���true����������������ų���Χ���򷵻�false
    boolean sign = false;
    String filterPath;
    PreparedStatement pst;
    ResultSet rs;
    try {
      pst = conn.prepareStatement("select * from EM_EXPERT_FILTER_PATH");
      rs = pst.executeQuery();
      while (rs.next()) {
        filterPath = rs.getString("filter_path");

        IFilter filter = (IFilter) Class.forName(filterPath).newInstance();
        if (filter.dofilter(emExpertCode, billCode, emCatalogueCode, emYear, conn)) {
          sign = true;
          break;
        }
      }
      rs.close();
      pst.close();
    } catch (Exception e) {
      //���˲����쳣�����й���
      return false;
    } finally {
    }
    return sign;
  }

}
