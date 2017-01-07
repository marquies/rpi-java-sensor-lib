package de.patricksteinert.rpisensorlib;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;

/*
 * Altitude, Pressure, Temperature
 */
public class AdafruitBMP280 {
    public final static int LITTLE_ENDIAN = 0;
    public final static int BIG_ENDIAN = 1;
    private final static int BMP280_ENDIANNESS = LITTLE_ENDIAN;
    /*
    Prompt> sudo i2cdetect -y 1
         0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f
    00:          -- -- -- -- -- -- -- -- -- -- -- -- --
    10: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    20: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    30: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    40: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    50: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    60: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    70: -- -- -- -- -- -- -- 77
     */
    // This next addresses is returned by "sudo i2cdetect -y 1", see above.
    public final static int BMP280_ADDRESS = 0x77;

    // Oversampling Setting
    public static final int BMP280_SAMPLE_0 = 0;
    public static final int BMP280_SAMPLE_1 = 1;
    public static final int BMP280_SAMPLE_2 = 2;
    public static final int BMP280_SAMPLE_4 = 3;
    public static final int BMP280_SAMPLE_8 = 4;
    public static final int BMP280_SAMPLE_16 = 7;

    // Power Modes
    private static final int BMP280_SLEEP_MODE = 0; // mode[1:0] bits 00;
    private static final int BMP280_FORCED_MODE = 2; // mode[1:0] bits 10 and 01;
    private static final int BMP280_NORMAL_MODE = 3; // mode[1:0] bits 11;

    // BMP280 Registers
    private static final int BMP280_CHIP_ID = 0xD0;  // R Chip Id 0x58 (8 bits)
    private static final int BMP280_RESET = 0xE0;  // R always 0x00 W 0xB6 to Force Reset (8 bits);
    private static final int BMP280_CONTROL = 0xF4;  // 7,6,5 osrsT 4,3,2 osrsP 1,0 mode(8 bits);
    private static final int BMP280_CONFIG = 0xF5;  // 7,6,5 t_sb 4,3,2 filter 0 spi3w_en;
    private static final int BMP280_PRESSURE_MSB = 0xF7;
    private static final int BMP280_PRESSURE_LSB = 0xF8;
    private static final int BMP280_PRESSURE_XLSB = 0xF9;
    private static final int BMP280_TEMP_MSB = 0xFA;
    private static final int BMP280_TEMP_LSB = 0xFB;
    private static final int BMP280_TEMP_XLSB = 0xFC;

    private static final int BMP280_DIG_T1 = 0x88;  // R   Unsigned Calibration data (16 bits);
    private static final int BMP280_DIG_T2 = 0x8A;  // R   Signed Calibration data (16 bits);
    private static final int BMP280_DIG_T3 = 0x8C;  // R   Signed Calibration data (16 bits);
    private static final int BMP280_DIG_P1 = 0x8E;  // R   Unsigned Calibration data (16 bits);
    private static final int BMP280_DIG_P2 = 0x90;  // R   Signed Calibration data (16 bits);
    private static final int BMP280_DIG_P3 = 0x92;  // R   Signed Calibration data (16 bits);
    private static final int BMP280_DIG_P4 = 0x94;  // R   Signed Calibration data (16 bits);
    private static final int BMP280_DIG_P5 = 0x96;  // R   Signed Calibration data (16 bits);
    private static final int BMP280_DIG_P6 = 0x98;  // R   Signed Calibration data (16 bits);
    private static final int BMP280_DIG_P7 = 0x9A;  // R   Signed Calibration data (16 bits);
    private static final int BMP280_DIG_P8 = 0x9C;  // R   Signed Calibration data (16 bits);
    private static final int BMP280_DIG_P9 = 0x9E;  // R   Signed Calibration data (16 bits);

    private static boolean verbose = false;

    private static final int osrsT = BMP280_SAMPLE_1;
    private static final int osrsP = BMP280_SAMPLE_4;

    private I2CBus bus;
    private I2CDevice bmp280;
    private int cal_t1 = 0;
    private int cal_t2 = 0;
    private int cal_t3 = 0;
    private int cal_p1 = 0;
    private int cal_p2 = 0;
    private int cal_p3 = 0;
    private int cal_p4 = 0;
    private int cal_p5 = 0;
    private int cal_p6 = 0;
    private int cal_p7 = 0;
    private int cal_p8 = 0;
    private int cal_p9 = 0;
    private int mode = BMP280_NORMAL_MODE;

    private static int standardSeaLevelPressure = 101325;

    public AdafruitBMP280() {
        this(BMP280_ADDRESS);
    }

    public AdafruitBMP280(int address) {
        try {
            // Get i2c bus
            bus = I2CFactory.getInstance(I2CBus.BUS_1); // Depends onthe RasPI version
            if (verbose)
                System.out.println("Connected to bus. OK.");

            // Get device itself
            bmp280 = bus.getDevice(address);
            if (verbose)
                System.out.println("Connected to device. OK.");

            try {
                this.readCalibrationData();
            } catch (Exception ex) {
                ex.printStackTrace();
            }


        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private int readU8(int reg) throws Exception {
        // "Read an unsigned byte from the I2C device"
        int result = 0;
        try {
            result = this.bmp280.read(reg);
            if (verbose)
                System.out.println("I2C: Device " + BMP280_ADDRESS + " returned " + result + " from reg " + reg);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    private int readS8(int reg) throws Exception {
        // "Reads a signed byte from the I2C device"
        int result = 0;
        try {
            result = this.bmp280.read(reg);
            if (result > 127)
                result -= 256;
            if (verbose)
                System.out.println("I2C: Device " + BMP280_ADDRESS + " returned " + result + " from reg " + reg);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    private int readU16(int register) throws Exception {
        int hi = this.readU8(register);
        int lo = this.readU8(register + 1);
        return (BMP280_ENDIANNESS == BIG_ENDIAN) ? (hi << 8) + lo : (lo << 8) + hi; // Big Endian
    }

    private int readS16(int register) throws Exception {
        int hi = 0, lo = 0;
        if (BMP280_ENDIANNESS == BIG_ENDIAN) {
            hi = this.readU8(register);
            lo = this.readU8(register + 1);
        } else {
            lo = this.readU8(register);
            hi = this.readU8(register + 1);
        }
        int value = (hi << 8) + lo;

        if (value > 32767) {
            value -= 65536;
        }

        return value;
    }

    public void readCalibrationData() throws Exception {
        // Reads the calibration data from the IC
        cal_t1 = readU16(BMP280_DIG_T1);
        cal_t2 = readS16(BMP280_DIG_T2); //-
        cal_t3 = readS16(BMP280_DIG_T3);

        cal_p1 = readU16(BMP280_DIG_P1);
        cal_p2 = readS16(BMP280_DIG_P2);
        cal_p3 = readS16(BMP280_DIG_P3);
        cal_p4 = readS16(BMP280_DIG_P4);
        cal_p5 = readS16(BMP280_DIG_P5);
        cal_p6 = readS16(BMP280_DIG_P6);
        cal_p7 = readS16(BMP280_DIG_P7);
        cal_p8 = readS16(BMP280_DIG_P8);
        cal_p9 = readS16(BMP280_DIG_P9);

        if (verbose)
            showCalibrationData();
    }

    private void showCalibrationData() {
        // Displays the calibration values for debugging purposes
        System.out.println("DBG: T1 = " + cal_t1 + " - hex = 0x" + Integer.toHexString(cal_t1));
        System.out.println("DBG: T2 = " + cal_t2 + " - hex = 0x" + Integer.toHexString(cal_t2));
        System.out.println("DBG: T3 = " + cal_t3 + " - hex = 0x" + Integer.toHexString(cal_t3));
        System.out.println("DBG: P1 = " + cal_p1 + " - hex = 0x" + Integer.toHexString(cal_p1));
        System.out.println("DBG: P2 = " + cal_p2 + " - hex = 0x" + Integer.toHexString(cal_p2));
        System.out.println("DBG: P3 = " + cal_p3 + " - hex = 0x" + Integer.toHexString(cal_p3));
        System.out.println("DBG: P4 = " + cal_p4 + " - hex = 0x" + Integer.toHexString(cal_p4));
        System.out.println("DBG: P5 = " + cal_p5 + " - hex = 0x" + Integer.toHexString(cal_p5));
        System.out.println("DBG: P6 = " + cal_p6 + " - hex = 0x" + Integer.toHexString(cal_p6));
        System.out.println("DBG: P7 = " + cal_p7 + " - hex = 0x" + Integer.toHexString(cal_p7));
        System.out.println("DBG: P8 = " + cal_p8 + " - hex = 0x" + Integer.toHexString(cal_p8));
        System.out.println("DBG: P9 = " + cal_p9 + " - hex = 0x" + Integer.toHexString(cal_p9));
    }

    /**
     * Reads the raw (uncompensated) temperature from the sensor
     *
     * @return raw temperature
     * @throws Exception
     */
    public int readRawTemperature() throws Exception {

        int tmp1 = (BMP280_NORMAL_MODE + (BMP280_SAMPLE_4 << 2) + (BMP280_SAMPLE_1 << 5));
        if (verbose)
            System.out.println("DBG: Raw Temp: " + Integer.toHexString(tmp1 & 0xFFFF) + ", " + tmp1);


        bmp280.write(BMP280_CONTROL, (byte) (BMP280_NORMAL_MODE + (BMP280_SAMPLE_4 << 2) + (BMP280_SAMPLE_1 << 5)));
        waitfor(5);  // Wait 5ms
        int msb = readU8(BMP280_TEMP_MSB);
        int lsb = readU8(BMP280_TEMP_LSB);
        int xlsb = readU8(BMP280_TEMP_XLSB);

        int raw = ((msb << 16) + (lsb << 8) + xlsb) >> (4);

        if (verbose)
            System.out.println("DBG: Raw Temp: " + (raw & 0xFFFF) + ", " + raw);

        return raw;
    }

    /**
     * Reads and compensated the temperature from the sensor.
     */
    public double readTemperature() throws Exception {
        int rawTemperature = readRawTemperature();
        return compensateTemperature(rawTemperature);
    }

    /**
     * Compensates the raw (uncompensated) temperature level from the sensor.
     *
     * @param rawTemperature
     * @return
     */
    public double compensateTemperature(int rawTemperature) {
        int var1 = (((rawTemperature >> 3) - (cal_t1 << 1)) * cal_t2) >> 11;
        if (verbose)
            System.out.println("DBG: var1 = " + var1);
        int var2 = (((rawTemperature >> 4) - cal_t1) * ((rawTemperature >> 4) - cal_t1) >> 12) * cal_t3 >> 14;
        if (verbose)
            System.out.println("DBG: var2 = " + var2);
        int t_fine = var1 + var2;
        if (verbose)
            System.out.println("DBG: t_fine = " + t_fine);
        return (((t_fine * 5 + 128) >> 8) / 100.0);

    }


    /**
     * Reads the raw (uncompensated) pressure from the sensor.
     *
     * @return
     * @throws Exception
     */
    public int readRawPressure() throws Exception {
        double var1, var2;

        //      bmp280.write(BMP280_CONTROL, (byte) (mode + (osrsP << 2) + (osrsT << 5)));
        bmp280.write(BMP280_CONTROL, (byte) (BMP280_NORMAL_MODE + (BMP280_SAMPLE_4 << 2) + (BMP280_SAMPLE_1 << 5)));
        waitfor(5);
        int msb = bmp280.read(BMP280_PRESSURE_MSB);
        int lsb = bmp280.read(BMP280_PRESSURE_LSB);
        int xlsb = bmp280.read(BMP280_PRESSURE_XLSB);

        int raw = ((msb << 16) + (lsb << 8) + xlsb) >> (4);
        return raw;
    }

    /**
     * Converts temperature from Celsius to Fahrenheit.
     *
     * @param temperature value in Celsius
     * @return temperature value in Fahrenheit
     */
    public static double convertCelsiusToFahrenheit(double temperature) {
        return temperature * 1.8 + 32;
    }

    /**
     * Reads the pressure from the sensor.
     *
     * @return the pressure in hPa.
     * @throws Exception if sensor communication fails.
     */
    public double readPressure() throws Exception {

//        //      bmp280.write(BMP280_CONTROL, (byte) (mode + (osrsP << 2) + (osrsT << 5)));
//        bmp280.write(BMP280_CONTROL, (byte) (BMP280_NORMAL_MODE + (BMP280_SAMPLE_4 << 2) + (BMP280_SAMPLE_1 << 5)));
//        waitfor(5);
//        int msb = bmp280.read(BMP280_PRESSURE_MSB);
//        int lsb = bmp280.read(BMP280_PRESSURE_LSB);
//        int xlsb = bmp280.read(BMP280_PRESSURE_XLSB);
//
//        int raw = ((msb << 16) + (lsb << 8) + xlsb) >> (4);

        int rawPressure = readRawPressure();
        long rawTemperature = readRawTemperature();


        if (verbose)
            System.out.println("DBG: raw pressure " + rawPressure);

        if (verbose)
            System.out.println("DBG: raw temperature " + rawTemperature);

        return compensatePressure(rawPressure, rawTemperature);
    }

    /**
     * @param rawPressure
     * @param rawTemperature
     * @return compensated pressure in Pascal
     */
    public double compensatePressure(double rawPressure, long rawTemperature) {
        double var1, var2;
        double tvar1 = (((rawTemperature >> 3) - (cal_t1 << 1)) * cal_t2) >> 11;
        double tvar2 = (((rawTemperature >> 4) - cal_t1) * ((rawTemperature >> 4) - cal_t1) >> 12) * cal_t3 >> 14;


        double t_fine = tvar1 + tvar2;

        if (verbose)
            System.out.println("DBG: t_fine " + t_fine);

        var1 = (t_fine / 2.0) - 64000.0;
        var2 = var1 * var1 * ((double) cal_p6) / 32768.0;

        var2 = var2 + var1 * ((double) cal_p5) * 2.0;
        var2 = (var2 / 4.0) + (((double) cal_p4) * 65536.0);
        var1 = (((double) cal_p3) * var1 * var1 / 524288.0 + ((double) cal_p2) * var1) / 524288.0;
        var1 = (1.0 + var1 / 32768.0) * ((double) cal_p1);
        double p = 1048576.0 - rawPressure;
        p = (p - (var2 / 4096.0)) * 6250.0 / var1;
        var1 = ((double) cal_p9) * p * p / 2147483648.0;
        var2 = p * ((double) cal_p8) / 32768.0;

        return (p + (var1 + var2 + ((double) cal_p7)) / 16.0);
    }

    public String readChipId() throws Exception {
        int raw = readU8(BMP280_CHIP_ID);
        CharSequence chipId = Integer.toHexString(raw & 0xFFFF);
        if (verbose)
            System.out.println("DBG: Chip Id 0x" + chipId + " (" + raw + ")");
        return "0x" + chipId;
    }


    /**
     * Calculates the altitude in meters.
     *
     * @return
     * @throws Exception
     */
    public double readAltitude() throws Exception {
        double altitude = 0.0;
        float pressure = (float) readPressure();
        altitude = 44330.0 * (1.0 - Math.pow(pressure / standardSeaLevelPressure, 0.1903));
        if (verbose)
            System.out.println("DBG: Altitude = " + altitude);
        return altitude;
    }


    public void setStandardSeaLevelPressure(int standardSeaLevelPressure)
    {
        this.standardSeaLevelPressure = standardSeaLevelPressure;
    }

    protected static void waitfor(long howMuch) {
        try {
            Thread.sleep(howMuch);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }


}