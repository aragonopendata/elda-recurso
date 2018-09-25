package com.localidata.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import net.sf.json.JSONObject;
import net.sf.json.xml.XMLSerializer;
import org.apache.log4j.Logger;

public class UtilidadesRecurso
{
  private static Logger log = Logger.getLogger(UtilidadesRecurso.class);
  public static final String cadenaVacia = "";
  private static final String directorioRecursosElda = "/lda-assets";
  
  public static String processUrlFromElda(String urlCadena, String urlElda, String urlRecurso, String urlEstaticos)
    throws IOException, MalformedURLException
  {
    log.info("Url a procesar: " + urlCadena);
    
    StringBuffer contenidoURL = new StringBuffer("");
    
    URL url = new URL(urlCadena);
    
    URLConnection connection = url.openConnection();
    connection.setUseCaches(true);
    connection.connect();
    
    BufferedReader in = null;
    
    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    if (in != null)
    {
      String contextoElda = urlElda.substring(urlElda.lastIndexOf("/"));
      String inputLine;
      while ((inputLine = in.readLine()) != null)
      {
        inputLine = inputLine.replaceAll(contextoElda + "/lda-assets", urlEstaticos + "/lda-assets");
        
        inputLine = inputLine.replaceAll(urlElda, urlRecurso + "/recurso");
        contenidoURL.append(inputLine + "\n");
      }
      in.close();
    }
    return contenidoURL.toString();
  }
  
  public static String processUrlFromStats(String urlCadena)
    throws IOException
  {
    log.info("Url a procesar: " + urlCadena);
    BufferedReader in = null;
    StringBuffer contenidoURL = new StringBuffer("");
    try
    {
      URL url = new URL(urlCadena);
      
      in = new BufferedReader(new InputStreamReader(url.openStream()));
      if (in != null)
      {
        String inputLine;
        while ((inputLine = in.readLine()) != null)
        {
          contenidoURL.append(inputLine + "\n");
        }
        in.close();
      }
    }
    catch (MalformedURLException e)
    {
      throw e;
    }
    catch (IOException e)
    {
      if (in != null) {
        in.close();
      }
      throw e;
    }
    URL url;
    String jsonChain = contenidoURL.toString();
    if (urlCadena.indexOf("services/getStatistics?") > 0)
    {
      int posicion = jsonChain.indexOf("values") - 1;
      
      String cadenaABorrar = jsonChain.substring(1, posicion);
      
      jsonChain = jsonChain.replace(cadenaABorrar, "");
      
      jsonChain = jsonChain.substring(0, jsonChain.length() - 2);
    }
    else if ((urlCadena.indexOf("services/getDimensions?") > 0) || (urlCadena.indexOf("services/getStatisticsValues?") > 0) || 
      (urlCadena.indexOf("services/getStatisticLabel?") > 0) || (urlCadena.indexOf("services/getDimensionLabel?") > 0) || 
      (urlCadena.indexOf("services/getXValueLabel?") > 0) || (urlCadena.indexOf("services/getMetricLabel?") > 0))
    {
      jsonChain = "{\"values\":" + jsonChain + "}";
    }
    return jsonChain;
  }
  
  public static String getFullURL(HttpServletRequest request)
  {
    StringBuffer requestURL = request.getRequestURL();
    String queryString = request.getQueryString();
    if (queryString == null) {
      return requestURL.toString();
    }
    return '?' + queryString;
  }
  
  public static String eliminaParametro(HttpServletRequest request, String parametroABorrar)
  {
    String parametros = "";
    if (request.getQueryString() == null) {
      return parametros;
    }
    Enumeration<String> paramNames = request.getParameterNames();
    while (paramNames.hasMoreElements())
    {
      String paramName = (String)paramNames.nextElement();
      if (!paramName.equals(parametroABorrar)) {
        parametros = parametros + paramName + "=" + request.getParameter(paramName) + "&";
      }
    }
    if (!parametros.equals("")) {
      parametros = parametros.substring(0, parametros.length() - 1);
    }
    return parametros;
  }
  
  public static String eliminaParametro(HttpServletRequest request, String[] parametrosABorrar)
  {
    String parametros = "";
    if (request.getQueryString() == null) {
      return parametros;
    }
    String[] params = request.getQueryString().split("&");
    
    ArrayList<String> listaParametros = new ArrayList();
    String[] arrayOfString1;
    int j = (arrayOfString1 = params).length;
    for (int i = 0; i < j; i++)
    {
      String p = arrayOfString1[i];
      listaParametros.add(p.substring(0, p.indexOf("=")));
    }
    j = (arrayOfString1 = parametrosABorrar).length;
    for (int i = 0; i < j; i++)
    {
      String eliminaParam = arrayOfString1[i];
      listaParametros.remove(eliminaParam);
    }
    if (listaParametros.size() > 0) {
      for (String param : listaParametros) {
        if (parametros.equals("")) {
          parametros = param + "=" + request.getParameter(param);
        } else {
          parametros = parametros + "&" + param + "=" + request.getParameter(param);
        }
      }
    }
    return parametros;
  }
  
  public static String asignaPropiedad(ConfigurationFile cf, String propiedad)
  {
    if (cf.getProperty(propiedad) == null)
    {
      log.error("Parámetro " + propiedad + " Sin Recuperar");
      return "";
    }
    log.info("Parámetro " + propiedad + " Recuperado");
    return cf.getProperty(propiedad);
  }
  
  public static String trataAlmohadilla(String cadena)
  {
    if (cadena.indexOf("#") > 0) {
      cadena = cadena.replaceAll("#", "%23");
    }
    return cadena;
  }
  
  public static String getCodigoPaginaError(String titulo, String mensajeSuperior, String mensajeInferior, String contexto)
  {
    String cadenaImagen = contexto + "/lda-assets" + Messages.getString("UtilidadesRecurso.PaginaErrorImagen");
    String cadenaCSS = contexto + "/lda-assets" + Messages.getString("UtilidadesRecurso.PaginaErrorCSS");
    
    String paginaError = "";
    paginaError = paginaError + "<!DOCTYPE html>";
    paginaError = paginaError + "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n";
    paginaError = paginaError + "\t<head>\n";
    paginaError = paginaError + "\t<title>Error</title>\n";
    paginaError = paginaError + "\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n";
    paginaError = paginaError + "\t<link href=\"" + cadenaCSS + "\" rel=\"stylesheet\" type=\"text/css\" />\n";
    paginaError = paginaError + "\t</head>\n";
    paginaError = paginaError + "<body>\n";
    paginaError = paginaError + "\t<div id=\"capa\">\n";
    paginaError = paginaError + "\t\t<table border=\"0\">\n";
    paginaError = paginaError + "\t\t\t<tr>\n";
    paginaError = paginaError + "\t\t\t\t<td width=\"400\" height=\"50\"><span id=\"localidata\">" + titulo + "</span></td>\n";
    paginaError = paginaError + "\t\t\t\t<td rowspan=\"3\"><img src=\"" + cadenaImagen + "\"></td>\n";
    paginaError = paginaError + "\t\t\t</tr>\n";
    paginaError = paginaError + "\t\t\t<tr>\n";
    paginaError = paginaError + "\t\t\t\t<td><span id=\"codError\"></span><span id=\"error\">" + mensajeSuperior + "</span></td>\n";
    paginaError = paginaError + "\t\t\t</tr>\n";
    paginaError = paginaError + "\t\t\t<tr>\n";
    paginaError = paginaError + "\t\t\t\t<td><span id=\"infoError\">" + mensajeInferior + "</span></td>\n";
    paginaError = paginaError + "\t\t\t</tr>\n";
    paginaError = paginaError + "\t\t</table>\n";
    paginaError = paginaError + "\t</div>\n";
    paginaError = paginaError + "</body>\n";
    paginaError = paginaError + "</html>\n";
    return paginaError;
  }
  
  public static String JSONtoXML(String contenidoJSON)
  {
    XMLSerializer serializer = new XMLSerializer();
    serializer.setTypeHintsEnabled(false);
    serializer.setRootName("ConversionJSON");
    
    JSONObject json = JSONObject.fromObject(contenidoJSON);
    
    String xml = serializer.write(json);
    
    return xml;
  }
  
  public static String transformXSL(File xmlFile, File xsltFile)
    throws TransformerException
  {
    byte[] xml = processFile(xmlFile).getBytes();
    byte[] xsl = processFile(xsltFile).getBytes();
    return transformXSL(xml, xsl);
  }
  
  public static String transformXSL(String xml, String xsl)
    throws TransformerException
  {
    return transformXSL(xml.getBytes(), xsl.getBytes());
  }
  
  public static String transformXSL(byte[] xml, byte[] xsl)
    throws TransformerException
  {
    Source srcXml = new StreamSource(new ByteArrayInputStream(xml));
    Source srcXsl = new StreamSource(new ByteArrayInputStream(xsl));
    StringWriter writer = new StringWriter();
    Result result = new StreamResult(writer);
    TransformerFactory tFactory = TransformerFactory.newInstance();
    Transformer transformer = tFactory.newTransformer(srcXsl);
    transformer.transform(srcXml, result);
    return writer.toString();
  }
  
  public static String processFile(File f)
  {
    StringBuilder sb = new StringBuilder(1000);
    try
    {
      Scanner sc = new Scanner(f);
      while (sc.hasNext()) {
        sb.append(sc.nextLine());
      }
    }
    catch (FileNotFoundException e)
    {
      e.printStackTrace();
    }
    return sb.toString();
  }
  
  public static Map<String, String> getQueryMap(String query)
    throws Exception
  {
    if ((query != null) && (!query.equals("null")))
    {
      String[] params = query.split("&");
      Map<String, String> map = new HashMap();
      String[] arrayOfString1;
      int j = (arrayOfString1 = params).length;
      for (int i = 0; i < j; i++)
      {
        String param = arrayOfString1[i];
        if (param.indexOf("=") < 0) {
          throw new Exception("Parámetros incorrectos en la url");
        }
        String name = param.split("=")[0];
        String value = param.split("=")[1];
        map.put(name, value);
      }
      return map;
    }
    log.info("No se han recibido parámetros");
    return null;
  }
  
  public static String procesaURL(String urlCadena)
    throws IOException, MalformedURLException
  {
    StringBuffer contenidoURL = new StringBuffer("");
    
    URL url = new URL(urlCadena);
    BufferedReader in = null;
    
    in = new BufferedReader(new InputStreamReader(url.openStream()));
    if (in != null)
    {
      String inputLine;
      while ((inputLine = in.readLine()) != null)
      {
        contenidoURL.append(inputLine + "\n");
      }
      in.close();
    }
    return contenidoURL.toString();
  }
  
  public static void main(String[] args)
  {
    String cadena = "{\"name\":\"json\",\"bool\":true,\"int\":1}";
    cadena = "{\"xURI\":\"http://datos.localidata.com/def/CityStats/dimension#refDivisionTerritorial\",\"xType\":\"http://datos.localidata.com/def/City#DivisionTerritorial\",\"xLabels\":{\"en\":\"Territorial Division (Section, Neighbourhood, District)\",\"es\":\"Divisi����n Territorial (Secci����n, Barrio, Distrito)\"},\"yURI\":\"http://datos.localidata.com/def/CityStats/stats#afiliadosRGTrabajadores\",\"yType\":\"http://www.w3.org/2001/XMLSchema#int\",\"yLabels\":{\"en\":\"Number of workers affiliated, working in this territorial division\",\"es\":\"N����mero de afiliados en R����gimen General trabajando en la divisi����n territorial\"},\"aggr\":\"\"},{\"xURI\":\"http://datos.localidata.com/def/CityStats/dimension#refGrupoCotizacion\",\"xType\":\"http://datos.localidata.com/def/CityStats/code#GrupoCotizacion\",\"xLabels\":{\"en\":\"Group\",\"es\":\"Grupo de cotización en Régimen General\"},\"yURI\":\"http://datos.localidata.com/def/CityStats/stats#afiliadosRGTrabajadores\",\"yType\":\"http://www.w3.org/2001/XMLSchema#int\",\"yLabels\":{\"en\":\"Number of workers affiliated, working in this territorial division\",\"es\":\"Número de afiliados en Régimen General trabajando en la división territorial\"},\"aggr\":\"\"}";
    
    String data = JSONtoXML(cadena);
    System.out.println(data);
  }
}
