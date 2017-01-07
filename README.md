# Raspberry PI Java Sensor Library

This is a library for Java Sensors. You can use it as Maven dependency.

To test it, you can use the examples.

## Using examples

Compile and package can be done on a seperate computer or on a Raspberry Pi itself.
```bash
 cd rpi-java-sensor-lib
 mvn package 
```

The package command generates a ```examples/target/examples-1.0-SNAPSHOT-jar-with-dependencies.jar```.

If you have packaged on the Raspberry Pi you can use it direct, otherwise you need to copy it to the Raspberry Pi.

Use this commands to test the Sensors:

```bash
sudo java -cp examples-1.0-SNAPSHOT-jar-with-dependencies.jar de.patricksteinert.rpisensorlib.examples.AdafruitBMP180Example
sudo java -cp examples-1.0-SNAPSHOT-jar-with-dependencies.jar de.patricksteinert.rpisensorlib.examples.AdafruitBMP280Example
sudo java -cp examples-1.0-SNAPSHOT-jar-with-dependencies.jar de.patricksteinert.rpisensorlib.examples.Sht15Example
sudo java -cp examples-1.0-SNAPSHOT-jar-with-dependencies.jar de.patricksteinert.rpisensorlib.examples.PollutionSensorADS1015Example
sudo java -cp examples-1.0-SNAPSHOT-jar-with-dependencies.jar de.patricksteinert.rpisensorlib.examples.Tsl45315Example
```
