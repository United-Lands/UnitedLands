package org.unitedlands.unitedlands.classes;

import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig.GeopolAttributeDefaults;

public class GeopolAttribute {

    private String display;
    private double currentValue;
    private double minValue;
    private double maxValue;
    private double dailyChange;

    public GeopolAttribute() {
    }

    public GeopolAttribute(String display, double currentValue, double minValue, double maxValue, double dailyChange) {
        this.display = display;
        this.currentValue = currentValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.dailyChange = dailyChange;
    }

    public GeopolAttribute(GeopolAttributeDefaults defaults) {
        this.display = defaults.display();
        this.currentValue = defaults.currentValue();
        this.minValue = defaults.minValue();
        this.maxValue = defaults.maxValue();
        this.dailyChange = defaults.dailyChange();
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public double getMinValue() {
        return minValue;
    }

    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    public double getDailyChange() {
        return dailyChange;
    }

    public void setDailyChange(double dailaChange) {
        this.dailyChange = dailaChange;
    }

    public GeopolAttribute clone() {
        return new GeopolAttribute(display, currentValue, minValue, maxValue, dailyChange);
    }

}
