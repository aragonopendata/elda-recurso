package com.localidata.geojson;

import com.localidata.utils.UtilidadesRecurso;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class FeaturesCollection
{
  private JSONObject geoJsonObject;
  private JSONArray features;
  
  public FeaturesCollection()
  {
    this.geoJsonObject = new JSONObject();
    
    this.features = new JSONArray();
    
    this.geoJsonObject.put("features", this.features);
    
    this.geoJsonObject.put("type", "FeatureCollection");
  }
  
  public JSONObject getGeoJson()
  {
    return this.geoJsonObject;
  }
  
  public void addFeature(Feature f)
  {
    this.features.add(f);
  }
  
  public void writeFile(String ruta)
  {
    try
    {
      FileWriter file = new FileWriter(ruta);
      file.write(this.geoJsonObject.toJSONString());
      file.flush();
      file.close();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    System.out.print(this.geoJsonObject);
  }
  
  public String toString()
  {
    return this.geoJsonObject.toJSONString();
  }
  
  public void urlLocalidataJsonToGeoJson(String urlJson)
  {
    if (urlJson.indexOf(".json") < 0)
    {
      System.out.println("Url no valida");
      return;
    }
    String cadenaJson = "";
    try
    {
      cadenaJson = UtilidadesRecurso.procesaURL(urlJson);
      jsonToGeoJson(cadenaJson);
    }
    catch (MalformedURLException e)
    {
      e.printStackTrace();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }
  
  public void jsonToGeoJson(String jsonText)
  {
    JSONParser parser = new JSONParser();
    try
    {
      Object obj = parser.parse(jsonText);
      
      JSONObject jsonObject = (JSONObject)obj;
      
      JSONObject result = (JSONObject)jsonObject.get("result");
      
      JSONArray items = (JSONArray)result.get("items");
      
      Iterator<JSONObject> iterator = items.iterator();
      while (iterator.hasNext())
      {
        JSONObject item = (JSONObject)iterator.next();
        Feature f = new Feature();
        
        Set<String> set = item.keySet();
        
        Iterator<String> iteratorItem = set.iterator();
        while (iteratorItem.hasNext())
        {
          String propiedad = (String)iteratorItem.next();
          if (propiedad.equals("posicion"))
          {
            JSONObject posicion = (JSONObject)item.get(propiedad);
            f.putTypeGeometry("Point");
            f.putCoordinates((Double)posicion.get("longitud"), (Double)posicion.get("latitud"));
          }
          else if (propiedad.equals("tieneGeometria"))
          {
            f.putTypeGeometry("MultiPolygon");
            
            JSONArray tieneGeometria = (JSONArray)item.get(propiedad);
            for (Object GEO : tieneGeometria)
            {
              JSONObject temp = (JSONObject)GEO;
              if (((String)temp.get("_about")).endsWith("WGS84"))
              {
                String geometria = (String)temp.get("geometria");
                geometria = geometria.substring(geometria.indexOf("(("));
                geometria = geometria.replace("((", "");
                geometria = geometria.replace("))", "");
                String[] coordenadas = geometria.split(",");
                String[] arrayOfString1;
                int j = (arrayOfString1 = coordenadas).length;
                for (int i = 0; i < j; i++)
                {
                  String c = arrayOfString1[i];
                  
                  f.putCoordinates(c.substring(0, c.indexOf(" ") - 1), c.substring(c.indexOf(" ") + 1));
                }
                break;
              }
            }
          }
          else
          {
            f.putProperty(propiedad, item.get(propiedad));
          }
        }
        this.features.add(f);
      }
    }
    catch (ParseException e)
    {
      e.printStackTrace();
    }
  }
  
  public static void main(String[] args) {}
}
