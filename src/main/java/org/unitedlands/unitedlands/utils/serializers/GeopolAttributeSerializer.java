package org.unitedlands.unitedlands.utils.serializers;

import org.unitedlands.unitedlands.classes.GeopolAttribute;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class GeopolAttributeSerializer implements JsonSerializer<GeopolAttribute> {

    @Override
    public JsonElement serialize(GeopolAttribute src, Type typeOfSrc, JsonSerializationContext context) {
        var jsonAttribute = new JsonObject();
        jsonAttribute.addProperty("display", src.getDisplay());
        jsonAttribute.addProperty("currentValue", src.getCurrentValue());
        jsonAttribute.addProperty("minValue", src.getMinValue());
        jsonAttribute.addProperty("maxValue", src.getMaxValue());
        jsonAttribute.addProperty("dailyChange", src.getDailyChange());
        return jsonAttribute;
    }
}
