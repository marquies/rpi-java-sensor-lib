package de.patricksteinert.rpisensorlib.examples;

import de.patricksteinert.rpisensorlib.AdafruitBMP180;

/**
 * Created by Patrick Steinert on 07.01.17.
 */
public class AdafruitBMP180Example {
    public static void main(String[] args) throws Exception {
        AdafruitBMP180 adafruitBMP180 = new AdafruitBMP180();

        double temperature = adafruitBMP180.readTemperature();

        double pressure = adafruitBMP180.readPressure();

        // Setting pressure at sealevel in my region
        adafruitBMP180.setStandardSeaLevelPressure(101900);

        System.out.printf("Pressure    : %.2f hPa %n", pressure / 100);
        System.out.printf("Temperature : %.2f °C %n", temperature);
        System.out.printf("Temperature : %.2f °F %n", AdafruitBMP180.convertCelsiusToFahrenheit(temperature));
        System.out.printf("Altitude    : %.2f m %n", adafruitBMP180.readAltitude());
    }

}
