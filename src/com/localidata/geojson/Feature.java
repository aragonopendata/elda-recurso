package com.localidata.geojson;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Feature
{
  private JSONObject feature;
  private JSONObject properties;
  private JSONObject geometry;
  private JSONArray coordinates;
  
  public Feature()
  {
    this.feature = new JSONObject();
    
    this.properties = new JSONObject();
    this.geometry = new JSONObject();
    this.coordinates = new JSONArray();
    
    this.feature.put("type", "Feature");
    this.feature.put("properties", this.properties);
    this.feature.put("geometry", this.geometry);
    
    this.geometry.put("coordinates", this.coordinates);
  }
  
  public void putProperty(String name, Object property)
  {
    this.properties.put(name, property);
  }
  
  public void putTypeGeometry(String type)
  {
    this.geometry.put("type", type);
    if (type.equals("MultiPolygon"))
    {
      JSONArray sCoordinates1 = new JSONArray();
      JSONArray sCoordinates2 = new JSONArray();
      sCoordinates1.add(sCoordinates2);
      sCoordinates2.add(this.coordinates);
      this.geometry.put("coordinates", sCoordinates1);
    }
  }
  
  public JSONObject getFeature()
  {
    return this.feature;
  }
  
  public void putCoordinates(Double longitud, Double latitud)
  {
    if ((this.geometry.get("type") != null) && (((String)this.geometry.get("type")).equals("Point")))
    {
      this.coordinates.add(longitud);
      this.coordinates.add(latitud);
    }
    else
    {
      JSONArray coord = new JSONArray();
      coord.add(longitud);
      coord.add(latitud);
      this.coordinates.add(coord);
    }
  }
  
  public void putCoordinates(String longitud, String latitud)
  {
    putCoordinates(new Double(longitud), new Double(latitud));
  }
  
  public void putCoordinates(Float longitud, Float latitud)
  {
    putCoordinates(new Double(longitud.toString()), new Double(latitud.toString()));
  }
  
  public String toString()
  {
    return this.feature.toJSONString();
  }
  
  public static void main(String[] args)
  {
    Feature f = new Feature();
    f.putProperty("area", "0");
    f.putProperty("name", "Andorra");
    
    f.putTypeGeometry("MultiPolygon");
    
    f.putCoordinates(new Float(1.710967D), new Float(42.473499D));
    f.putCoordinates(new Float(1.533333D), new Float(42.4361D));
    f.putCoordinates(new Float(1.448333D), new Float(42.450821D));
    f.putCoordinates(new Float(1.446388D), new Float(42.572208D));
    f.putCoordinates(new Float(1.435247D), new Float(42.597149D));
    f.putCoordinates(new Float(1.541111D), new Float(42.65387D));
    f.putCoordinates(new Float(1.781667D), new Float(42.581661D));
    f.putCoordinates(new Float(1.710967D), new Float(42.473499D));
  }
}
