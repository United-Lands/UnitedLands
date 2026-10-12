package org.unitedlands.unitedlands.utils;

import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.util.*;

public class SvgUtils {

    public static Map<String, SVGPolygon> loadFile(File file) throws Exception {
        return parsePolygons(file);
    }

    public static class SVGPolygon {
        public double[] vertices;
        public String color;
    }

    public static Map<String, SVGPolygon> parsePolygons(File svgFile) throws Exception {
        Map<String, SVGPolygon> result = new LinkedHashMap<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // SVG uses a namespace
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(svgFile);

        NodeList polygons = doc.getElementsByTagNameNS("*", "polygon");

        for (int i = 0; i < polygons.getLength(); i++) {

            Element poly = (Element) polygons.item(i);
            String style = poly.getAttribute("style");
            String strokeColor = extractStrokeColor(style);

            String id = poly.getAttribute("id");
            if (id == null || id.isEmpty()) {
                id = "Region_" + i; // fallback if no id given
            }

            String pointsAttr = poly.getAttribute("points");

            var p = new SVGPolygon();
            p.vertices = parsePoints(pointsAttr);
            p.color = strokeColor;

            result.put(id, p);
        }

        return result;
    }

    // Parses "x1,y1 x2,y2 x3,y3 ..." (also handles space- or comma-mixed
    // separators)
    private static double[] parsePoints(String pointsAttr) {

        double scaling = UnitedLandsConfig.get().importSetting().scale();
        double xOffset = UnitedLandsConfig.get().importSetting().offsetX();
        double yOffset = UnitedLandsConfig.get().importSetting().offsetY();

        // Normalize: replace commas with spaces, collapse whitespace
        String cleaned = pointsAttr.trim().replaceAll("[,\\s]+", " ");
        String[] tokens = cleaned.isEmpty() ? new String[0] : cleaned.split(" ");

        double[] coords = new double[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            var scaled = Double.parseDouble(tokens[i]) * scaling;
            if (i % 2 == 0)
                scaled -= xOffset;
            else
                scaled -= yOffset;
            coords[i] = Math.floor(scaled);
        }
        return coords;
    }

    public static String extractStrokeColor(String style) {
        if (style == null || style.isEmpty()) {
            return null;
        }

        // Split into declarations on ';', then key/value on ':'
        for (String declaration : style.split(";")) {
            String[] parts = declaration.split(":", 2);
            if (parts.length != 2)
                continue;

            String property = parts[0].trim();
            String value = parts[1].trim();

            if (property.equals("stroke")) {
                if (value.equalsIgnoreCase("none")) {
                    return null;
                }
                return value;
            }
        }

        return null;
    }

    

}
