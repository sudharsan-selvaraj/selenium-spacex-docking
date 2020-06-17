package com.testninja.spacex;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.HashMap;
import java.util.Map;

public class DockingPage {

    private WebDriver driver;
    private static Map<String, DockingControl> controls;
    private static final By hubLocator = By.id("hud");
    private static final By beginButton = By.id("begin-button");
    private static final String url = "https://iss-sim.spacex.com/";

    static {
        controls = new HashMap<String, DockingControl>() {{
            put("yaw", new DockingControl("yaw", "error", By.cssSelector("#yaw-left-button"), By.cssSelector("#yaw-right-button")));
            put("pitch", new DockingControl("pitch", "error", By.cssSelector("#pitch-up-button"), By.cssSelector("#pitch-down-button")));
            put("roll", new DockingControl("roll", "error", By.cssSelector("#roll-left-button"), By.cssSelector("#roll-right-button")));
            put("x-range", new DockingControl("x-range", "distance", By.cssSelector("#translate-forward-button"), By.cssSelector("#translate-backward-button")));
            put("y-range", new DockingControl("y-range", "distance", By.cssSelector("#translate-right-button"), By.cssSelector("#translate-left-button")));
            put("z-range", new DockingControl("z-range", "distance", By.cssSelector("#translate-up-button"), By.cssSelector("#translate-down-button")));
        }};
    }

    void initialize() {
        driver = new ChromeDriver();
        driver.get(url);
        clickBegin();
    }

    void startDockingProcess() {

        DockingControl xAxis = controls.get("x-range");
        DockingControl yAxis = controls.get("y-range");
        DockingControl zAxis = controls.get("z-range");

        /* Equalize Pitch, Yaw and Roll first */
        equalizeControls();

        /* Equalize Y-axis and Z-axis  */
        equalizeAxisControl(yAxis);
        equalizeAxisControl(zAxis);


        /* Loop to move the aircraft forward. Loop will be executed until the distance between space craft and space station 0.4m*/
        Float xValue = getValue(xAxis);
        while (xValue > 0.4) {
            xValue = getValue(xAxis);
            Float rate = getValue("rate", "rate");

            /* if the distance is greater than 100 and the moving rate is sloe, then increase the velocity */
            if (xValue > 100) {
                while (rate >= -1.50) {
                    driver.findElement(xAxis.getIncreaseButton()).click();
                    rate = getValue("rate", "rate");
                }
            } else if (xValue < 35) { /* When the distance is reduced, slow down the speed to avoid collision with the space station */
                while (rate <= -0.1) {
                    driver.findElement(xAxis.getDecreaseButton()).click();
                    rate = getValue("rate", "rate");
                }
            }

            /* When the space craft is moving forward, monitor Y-axis and Z-axis and equalize if required  */
            equalizeAxisControl(yAxis);
            equalizeAxisControl(zAxis);

            /* When space craft is near ISS, add a little thrust to move the craft by maintaining the maximum speed */
            if (xValue < 30 && xValue > 1 && rate > -0.09) {
                driver.findElement(xAxis.getIncreaseButton()).click();
            }
        }

        new WebDriverWait(driver, 60).until(ExpectedConditions.presenceOfElementLocated(By.id("success")));
        System.out.println("Successfully completed the docking process..");
    }

    private void clickBegin() {
        try {
            WebElement beginButtonElement = new WebDriverWait(driver, 60).until(ExpectedConditions.elementToBeClickable(beginButton));
            ((JavascriptExecutor) driver).executeScript("document.querySelector('#begin-button').style.width='100%'");
            Thread.sleep(1000);
            beginButtonElement.click();
            new WebDriverWait(driver, 60).until(ExpectedConditions.visibilityOf(driver.findElement(hubLocator)));
            Thread.sleep(7000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Boolean isEqualized(DockingControl control) {
        return Math.abs(getValue(control)) == 0;
    }

    private Float getValue(String controlName, String labelName) {
        String value = driver.findElement(By.id(controlName)).findElement(By.cssSelector("." + labelName)).getText();
        value = value.replace("°", "").replace("/s", "").replace("m", "");
        return Float.parseFloat(value);
    }

    private Float getValue(DockingControl control) {
        return getValue(control.getName(), control.getValueLabel());
    }

    private void equalizeRightControl(DockingControl control) {
        System.out.println("Equalizing " + control.getName() + " ..");
        while (!isEqualized(control)) {
            Float value = getValue(control);
            Boolean increase = false;
            int noOfTime = 0;
            if (value < 0) {
                increase = true;
            }
            float absValue = Math.abs(value);
            if (absValue > 10) {
                noOfTime = 10;
            } else if (absValue > 1) {
                noOfTime = 5;
            } else {
                noOfTime = 1;
            }
            pressButton(control, increase, noOfTime, Math.round(absValue / (noOfTime / 0.1)));
        }
    }

    private void equalizeAxisControl(DockingControl control) {
        System.out.println("Equalizing " + control.getName() + " ..");
        while (!isEqualized(control)) {
            Float value = getValue(control);
            String precisionStatus = driver.findElement(By.cssSelector("#precision-translation-status")).getAttribute("class").replace("noselect", "");
            Boolean increase = false;
            String requiredPrecisionStatus = "small";
            int noOfTime = 0;
            if (value < 0) {
                increase = true;
            }

            Float absValue = Math.abs(value);
            if (absValue > 10) {
                requiredPrecisionStatus = "large";
                noOfTime = 15;
            } else if (absValue > 1) {
                requiredPrecisionStatus = "";
                noOfTime = 10;
            } else {
                requiredPrecisionStatus = "";
                noOfTime = 2;
            }

            if (!precisionStatus.equalsIgnoreCase(requiredPrecisionStatus)) {
                driver.findElement(By.cssSelector("#toggle-translation")).click();
            }

            pressButton(control, increase, noOfTime, Math.round(absValue * (noOfTime * 0.1)));
        }
    }

    private void equalizeControls() {
        while (!isEqualized(controls.get("pitch")) || !isEqualized(controls.get("yaw"))) {
            equalizeRightControl(controls.get("pitch"));
            equalizeRightControl(controls.get("yaw"));
        }

        equalizeRightControl(controls.get("roll"));
    }


    private void pressButton(DockingControl control, Boolean increment, int clickCount, long sleepTime) {
        try {
            By firstButtonToClick;
            By secondButtonToClick;

            if (increment) {
                firstButtonToClick = control.getIncreaseButton();
                secondButtonToClick = control.getDecreaseButton();
            } else {
                firstButtonToClick = control.getDecreaseButton();
                secondButtonToClick = control.getIncreaseButton();
            }

            for (int i = 0; i < clickCount; i++) {
                driver.findElement(firstButtonToClick).click();
            }
            Thread.sleep(sleepTime);

            for (int i = 0; i < clickCount; i++) {
                driver.findElement(secondButtonToClick).click();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
