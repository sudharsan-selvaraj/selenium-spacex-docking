package com.testninja.spacex;

public class ScriptEntry {
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "/Users/sudharsan/Documents/Applications/chromedriver");

        DockingPage dockingPage = new DockingPage();
        dockingPage.initialize();

        dockingPage.startDockingProcess();
    }
}
