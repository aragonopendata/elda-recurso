package com.localidata.utils;

import java.util.ArrayList;
import org.apache.log4j.Logger;

public class CacheURLs
{
  private static Logger log = Logger.getLogger(CacheURLs.class);
  private ArrayList<String> direccion;
  private ArrayList<String> contenido;
  private int max = 50;
  
  public CacheURLs()
  {
    this.direccion = new ArrayList();
    this.contenido = new ArrayList();
  }
  
  public CacheURLs(ArrayList<String> direccion, ArrayList<String> contenido)
  {
    this.direccion = direccion;
    this.contenido = contenido;
  }
  
  public synchronized int size()
  {
    return this.direccion.size();
  }
  
  public synchronized boolean isEmpty()
  {
    return this.direccion.isEmpty();
  }
  
  public synchronized void add(String url, String response)
  {
    if (this.direccion.size() == this.max)
    {
      log.info("cache llena, eliminamos primer elemento");
      this.direccion.remove(0);
      this.contenido.remove(0);
    }
    this.direccion.add(url);
    this.contenido.add(response);
    log.info("Contenido añadido a cache (" + this.direccion.size() + " / " + this.max + "): " + url);
  }
  
  public synchronized void remove()
  {
    if (this.max >= 1)
    {
      log.info("cache vacia");
      this.direccion.remove(0);
      this.contenido.remove(0);
      this.max -= 1;
    }
  }
  
  public synchronized int indexOf(String url)
  {
    return this.direccion.indexOf(url);
  }
  
  public synchronized String getContent(int pos)
  {
    return (String)this.contenido.get(pos);
  }
  
  public int getMax()
  {
    return this.max;
  }
  
  public void setMax(int max)
  {
    log.info("La cache es de : " + max + " elementos");
    this.max = max;
  }
}
