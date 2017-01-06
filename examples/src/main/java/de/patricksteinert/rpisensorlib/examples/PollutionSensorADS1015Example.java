package de.patricksteinert.rpisensorlib.examples;

import de.patricksteinert.rpisensorlib.PollutionSensorADS1015;

import java.io.IOException;

/**
 * Created by Patrick Steinert on 06.01.17.
 */
public class PollutionSensorADS1015Example {

    public static void main(String[] args) throws IOException, InterruptedException {
        PollutionSensorADS1015 pollutionSensorADS1015 = new PollutionSensorADS1015();
        int value = pollutionSensorADS1015.readValue();

        System.out.println("Seed Studios AirQuality Sensor read '" + value + "' ppm");
    }

}
