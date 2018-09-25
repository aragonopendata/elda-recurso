package com.localidata.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class ConfigurationFile
{
  Properties propiedades = null;
  
  public ConfigurationFile(Properties _propiedades)
  {
    this.propiedades = _propiedades;
  }
  
  public ConfigurationFile(String rutaFichero)
    throws FileNotFoundException, IOException
  {
    this.propiedades = new Properties();
    this.propiedades.load(new FileInputStream(rutaFichero));
  }
  
  public String getProperty(String propiedad)
  {
    if ((this.propiedades != null) && (this.propiedades.get(propiedad) != null)) {
      return (String)this.propiedades.get(propiedad);
    }
    return null;
  }
}
