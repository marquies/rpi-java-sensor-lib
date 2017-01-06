package de.patricksteinert.rpisensorlib.examples;

import de.patricksteinert.rpisensorlib.SHT15;

/**
 * Created by Patrick Steinert on 06.01.17.
 */
public class Sht15Example {

    public static void main(String[] args) throws InterruptedException {
        SHT15 sht15 = new SHT15();
        double humidity = sht15.readHumidity();
        double temperature = sht15.readTemperature();

        System.out.println("SHT15 (humidity & temperature) Sensor read: " +
                "Humidity: '" + humidity + "' %; Temperature: '" + temperature + "' °C");
    }

}
