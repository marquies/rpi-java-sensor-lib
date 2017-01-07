package de.patricksteinert.rpisensorlib.examples;

import de.patricksteinert.rpisensorlib.AdafruitBMP280;

/**
 * Created by Patrick Steinert on 07.01.17.
 */
public class AdafruitBMP280Example {
    public static void main(String[] args) throws Exception {
        AdafruitBMP280 adafruitBMP280 = new AdafruitBMP280();
        String chipId = adafruitBMP280.readChipId();

        double temperature = adafruitBMP280.readTemperature();

        double pressure = adafruitBMP280.readPressure();

        // Setting pressure at sealevel in my region
        adafruitBMP280.setStandardSeaLevelPressure(101900);

        System.out.printf("Chip ID     : %s %n", chipId);
        System.out.printf("Pressure    : %.2f hPa %n", pressure / 100);
        System.out.printf("Temperature : %.2f °C %n", temperature);
        System.out.printf("Temperature : %.2f °F %n", AdafruitBMP280.convertCelsiusToFahrenheit(temperature));
        System.out.printf("Altitude    : %.2f m %n", adafruitBMP280.readAltitude());
    }

}
