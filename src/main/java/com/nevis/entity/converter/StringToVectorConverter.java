package com.nevis.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;
import java.sql.SQLException;

@Converter
public class StringToVectorConverter implements AttributeConverter<String, Object> {

  @Override
  public Object convertToDatabaseColumn(String attribute) {
    if (attribute == null) return null;
    try {
      PGobject vectorObject = new PGobject();
      vectorObject.setType("vector");
      vectorObject.setValue(attribute);
      return vectorObject;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to convert String to PGvector", e);
    }
  }

  @Override
  public String convertToEntityAttribute(Object dbData) {
    return dbData == null ? null : dbData.toString();
  }
}
