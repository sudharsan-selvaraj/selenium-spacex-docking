package com.testninja.spacex;

import org.openqa.selenium.By;

public class DockingControl {

    private String name;
    private String valueLabel;
    private By increaseButton;
    private By decreaseButton;

    public DockingControl(String name, String valueLabel, By increaseButton, By decreaseButton) {
        this.name = name;
        this.valueLabel = valueLabel;
        this.increaseButton = increaseButton;
        this.decreaseButton = decreaseButton;
    }


    public String getName() {
        return name;
    }

    public String getValueLabel() {
        return valueLabel;
    }

    public By getIncreaseButton() {
        return increaseButton;
    }

    public By getDecreaseButton() {
        return decreaseButton;
    }
}
