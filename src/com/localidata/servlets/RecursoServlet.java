package com.localidata.servlets;

import com.github.jsonldjava.jena.JenaJSONLD;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.localidata.geojson.FeaturesCollection;
import com.localidata.utils.CacheURLs;
import com.localidata.utils.ConfigurationFile;
import com.localidata.utils.Messages;
import com.localidata.utils.PlantillasXSL;
import com.localidata.utils.Seguridad;
import com.localidata.utils.UtilidadesRecurso;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.lang.StringUtils;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import threescale.v3.api.ServerError;

public class RecursoServlet
  extends HttpServlet
{
  private static final long serialVersionUID = 1L;
  private static final String formatoAPI = "localidata-api";
  private static final String versionAPI = "1.0";
  private static Logger log = Logger.getLogger(RecursoServlet.class);
  private static final String localidataFile = "WEB-INF/localidata.properties";
  private static final String parametroOrden = "_sort=";
  private static final String ordenPorDefecto = "label";
  private static final String propiedadSeguridadActivada = "seguridad3Scale";
  private static final String propiedadApiKey3Scale = "localidataKey3Scale";
  private static final String propiedadUrlRecurso = "urlRecurso";
  private static final String propiedadUrlElda = "urlElda";
  private static final String propiedadUrlStats = "urlStats";
  private static final String propiedadContextoEstadisticas = "contextoEstadisticas";
  private static final String propiedadCronometrarProcesos = "cronometrarProcesos";
  private static final String propiedadPatronesUrls = "patronesUrls";
  private static final String propiedadPesoUrls = "pesoUrls";
  private static final String propiedadPuntoSparqlStats = "statisticsEndpoint";
  private static final String propiedadUrlStaticos = "urlStaticos";
  private static final String propiedadPatronesUrlsMetodo = "patronUrlsMetodo";
  private static final String propiedadMetodos = "metodos";
  private static final String propiedadPatronesUrlsEldasIndependientes = "patronUrlsEldaIndependientes";
  private static final String propiedadUrlsEldasIndependientes = "urlsEldaIndependientes";
  private static final String propiedadUrlsEstaticosIndependientes = "urlsEstaticosIndependientes";
  private static final String propiedadCacheCubo = "urlsCacheCubo";
  private static final String propiedadLimpiarRespuestaElda = "limpiarRespuestasElda";
  private static boolean seguridadActivada = true;
  private static String localidataKey = "";
  private static String urlRecurso = "";
  private static String urlEldaPrincipal = "";
  private static String urlStats = "";
  private static String contextoEstadisticas = "";
  private static boolean cronometrarProcesos = true;
  private static String puntoSparqlStats = "";
  private static String urlEstaticosPrincipal = "urlStaticos";
  private static String[] urlsConPeso = null;
  private static String[] pesoUrls = null;
  private static String[] urlsMetodos = null;
  private static String[] metodos = null;
  private static String[] patronesUrlEldasIndependientes = null;
  private static String[] urlsEldasIndependientes = null;
  private static String[] urlsEstaticosIndependientes = null;
  private static int limiteCacheCubo = 50;
  private static boolean limpiarRespuestas = false;
  private static final String parametroUserKey = "api_key";
  private static final String parametroMetadata = "_metadata";
  private static final String parametroServerStats = "Server";
  private static final String pesoPorDefecto = "1";
  private final String cadenaErrorJsonIni = "{result:{error:\"";
  private final String cadenaErrorJsonFin = "\"}}";
  private final int caracteresComodin = 4;
  private static String contexto = null;
  private static Map<String, String> mapaParametros = null;
  private static CacheURLs cacheCubo = null;
  
  public void init(ServletConfig config)
    throws ServletException
  {
    super.init(config);
    
    contexto = config.getServletContext().getContextPath().substring(1);
    
    configuracionInicial(config);
    
    JenaJSONLD.init();
  }
  
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
  {
    String userkey = "";
    String peso = "";
    String metodo = "";
    String referrer = "";
    String formatoStats = "";
    String url = "";
    boolean petitionJSONP = false;
    String llamadaCallBack = "";
    int autenticacion = -1;
    int posicionEnCache = -1;
    
    long marcaTiempo1 = 0L;
    long marcaTiempo2 = 0L;
    
    response.setContentType("text/html");
    response.setCharacterEncoding("utf-8");
    
    response.setHeader("Access-Control-Allow-Origin", "*");
    
    url = request.getRequestURL().toString();
    
    log.info("Url solicitada:" + url);
    
    String cabeceraAccept = "";
    if (request.getHeader("Accept") != null) {
      cabeceraAccept = request.getHeader("Accept");
    }
    log.info("Cabecera Accept:'" + cabeceraAccept + "'");
    
    String extension = extensionUrl(url, request);
    log.info("Extensión: '" + extension + "'");
    if ((extension.equals("")) && (url.indexOf(contextoEstadisticas) < 0))
    {
      String location = "";
      if (cabeceraAccept.indexOf("text/html") < 0)
      {
        if ((cabeceraAccept.indexOf("application/json") >= 0) || (cabeceraAccept.indexOf("text/javascript") >= 0) || 
          (cabeceraAccept.indexOf("application/javascript") >= 0)) {
          url = url + ".json";
        } else if ((cabeceraAccept.indexOf("text/xml") >= 0) || (cabeceraAccept.indexOf("application/xml") >= 0)) {
          url = url + ".xml";
        } else if (cabeceraAccept.indexOf("text/turtle") >= 0) {
          url = url + ".ttl";
        } else if (cabeceraAccept.indexOf("application/rdf+xml") >= 0) {
          url = url + ".rdf";
        } else if (cabeceraAccept.indexOf("text/plain") >= 0) {
          url = url + ".text";
        } else if (cabeceraAccept.indexOf("text/csv") >= 0) {
          url = url + ".csv";
        } else if (cabeceraAccept.indexOf("text/geocsv") >= 0) {
          url = url + ".geocsv";
        } else if (cabeceraAccept.indexOf("application/geojson") >= 0) {
          url = url + ".geojson";
        } else if (cabeceraAccept.indexOf("application/ld+json") >= 0) {
          url = url + ".jsonld";
        } else {
          url = url + ".html";
        }
        if (request.getQueryString() != null) {
          location = url + "?" + request.getQueryString();
        } else {
          location = url;
        }
        try
        {
          String direccionConDominio = location;
          if (direccionConDominio.indexOf("/recurso") > 0) {
            direccionConDominio = urlRecurso + direccionConDominio.substring(direccionConDominio.indexOf("/recurso"));
          }
          log.info("Devolvemos un seeOther cambiando el dominio: " + direccionConDominio);
          seeOther(direccionConDominio, response);
        }
        catch (IOException e)
        {
          log.error("Error al devolver error 303");
          log.error(e.getMessage());
          log.error(e.toString());
        }
        return;
      }
    }
    if ((request.getQueryString() != null) && (request.getQueryString().indexOf("callback=") >= 0))
    {
      petitionJSONP = true;
      llamadaCallBack = request.getParameter("callback");
    }
    if (request.getHeader("referer") != null)
    {
      referrer = request.getHeader("referer");
      log.info("Referer de cabecera: " + referrer);
      try
      {
        URI uri = new URI(referrer);
        referrer = uri.getHost();
        log.info("Referer recortado: " + referrer);
      }
      catch (URISyntaxException e)
      {
        log.error("Referer de cabecera erroneo: " + referrer);
      }
    }
    else
    {
      log.info("En la cabecera no existe la propiedad referer");
    }
    log.info("Stats - Url a procesar: " + UtilidadesRecurso.getFullURL(request));
    
    log.info("Stats - Ip origen de la petición: " + request.getRemoteAddr());
    if (referrer.equals("")) {
      log.info("Stats - Host origen de la petición: " + request.getRemoteHost());
    } else {
      log.info("Stats - Host origen de la petición: " + referrer);
    }
    if (referrer.equals(""))
    {
      referrer = request.getRemoteAddr();
      if ((request.getRemoteHost() != null) && (!referrer.equals(request.getRemoteHost()))) {
        referrer = request.getRemoteHost();
      }
    }
    try
    {
      mapaParametros = UtilidadesRecurso.getQueryMap(request.getQueryString());
    }
    catch (Exception e)
    {
      try
      {
        String mensajeHTML = UtilidadesRecurso.getCodigoPaginaError(Messages.getString("RecursoServlet.TituloPantallaError"), 
          Messages.getString("RecursoServlet.ErrorParametrosError1"), Messages.getString("RecursoServlet.ErrorParametrosError2"), urlEstaticosPrincipal);
        errorExtension(extension, 400, mensajeHTML, response);
      }
      catch (IOException ex)
      {
        log.error("Error al pintar error de parametros incorrectos en respuesta");
        log.error(e.getMessage());
        log.error(e.toString());
      }
      return;
    }
    if (url.indexOf("*") >= 0)
    {
      boolean comodinOk = true;
      int posicion = url.indexOf("*");
      char tempChar = ' ';
      for (int i = 1; i <= 4; i++)
      {
        tempChar = url.charAt(posicion - i);
        if ((tempChar == '/') || (tempChar == '\\'))
        {
          comodinOk = false;
          break;
        }
      }
      if (!comodinOk)
      {
        try
        {
          if (petitionJSONP)
          {
            response.getWriter().println(llamadaCallBack + "(" + "{result:{error:\"" + 406 + "\"}}" + ")");
          }
          else
          {
            String mensajeHTML = UtilidadesRecurso.getCodigoPaginaError(Messages.getString("RecursoServlet.TituloPantallaError"), 
              Messages.getString("RecursoServlet.ErrorComodin1"), Messages.getString("RecursoServlet.ErrorComodin2"), urlEstaticosPrincipal);
            errorExtension(extension, 406, mensajeHTML, response);
          }
        }
        catch (IOException e)
        {
          log.error("Error al pintar error de comodín en respuesta");
          log.error(e.getMessage());
          log.error(e.toString());
        }
        return;
      }
    }
    label1838:{
    if (seguridadActivada)
    {
      log.info("Comprobamos si la autenticación es correcta");
      if (request.getParameter("api_key") != null)
      {
        userkey = request.getParameter("api_key");
        log.info("Userkey obtenido de la url");
      }
      else if (request.getSession(true).getAttribute("cadenaUserKey") != null)
      {
        userkey = (String)request.getSession(true).getAttribute("cadenaUserKey");
        log.info("Userkey obtenido de la sesion");
      }
      if (!userkey.equals(""))
      {
        try
        {
          if (cronometrarProcesos) {
            marcaTiempo1 = Calendar.getInstance().getTimeInMillis();
          }
          peso = compruebaPeso(url);
          
          metodo = compruebaMetodo(url);
          
          autenticacion = Seguridad.autenticacion(localidataKey, userkey, referrer, peso, metodo);
          if (!cronometrarProcesos) {
            break label1838;
          }
          marcaTiempo2 = Calendar.getInstance().getTimeInMillis();
          log.info("Tiempo en autenticar: " + (marcaTiempo2 - marcaTiempo1) + " milisegundos");
        }
        catch (ServerError e)
        {
          log.error("Error al realizar la autenticación");
          log.error(e.getMessage());
          log.error(e.toString());
          e.printStackTrace();
        }
      }
      else
      {
        try
        {
          if (petitionJSONP)
          {
            response.setStatus(402);
            response.getWriter().println(llamadaCallBack + "(" + "{result:{error:\"" + 402 + "\"}}" + ")");
          }
          else
          {
            String mensajeHTML = 
              UtilidadesRecurso.getCodigoPaginaError(Messages.getString("RecursoServlet.TituloPantallaError"), Messages.getString("RecursoServlet.ErrorApiKey1"), 
              Messages.getString("RecursoServlet.ErrorApiKey2") + "api_key" + Messages.getString("RecursoServlet.ErrorApiKey3"), 
              urlEstaticosPrincipal);
            
            errorExtension(extension, 402, mensajeHTML, response);
          }
        }
        catch (IOException e)
        {
          log.error("Error al pintar error de comodín en respuesta");
          log.error(e.getMessage());
          log.error(e.toString());
        }
        return;
      }
    }
    }
    //label1838:
    if ((autenticacion < 0) && (seguridadActivada))
    {
      try
      {
        if (autenticacion == -2)
        {
          log.error("Autenticación incorrecta por referrer");
          if (petitionJSONP)
          {
            response.getWriter().println(llamadaCallBack + "(" + "{result:{error:\"" + 401 + "\"}}" + ")");
          }
          else
          {
            String mensajeHTML = UtilidadesRecurso.getCodigoPaginaError(Messages.getString("RecursoServlet.TituloPantallaError"), 
              Messages.getString("RecursoServlet.ErrorReferrer1") + referrer + Messages.getString("RecursoServlet.ErrorReferrer2"), 
              Messages.getString("RecursoServlet.ErrorReferrer3") + referrer + Messages.getString("RecursoServlet.ErrorReferrer4"), urlEstaticosPrincipal);
            
            errorExtension(extension, 401, mensajeHTML, response);
          }
        }
        else if (autenticacion == -3)
        {
          log.error("Autenticación incorrecta por limite de hits");
          if (petitionJSONP)
          {
            response.getWriter().println(llamadaCallBack + "(" + "{result:{error:\"" + 401 + "\"}}" + ")");
          }
          else
          {
            String mensajeHTML = UtilidadesRecurso.getCodigoPaginaError(Messages.getString("RecursoServlet.TituloPantallaError"), 
              Messages.getString("RecursoServlet.ErrorHits1"), Messages.getString("RecursoServlet.ErrorHits1"), urlEstaticosPrincipal);
            
            errorExtension(extension, 401, mensajeHTML, response);
          }
        }
        else
        {
          log.error("Autenticación incorrecta, lanzamos error 401");
          if (petitionJSONP)
          {
            response.getWriter().println(llamadaCallBack + "(" + "{result:{error:\"" + 401 + "\"}}" + ")");
          }
          else
          {
            String mensajeHTML = UtilidadesRecurso.getCodigoPaginaError(Messages.getString("RecursoServlet.TituloPantallaError"), 
              Messages.getString("RecursoServlet.ErrorAutenticacion1"), Messages.getString("RecursoServlet.ErrorAutenticacion2"), urlEstaticosPrincipal);
            
            errorExtension(extension, 401, mensajeHTML, response);
          }
        }
      }
      catch (IOException e)
      {
        log.error("Error al pintar error en respuesta");
        log.error(e.getMessage());
        log.error(e.toString());
      }
    }
    else
    {
      request.getSession(true).setAttribute("cadenaUserKey", userkey);
      
      String parametros = UtilidadesRecurso.eliminaParametro(request, new String[] { "api_key", "_metadata" });
      
      url = url.substring(url.indexOf(contexto) + contexto.length());
      try
      {
        try
        {
          PrintWriter out;
          if (url.indexOf(contextoEstadisticas + "/") < 0)
          {
            String[] propiedades = compruebaPatronElda(url);
            
            String urlElda = propiedades[0];
            String urlEstaticos = propiedades[1];
            
            log.info("Elda: " + urlElda);
            log.info("Estaticos: " + urlEstaticos);
            if (parametros.equals("")) {
              url = urlElda + url;
            } else {
              url = urlElda + url + "?" + parametros;
            }
            if (url.indexOf("_sort=") < 0) {
              if (url.indexOf("?") < 0) {
                url = url + "?" + "_sort=" + "label";
              } else {
                url = url + "&" + "_sort=" + "label";
              }
            }
            if (cronometrarProcesos) {
              marcaTiempo1 = Calendar.getInstance().getTimeInMillis();
            }
            String respuestaElda = "";
            if (extension.endsWith(".geojson")) {
              respuestaElda = UtilidadesRecurso.processUrlFromElda(url.replace(".geojson", ".json"), urlElda, urlRecurso, urlEstaticos);
            } else if (extension.endsWith(".jsonld")) {
              respuestaElda = UtilidadesRecurso.processUrlFromElda(url.replace(".jsonld", ".rdf"), urlElda, urlRecurso, urlEstaticos);
            } else {
              respuestaElda = UtilidadesRecurso.processUrlFromElda(url, urlElda, urlRecurso, urlEstaticos);
            }
            if (cronometrarProcesos)
            {
              marcaTiempo2 = Calendar.getInstance().getTimeInMillis();
              log.info("Tiempo en procesar la url: " + (marcaTiempo2 - marcaTiempo1) + " milisegundos");
            }
            if (limpiarRespuestas)
            {
              if (cronometrarProcesos) {
                marcaTiempo1 = Calendar.getInstance().getTimeInMillis();
              }
              respuestaElda = limpiarRespuesta(extension, respuestaElda, petitionJSONP);
              if (cronometrarProcesos)
              {
                marcaTiempo2 = Calendar.getInstance().getTimeInMillis();
                log.info("Tiempo en limpiar la respuesta: " + (marcaTiempo2 - marcaTiempo1) + " milisegundos");
              }
            }
            if (extension.endsWith(".jsonld"))
            {
              if (cronometrarProcesos) {
                marcaTiempo1 = Calendar.getInstance().getTimeInMillis();
              }
              respuestaElda = generaJSONLDFromRDF(respuestaElda);
              if (cronometrarProcesos)
              {
                marcaTiempo2 = Calendar.getInstance().getTimeInMillis();
                log.info("Tiempo en transformar rdf en jsonld: " + (marcaTiempo2 - marcaTiempo1) + " milisegundos");
              }
            }
            if ((extension.endsWith(".json")) || (extension.endsWith(".geojson")) || (extension.endsWith(".jsonld"))) {
              response.setContentType("application/json");
            } else if (extension.endsWith(".rdf")) {
              response.setContentType("application/rdf+xml");
            } else if ((extension.endsWith(".csv")) || (extension.endsWith(".geocsv"))) {
              response.setContentType("text/csv");
            } else if (extension.endsWith(".xml")) {
              response.setContentType("text/xml");
            } else if (extension.endsWith(".ttl")) {
              response.setContentType("text/turtle");
            } else if (extension.endsWith(".text")) {
              response.setContentType("text/plain");
            } else {
              response.setContentType("text/html");
            }
            out = response.getWriter();
            if (extension.endsWith(".geojson"))
            {
              FeaturesCollection geoJson = new FeaturesCollection();
              geoJson.jsonToGeoJson(respuestaElda);
              out.println(geoJson.toString());
            }
            else
            {
              out.println(respuestaElda);
            }
          }
          else
          {
            String[] suprimirParametros = { "Server", "api_key", "callback", "_" };
            
            parametros = UtilidadesRecurso.eliminaParametro(request, suprimirParametros);
            if (parametros.equals("")) {
              parametros = "Server=" + puntoSparqlStats;
            } else {
              parametros = "Server=" + puntoSparqlStats + "&" + parametros;
            }
            if (url.endsWith(".json"))
            {
              formatoStats = "json";
              url = url.replace(".json", "");
            }
            else if (url.endsWith(".csv"))
            {
              formatoStats = "csv";
              url = url.replace(".csv", "");
              response.setContentType("text/html");
            }
            else if (url.endsWith(".xml"))
            {
              formatoStats = "xml";
              url = url.replace(".xml", "");
              response.setContentType("text/xml");
            }
            else if (url.endsWith(".html"))
            {
              formatoStats = "html";
              url = url.replace(".html", "");
              response.setContentType("text/html");
            }
            else
            {
              formatoStats = "json";
            }
            out = response.getWriter();
            
            url = url.substring(url.indexOf(contextoEstadisticas) + contextoEstadisticas.length());
            url = urlStats + url + "?" + parametros;
            
            url = UtilidadesRecurso.trataAlmohadilla(url);
            
            String contenido = "";
            if (limiteCacheCubo > 0) {
              posicionEnCache = cacheCubo.indexOf(url);
            }
            if (posicionEnCache >= 0)
            {
              log.info("Contenido en cache");
              contenido = cacheCubo.getContent(posicionEnCache);
            }
            else
            {
              if (cronometrarProcesos) {
                marcaTiempo1 = Calendar.getInstance().getTimeInMillis();
              }
              contenido = UtilidadesRecurso.processUrlFromStats(url);
              log.info("Llamada a url de cubo: " + url);
              if (cronometrarProcesos)
              {
                marcaTiempo2 = Calendar.getInstance().getTimeInMillis();
                log.info("Tiempo en procesar la url del cubo: " + (marcaTiempo2 - marcaTiempo1) + " milisegundos");
              }
              if (limiteCacheCubo > 0) {
                cacheCubo.add(url, contenido);
              }
            }
            boolean errorTransformacion = true;
            try
            {
              if (formatoStats.equals("xml"))
              {
                contenido = generaContenidoJSONEnXML(url, contenido);
              }
              else if (formatoStats.equals("csv"))
              {
                contenido = generaContenidoJSONEnXML(url, contenido);
                if (url.indexOf("services/getStatisticsValues?") > 0) {
                  contenido = UtilidadesRecurso.transformXSL(contenido, PlantillasXSL.getCsvGetStatisticsValue());
                } else if (url.indexOf("services/getStatistics?") > 0) {
                  contenido = UtilidadesRecurso.transformXSL(contenido, PlantillasXSL.getCsvGetStatistics());
                } else if (url.indexOf("services/getDimensions?") > 0) {
                  contenido = UtilidadesRecurso.transformXSL(contenido, PlantillasXSL.getCsvGetDimensions());
                } else if (url.indexOf("services/getStatisticsXValues?") > 0) {
                  contenido = UtilidadesRecurso.transformXSL(contenido, PlantillasXSL.getCsvGetStatisticsXValues());
                } else if (url.indexOf("services/getOrderDimensions?") > 0) {
                  contenido = UtilidadesRecurso.transformXSL(contenido, PlantillasXSL.getCsvGetOrderDimensions());
                }
              }
              else if (formatoStats.equals("html"))
              {
                if (url.indexOf("services/getStatisticsValues?") > 0)
                {
                  contenido = generaContenidoJSONEnXML(url, contenido);
                  if ((mapaParametros != null) && (mapaParametros.containsKey("graphic")))
                  {
                    if (((String)mapaParametros.get("graphic")).equals("pie")) {
                      contenido = UtilidadesRecurso.transformXSL(contenido, PlantillasXSL.getHtmlGetStatisticsValueGraphPie());
                    } else if (((String)mapaParametros.get("graphic")).equals("bar")) {
                      contenido = UtilidadesRecurso.transformXSL(contenido, PlantillasXSL.getHtmlGetStatisticsValueGraphBar());
                    } else if (((String)mapaParametros.get("graphic")).equals("column")) {
                      contenido = UtilidadesRecurso.transformXSL(contenido, PlantillasXSL.getHtmlGetStatisticsValueGraphColumn());
                    } else if (((String)mapaParametros.get("graphic")).equals("line")) {
                      contenido = UtilidadesRecurso.transformXSL(contenido, PlantillasXSL.getHtmlGetStatisticsValueGraphLine());
                    } else {
                      contenido = UtilidadesRecurso.transformXSL(contenido, PlantillasXSL.getHtmlGetStatisticsValue());
                    }
                  }
                  else {
                    contenido = UtilidadesRecurso.transformXSL(contenido, PlantillasXSL.getHtmlGetStatisticsValue());
                  }
                }
                else if (url.indexOf("services/getStatistics?") > 0)
                {
                  contenido = UtilidadesRecurso.transformXSL(contenido, PlantillasXSL.getHtmlGetStatistics());
                }
                else if (url.indexOf("services/getDimensions?") > 0)
                {
                  contenido = UtilidadesRecurso.transformXSL(contenido, PlantillasXSL.getHtmlGetDimensions());
                }
                else if (url.indexOf("services/getStatisticsXValues?") > 0)
                {
                  contenido = UtilidadesRecurso.transformXSL(contenido, PlantillasXSL.getHtmlGetStatisticsXValues());
                }
                else if (url.indexOf("services/getOrderDimensions?") > 0)
                {
                  contenido = UtilidadesRecurso.transformXSL(contenido, PlantillasXSL.getHtmlGetOrderDimensions());
                }
              }
              errorTransformacion = false;
            }
            catch (Exception e)
            {
              log.error("Error al aplicar la transformación en el cubo de datos");
              log.error(e.getMessage());
              log.error(e.toString());
              out.close();
              
              response.setContentType("text/html");
              response.setStatus(400);
              response.getWriter().println(
                UtilidadesRecurso.getCodigoPaginaError(Messages.getString("RecursoServlet.TituloPantallaError"), 
                Messages.getString("RecursoServlet.ErrorTransCubo1"), Messages.getString("RecursoServlet.ErrorTransCubo2"), urlEstaticosPrincipal));
            }
            if (!errorTransformacion) {
              if (petitionJSONP) {
                out.println(llamadaCallBack + "(" + contenido + ")");
              } else {
                out.println(contenido);
              }
            }
          }
          out.close();
        }
        catch (MalformedURLException me)
        {
          log.error("Error al cargar la url recibida: " + url);
          log.error(me.getMessage());
          log.error(me.toString());
          if (petitionJSONP)
          {
            response.getWriter().println(llamadaCallBack + "(" + "{result:{error:\"" + 400 + "\"}}" + ")");
          }
          else
          {
            response.setContentType("text/html");
            response.setStatus(400);
            response.getWriter().println(
              UtilidadesRecurso.getCodigoPaginaError(Messages.getString("RecursoServlet.TituloPantallaError"), 
              Messages.getString("RecursoServlet.ErrorNoExiste1"), Messages.getString("RecursoServlet.ErrorNoExiste2"), urlEstaticosPrincipal));
          }
        }
        catch (FileNotFoundException me)
        {
          log.error("Error al cargar la url recibida: " + url);
          log.error(me.getMessage());
          log.error(me.toString());
          if (petitionJSONP)
          {
            response.getWriter().println(llamadaCallBack + "(" + "{result:{error:\"" + 400 + "\"}}" + ")");
          }
          else
          {
            response.setContentType("text/html");
            response.setStatus(400);
            response.getWriter().println(
              UtilidadesRecurso.getCodigoPaginaError(Messages.getString("RecursoServlet.TituloPantallaError"), 
              Messages.getString("RecursoServlet.ErrorNoExiste1"), Messages.getString("RecursoServlet.ErrorNoExiste2"), urlEstaticosPrincipal));
          }
        }
        catch (IOException e)
        {
          log.error("Error de Entrada/Salida al procesar la url recibida</br>");
          log.error(e.getMessage());
          log.error(e.toString());
          if (petitionJSONP)
          {
            response.getWriter().println(llamadaCallBack + "(" + "{result:{error:\"" + 400 + "\"}}" + ")");
          }
          else
          {
            response.setContentType("text/html");
            response.setStatus(400);
            response.getWriter().println(
              UtilidadesRecurso.getCodigoPaginaError(Messages.getString("RecursoServlet.TituloPantallaError"), 
              Messages.getString("RecursoServlet.ErrorNoExiste1"), Messages.getString("RecursoServlet.ErrorNoExiste2"), urlEstaticosPrincipal));
          }
        }
        return;
      }
      catch (IOException e1)
      {
        log.error("Error de Entrada/Salida al utilizar la salida de datos");
        log.error(e1.getMessage());
        log.error(e1.getStackTrace());
      }
    }
  }
  
  private String generaJSONLDFromRDF(String respuestaElda)
  {
    Model model = ModelFactory.createDefaultModel();
    InputStream is = new ByteArrayInputStream(respuestaElda.getBytes());
    model.read(is, null);
    StringWriter salida = new StringWriter();
    RDFDataMgr.write(salida, model, JenaJSONLD.JSONLD);
    respuestaElda = salida.toString();
    return respuestaElda;
  }
  
  private String limpiarRespuesta(String extension, String respuestaElda, boolean isJSONP)
  {
    if ((extension.endsWith(".json")) || (extension.endsWith(".text")))
    {
      String prefijo = "";
      if ((isJSONP) && (respuestaElda.indexOf("(") >= 0))
      {
        prefijo = respuestaElda.substring(0, respuestaElda.indexOf("(") + 1);
        respuestaElda = respuestaElda.substring(respuestaElda.indexOf("(") + 1);
        respuestaElda = respuestaElda.substring(0, respuestaElda.lastIndexOf(")"));
      }
      JSONParser parser = new JSONParser();
      JSONObject obj = null;
      try
      {
        obj = (JSONObject)parser.parse(respuestaElda);
        JSONObject result = (JSONObject)obj.get("result");
        result.remove("extendedMetadataVersion");
        result.remove("isPartOf");
        result.remove("definition");
        result.remove("type");
        result.remove("hasPart");
        obj.put("result", result);
        obj.put("format", "localidata-api");
        obj.put("version", "1.0");
        if (prefijo.equals("")) {
          respuestaElda = obj.toJSONString();
        } else {
          respuestaElda = prefijo + obj.toJSONString() + ")";
        }
      }
      catch (ParseException e)
      {
        log.error("Error al parsear JSON para reformarlo");
        log.error(e.getMessage());
        log.error(e.toString());
      }
    }
    else
    {
      Object salida;
      if ((extension.endsWith(".rdf")) || (extension.endsWith(".jsonld")))
      {
        Model model = ModelFactory.createDefaultModel();
        
        InputStream is = new ByteArrayInputStream(respuestaElda.getBytes());
        
        model.read(is, null);
        
        StmtIterator it = model.listStatements();
        ArrayList<Statement> deleteList = new ArrayList();
        while (it.hasNext())
        {
          Statement stmt = (Statement)it.next();
          if ((stmt.getPredicate().toString().equals("http://purl.org/dc/terms/hasPart")) || 
            (stmt.getPredicate().toString().equals("http://purl.org/linked-data/api/vocab#definition")) || 
            (stmt.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) || 
            (stmt.getPredicate().toString().equals("http://purl.org/dc/terms/isPartOf")) || 
            (stmt.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) || 
            (stmt.getPredicate().toString().equals("http://purl.org/linked-data/api/vocab#extendedMetadataVersion"))) {
            deleteList.add(stmt);
          }
        }
        for (Statement s : deleteList) {
          model.remove(s);
        }
        String syntax = "RDF/XML-ABBREV";
        salida = new StringWriter();
        model.write((Writer)salida, syntax);
        respuestaElda = ((StringWriter)salida).toString();
      }
      else if (extension.endsWith(".ttl"))
      {
        Model model = ModelFactory.createDefaultModel();
        
        InputStream is = new ByteArrayInputStream(respuestaElda.getBytes());
        
        model.read(is, null, "TTL");
        
        StmtIterator it = model.listStatements();
        ArrayList<Statement> deleteList = new ArrayList();
        while (it.hasNext())
        {
          Statement stmt = (Statement)it.next();
          if ((stmt.getPredicate().toString().equals("http://purl.org/dc/terms/hasPart")) || 
            (stmt.getPredicate().toString().equals("http://purl.org/linked-data/api/vocab#definition")) || 
            (stmt.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) || 
            (stmt.getPredicate().toString().equals("http://purl.org/dc/terms/isPartOf")) || 
            (stmt.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) || 
            (stmt.getPredicate().toString().equals("http://purl.org/linked-data/api/vocab#extendedMetadataVersion"))) {
            deleteList.add(stmt);
          }
        }
        for (salida = deleteList.iterator(); ((Iterator)salida).hasNext();)
        {
          Statement s = (Statement)((Iterator)salida).next();
          model.remove(s);
        }
        String syntax = "TURTLE";
        salida = new StringWriter();
        model.write((Writer) salida, syntax);
        respuestaElda = salida.toString();
      }
      else if (extension.endsWith(".xml"))
      {
        DocumentBuilderFactory docBuilderFactory = null;
        DocumentBuilder docBuilder = null;
        docBuilderFactory = DocumentBuilderFactory.newInstance();
        try
        {
          docBuilder = docBuilderFactory.newDocumentBuilder();
          Document doc = docBuilder.parse(new ByteArrayInputStream(respuestaElda.getBytes()));
          
          Element e = (Element)doc.getElementsByTagName("definition").item(0);
          if (e != null) {
            e.getParentNode().removeChild(e);
          }
          e = (Element)doc.getElementsByTagName("extendedMetadataVersion").item(0);
          if (e != null) {
            e.getParentNode().removeChild(e);
          }
          e = (Element)doc.getElementsByTagName("isPartOf").item(0);
          if (e != null) {
            e.getParentNode().removeChild(e);
          }
          e = (Element)doc.getElementsByTagName("hasPart").item(0);
          if (e != null) {
            e.getParentNode().removeChild(e);
          }
          e = (Element)doc.getElementsByTagName("tipo").item(0);
          if (e != null) {
            e.getParentNode().removeChild(e);
          }
          doc.normalize();
          
          e = (Element)doc.getElementsByTagName("result").item(0);
          e.setAttribute("format", "localidata-api");
          e.setAttribute("version", "1.0");
          
          StringWriter sw = new StringWriter();
          TransformerFactory tf = TransformerFactory.newInstance();
          Transformer transformer = tf.newTransformer();
          transformer.setOutputProperty("omit-xml-declaration", "no");
          transformer.setOutputProperty("method", "xml");
          transformer.setOutputProperty("indent", "yes");
          transformer.setOutputProperty("encoding", "UTF-8");
          transformer.transform(new DOMSource(doc), new StreamResult(sw));
          respuestaElda = sw.toString();
        }
        catch (Exception e)
        {
          log.error("Error al tratar XML para reformarlo");
          log.error(e.getMessage());
          log.error(e.toString());
        }
      }
    }
    return respuestaElda;
  }
  
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {}
  
  private void configuracionInicial(ServletConfig config)
  {
    log.info("Carga inicial de Servlet Recurso");
    
    String rutaFicheroConfiguracion = config.getServletContext().getRealPath("/") + "WEB-INF/localidata.properties";
    try
    {
      ConfigurationFile cf = new ConfigurationFile(rutaFicheroConfiguracion);
      
      localidataKey = UtilidadesRecurso.asignaPropiedad(cf, "localidataKey3Scale");
      
      urlRecurso = UtilidadesRecurso.asignaPropiedad(cf, "urlRecurso");
      
      urlEldaPrincipal = UtilidadesRecurso.asignaPropiedad(cf, "urlElda");
      
      urlStats = UtilidadesRecurso.asignaPropiedad(cf, "urlStats");
      
      contextoEstadisticas = UtilidadesRecurso.asignaPropiedad(cf, "contextoEstadisticas");
      
      puntoSparqlStats = UtilidadesRecurso.asignaPropiedad(cf, "statisticsEndpoint");
      
      urlEstaticosPrincipal = UtilidadesRecurso.asignaPropiedad(cf, "urlStaticos");
      if (UtilidadesRecurso.asignaPropiedad(cf, "seguridad3Scale").equals("false")) {
        seguridadActivada = false;
      }
      if (UtilidadesRecurso.asignaPropiedad(cf, "cronometrarProcesos").equals("false")) {
        cronometrarProcesos = false;
      }
      urlsConPeso = UtilidadesRecurso.asignaPropiedad(cf, "patronesUrls").split(",");
      pesoUrls = UtilidadesRecurso.asignaPropiedad(cf, "pesoUrls").split(",");
      if ((urlsConPeso.length == 1) && (urlsConPeso[0].equals("")))
      {
        urlsConPeso = new String[0];
        pesoUrls = new String[0];
      }
      urlsMetodos = UtilidadesRecurso.asignaPropiedad(cf, "patronUrlsMetodo").split(",");
      metodos = UtilidadesRecurso.asignaPropiedad(cf, "metodos").split(",");
      if ((urlsMetodos.length == 1) && (urlsMetodos[0].equals("")))
      {
        urlsMetodos = new String[0];
        metodos = new String[0];
      }
      patronesUrlEldasIndependientes = UtilidadesRecurso.asignaPropiedad(cf, "patronUrlsEldaIndependientes").split(",");
      urlsEldasIndependientes = UtilidadesRecurso.asignaPropiedad(cf, "urlsEldaIndependientes").split(",");
      urlsEstaticosIndependientes = UtilidadesRecurso.asignaPropiedad(cf, "urlsEstaticosIndependientes").split(",");
      if ((patronesUrlEldasIndependientes.length == 1) && (patronesUrlEldasIndependientes[0].equals("")))
      {
        patronesUrlEldasIndependientes = new String[0];
        urlsEldasIndependientes = new String[0];
        urlsEstaticosIndependientes = new String[0];
      }
      if ((UtilidadesRecurso.asignaPropiedad(cf, "urlsCacheCubo") != null) && (!UtilidadesRecurso.asignaPropiedad(cf, "urlsCacheCubo").equals(""))) {
        limiteCacheCubo = Integer.parseInt(UtilidadesRecurso.asignaPropiedad(cf, "urlsCacheCubo"));
      }
      cacheCubo = new CacheURLs();
      cacheCubo.setMax(limiteCacheCubo);
      if (UtilidadesRecurso.asignaPropiedad(cf, "limpiarRespuestasElda").equals("true")) {
        limpiarRespuestas = true;
      }
    }
    catch (FileNotFoundException e)
    {
      log.error("Fichero de Configuración no encontrado: " + rutaFicheroConfiguracion);
      log.error(e.getMessage());
      log.error(e.toString());
    }
    catch (IOException e)
    {
      log.error("Error de Entrada/Salida al leer el fichero: " + rutaFicheroConfiguracion);
      log.error(e.getMessage());
      log.error(e.toString());
    }
    log.info("Fin de carga inicial de Servlet Recurso");
  }
  
  private String generaContenidoJSONEnXML(String url, String contenido)
  {
    String contenidoTemp = contenido;
    if (url.indexOf("services/getStatisticsValues?") < 0)
    {
      contenidoTemp = StringUtils.substringBetween(contenidoTemp, "[", "]");
      contenidoTemp = "{\"datos\":[" + contenidoTemp + "]}";
    }
    return UtilidadesRecurso.JSONtoXML(contenidoTemp);
  }
  
  private String compruebaPeso(String url)
  {
    String pesoTemp = "1";
    for (int i = 0; i < urlsConPeso.length; i++) {
      if (url.toLowerCase().indexOf(urlsConPeso[i].toLowerCase().trim()) >= 0)
      {
        pesoTemp = pesoUrls[i];
        break;
      }
    }
    return pesoTemp;
  }
  
  private String compruebaMetodo(String url)
  {
    String metodo = null;
    if (urlsMetodos.length > 0) {
      for (int i = 0; i < urlsMetodos.length; i++) {
        if (url.toLowerCase().indexOf(urlsMetodos[i].toLowerCase().trim()) > 0)
        {
          metodo = metodos[i];
          break;
        }
      }
    }
    return metodo;
  }
  
  private String[] compruebaPatronElda(String url)
  {
    String[] propiedadesIndependientes = new String[2];
    propiedadesIndependientes[0] = urlEldaPrincipal;
    propiedadesIndependientes[1] = urlEstaticosPrincipal;
    if (patronesUrlEldasIndependientes.length > 0) {
      for (int i = 0; i < patronesUrlEldasIndependientes.length; i++) {
        if (url.toLowerCase().indexOf(patronesUrlEldasIndependientes[i].toLowerCase().trim()) >= 0)
        {
          propiedadesIndependientes[0] = urlsEldasIndependientes[i];
          propiedadesIndependientes[1] = urlsEstaticosIndependientes[i];
          break;
        }
      }
    }
    return propiedadesIndependientes;
  }
  
  private void seeOther(String url, HttpServletResponse response)
    throws IOException
  {
    response.setStatus(303);
    response.setHeader("Location", url);
    response.addHeader("Content-Type", "text/plain");
    String entityBody = "See Other: " + url;
    response.setContentLength(entityBody.length());
    response.getOutputStream().write(entityBody.getBytes());
  }
  
  private String extensionUrl(String url, HttpServletRequest request)
  {
    String extension = "";
    try
    {
      String urlTemporal = url.substring(url.indexOf(request.getContextPath()));
      if (urlTemporal.indexOf(".") >= 0) {
        extension = urlTemporal.substring(urlTemporal.lastIndexOf("."));
      }
    }
    catch (Exception e)
    {
      log.error("Error al obtener extensión de URL: " + e.getMessage());
    }
    return extension;
  }
  
  private void errorExtension(String extension, int codigoError, String mensajeHtml, HttpServletResponse response)
    throws IOException
  {
    if ((extension.endsWith(".json")) || (extension.endsWith(".geojson")))
    {
      response.setContentType("application/json");
      response.setStatus(codigoError);
      response.getWriter().println("{\"error\":" + codigoError + "}");
    }
    else if (extension.endsWith(".rdf"))
    {
      response.setContentType("application/rdf+xml");
      response.setStatus(codigoError);
      response.getWriter().println(
        "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><rdf:Description>" + codigoError + "</rdf:Description></rdf:RDF>");
    }
    else if ((extension.endsWith(".csv")) || (extension.endsWith(".geocsv")))
    {
      response.setContentType("text/csv");
      response.setStatus(codigoError);
      response.getWriter().println("error:" + codigoError + ";");
    }
    else if (extension.endsWith(".xml"))
    {
      response.setContentType("text/xml");
      response.setStatus(codigoError);
      response.getWriter().println("<error>" + codigoError + "</error>");
    }
    else if (extension.endsWith(".ttl"))
    {
      response.setContentType("text/plain");
      response.setStatus(codigoError);
      response.getWriter().println("error:" + codigoError);
    }
    else if (extension.endsWith(".text"))
    {
      response.setContentType("text/plain");
      response.setStatus(codigoError);
      response.getWriter().println("error:" + codigoError);
    }
    else
    {
      response.setContentType("text/html");
      response.setStatus(codigoError);
      response.getWriter().println(mensajeHtml);
    }
  }
}
