package com.ufgov.util;

import java.net.URL;

/**
 * ��ȡ�ļ���url���� added by mengw 20100803
 */

public class FileURL {
  public FileURL() {

  }

  /**
   * �õ��ļ���URL���� ;
   */
  public URL getFileURL(String fileName) {
    ClassLoader loader = this.getClass().getClassLoader();
    URL fileUrl = loader.getResource(fileName);
    return fileUrl;

  }

}