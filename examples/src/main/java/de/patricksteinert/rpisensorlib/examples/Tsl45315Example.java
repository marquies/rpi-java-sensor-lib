package de.patricksteinert.rpisensorlib.examples;

import de.patricksteinert.rpisensorlib.TSL45315;

import java.io.IOException;

/**
 * Created by Patrick Steinert on 05.01.17.
 */
public class Tsl45315Example {

    public static void main(String[] args) throws IOException {
        TSL45315 tsl45315 = new TSL45315();
        double value = tsl45315.readValue();
        System.out.println("TSL45315 (light) Sensor read '" + value + "' Lux");
    }
}
