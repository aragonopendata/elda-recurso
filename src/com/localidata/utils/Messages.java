package com.localidata.utils;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages
{
  private static final String BUNDLE_NAME = "com.localidata.utils.messages";
  private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("com.localidata.utils.messages");
  
  public static String getString(String key)
  {
    try
    {
      return RESOURCE_BUNDLE.getString(key);
    }
    catch (MissingResourceException e) {}
    return '!' + key + '!';
  }
}
