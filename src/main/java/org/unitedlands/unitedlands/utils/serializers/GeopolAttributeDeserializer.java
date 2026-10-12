package org.unitedlands.unitedlands.utils.serializers;

import java.lang.reflect.Type;

import org.unitedlands.unitedlands.classes.GeopolAttribute;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;

public class GeopolAttributeDeserializer implements JsonDeserializer<GeopolAttribute> {
    @Override
    public GeopolAttribute deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        var jsonObj = json.getAsJsonObject();
        var display = jsonObj.get("display").getAsString();
        var currentValue = jsonObj.get("currentValue").getAsDouble();
        var minValue = jsonObj.get("minValue").getAsDouble();
        var maxValue = jsonObj.get("maxValue").getAsDouble();
        var dailyChange = jsonObj.get("dailyChange").getAsDouble();
        return new GeopolAttribute(display, currentValue, minValue, maxValue, dailyChange);
    }
}
