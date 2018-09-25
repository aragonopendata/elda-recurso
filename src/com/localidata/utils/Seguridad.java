package com.localidata.utils;

import org.apache.log4j.Logger;
import threescale.v3.api.AuthorizeResponse;
import threescale.v3.api.ParameterMap;
import threescale.v3.api.ServerError;
import threescale.v3.api.ServiceApi;
import threescale.v3.api.impl.ServiceApiDriver;

public class Seguridad
{
  private static Logger log = Logger.getLogger(Seguridad.class);
  private static final String errorReferrer = "referrer";
  private static final String errorLimits = "limits are exceeded";
  
  public static int autenticacion(String apiLocalidata, String userKey, String referrer, String peso, String metodo)
    throws ServerError
  {
    int autenticacion_ok = -1;
    
    ServiceApi serviceApi = new ServiceApiDriver(apiLocalidata);
    
    ParameterMap params = new ParameterMap();
    
    params.add("user_key", userKey);
    
    log.info("Referrer: " + referrer);
    params.add("referrer", referrer);
    
    ParameterMap usage = new ParameterMap();
    if (metodo != null)
    {
      usage.add("hits", "0");
      usage.add(metodo, peso);
    }
    else
    {
      usage.add("hits", peso);
    }
    params.add("usage", usage);
    
    AuthorizeResponse response = null;
    
    log.info("Preautenticación contra 3scale");
    response = serviceApi.authrep(params);
    log.info("Fin autenticación");
    if (response.success())
    {
      log.info("Autenticación Correcta: " + peso + " hits");
      autenticacion_ok = 0;
    }
    else
    {
      log.info("Autenticación Incorrecta");
      log.info("Error: " + response.getErrorCode());
      log.info("Reason: " + response.getReason());
      if (response.getReason().indexOf("referrer") >= 0) {
        autenticacion_ok = -2;
      } else if (response.getReason().indexOf("limits are exceeded") >= 0) {
        autenticacion_ok = -3;
      }
    }
    return autenticacion_ok;
  }
  
  public static void main(String[] args) {}
}
