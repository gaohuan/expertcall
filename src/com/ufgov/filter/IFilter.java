package com.ufgov.filter;

import java.sql.Connection;

/**
 * ����ʵ�������added by mengw 20100813
 * */
public interface IFilter {

  public boolean dofilter(String emExpertCode, String billCode, String emCatalogueCode, String emYear,
    Connection conn) throws Exception;

}
