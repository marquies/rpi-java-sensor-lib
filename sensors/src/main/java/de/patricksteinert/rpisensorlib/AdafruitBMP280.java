package de.patricksteinert.rpisensorlib;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;

import static de.patricksteinert.rpisensorlib.AdafruitBMP180.BMP180_CAL_AC1;
import static de.patricksteinert.rpisensorlib.AdafruitBMP180.BMP180_CAL_AC2;
import static javafx.scene.input.KeyCode.T;

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
    private static final int BMP280_CONTROL = 0xF4;  // 7,6,5 osrs_t 4,3,2 osrs_p 1,0 mode(8 bits);
    private static final int BMP280_CONFIG = 0xF5;  // 7,6,5 t_sb 4,3,2 filter 0 spi3w_en;
    private static final int BMP280_PRESSURE_MSB = 0xF7;
    private static final int BMP280_PRESSURE_LSB = 0xF8;
    private static final int BMP280_PRESSURE_XLSB = 0xF9;
    private static final int BMP280_TEMP_MSB = 0xFA;
    private static final int BMP280_TEMP_LSB = 0xFB;
    private static final int BMP280_TEMP_XLSB = 0xFC;

    //public final static int BMP180_TEMPDATA          = 0xF6;
    //public final static int BMP180_PRESSUREDATA      = 0xF6;
    //public final static int BMP180_READTEMPCMD       = 0x2E;
    //public final static int BMP180_READPRESSURECMD   = 0x34;

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

//    private int cal_AC1 = 0;
//    private int cal_AC2 = 0;
//    private int cal_AC3 = 0;
//    private int cal_AC4 = 0;
//    private int cal_AC5 = 0;
//    private int cal_AC6 = 0;
//    private int cal_B1  = 0;
//    private int cal_B2  = 0;
//    private int cal_MB  = 0;
//    private int cal_MC  = 0;
//    private int cal_MD  = 0;

    private static boolean verbose = true;

    private I2CBus bus;
    private I2CDevice bmp280;
    private short cal_t1 = 0;
    private short cal_t2 = 0;
    private short cal_t3 = 0;
    private short cal_p1 = 0;
    private short cal_p2 = 0;
    private short cal_p3 = 0;
    private short cal_p4 = 0;
    private short cal_p5 = 0;
    private short cal_p6 = 0;
    private short cal_p7 = 0;
    private short cal_p8 = 0;
    private short cal_p9 = 0;

    //    private int mode = BMP180_STANDARD;

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
            hi = this.readS8(register);
            lo = this.readU8(register + 1);
        } else {
            lo = this.readS8(register);
            hi = this.readU8(register + 1);
        }
        return (hi << 8) + lo;
    }

    public void readCalibrationData() throws Exception {
        // Reads the calibration data from the IC
        cal_t1 = (short) readU16(BMP280_DIG_T1);
        cal_t2 = (short) readS16(BMP280_DIG_T2); //-
        cal_t3 = (short) readS16(BMP280_DIG_T3);

        cal_p1 = (short) readU16(BMP280_DIG_P1);
        cal_p2 = (short) readS16(BMP280_DIG_P2);
        cal_p3 = (short) readS16(BMP280_DIG_P3);
        cal_p4 = (short) readS16(BMP280_DIG_P4);
        cal_p5 = (short) readS16(BMP280_DIG_P5);
        cal_p6 = (short) readS16(BMP280_DIG_P6);
        cal_p7 = (short) readS16(BMP280_DIG_P7);
        cal_p8 = (short) readS16(BMP280_DIG_P8);
        cal_p9 = (short) readS16(BMP280_DIG_P9);

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
    public int readRawTemp() throws Exception {

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
//        bmp180.write(BMP180_CONTROL, (byte)BMP180_READTEMPCMD);
//        waitfor(5);  // Wait 5ms
//        int raw = readU16(BMP180_TEMPDATA);
//        if (verbose)
//            System.out.println("DBG: Raw Temp: " + (raw & 0xFFFF) + ", " + raw);
//        return raw;
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
            System.out.println("var2 = " + var2);
        int t_fine = var1 + var2;
        if (verbose)
            System.out.println("t_fine = " + t_fine);
        double t = (((t_fine * 5 + 128) >> 8) / 100.0);
        if (verbose)
            System.out.println("T = " +  t);
        return t;
    }

//    public int readRawPressure() throws Exception
//    {
//        // Reads the raw (uncompensated) pressure level from the sensor
//        bmp280.write(BMP180_CONTROL, (byte)(BMP180_READPRESSURECMD + (this.mode << 6)));
//        if (this.mode == BMP180_ULTRALOWPOWER)
//            waitfor(5);
//        else if (this.mode == BMP180_HIGHRES)
//            waitfor(14);
//        else if (this.mode == BMP180_ULTRAHIGHRES)
//            waitfor(26);
//        else
//            waitfor(8);
//        int msb = bmp280.read(BMP180_PRESSUREDATA);
//        int lsb = bmp280.read(BMP180_PRESSUREDATA + 1);
//        int xlsb = bmp280.read(BMP180_PRESSUREDATA + 2);
//        int raw = ((msb << 16) + (lsb << 8) + xlsb) >> (8 - this.mode);
//        if (verbose)
//            System.out.println("DBG: Raw Pressure: " + (raw & 0xFFFF) + ", " + raw);
//        return raw;
//    }

//    public float readTemperature() throws Exception
//    {
//        // Gets the compensated temperature in degrees celcius
//        int UT = 0;
//        int X1 = 0;
//        int X2 = 0;
//        int B5 = 0;
//        float temp = 0.0f;
//
//        // Read raw temp before aligning it with the calibration values
//        UT = this.readRawTemp();
//        X1 = ((UT - this.cal_AC6) * this.cal_AC5) >> 15;
//        X2 = (this.cal_MC << 11) / (X1 + this.cal_MD);
//        B5 = X1 + X2;
//        temp = ((B5 + 8) >> 4) / 10.0f;
//        if (verbose)
//            System.out.println("DBG: Calibrated temperature = " + temp + " C");
//        return temp;
//    }

//    public float readPressure() throws Exception
//    {
//        // Gets the compensated pressure in pascal
//        int UT = 0;
//        int UP = 0;
//        int B3 = 0;
//        int B5 = 0;
//        int B6 = 0;
//        int X1 = 0;
//        int X2 = 0;
//        int X3 = 0;
//        int p = 0;
//        int B4 = 0;
//        int B7 = 0;
//
//        UT = this.readRawTemp();
//        UP = this.readRawPressure();
//
//        // You can use the datasheet values to test the conversion results
//        // boolean dsValues = true;
//        boolean dsValues = false;
//
//        if (dsValues)
//        {
//            UT = 27898;
//            UP = 23843;
//            this.cal_AC6 = 23153;
//            this.cal_AC5 = 32757;
//            this.cal_MB = -32768;
//            this.cal_MC = -8711;
//            this.cal_MD = 2868;
//            this.cal_B1 = 6190;
//            this.cal_B2 = 4;
//            this.cal_AC3 = -14383;
//            this.cal_AC2 = -72;
//            this.cal_AC1 = 408;
//            this.cal_AC4 = 32741;
//            this.mode = BMP180_ULTRALOWPOWER;
//            if (verbose)
//                this.showCalibrationData();
//        }
//        // True Temperature Calculations
//        X1 = (int)((UT - this.cal_AC6) * this.cal_AC5) >> 15;
//        X2 = (this.cal_MC << 11) / (X1 + this.cal_MD);
//        B5 = X1 + X2;
//        if (verbose)
//        {
//            System.out.println("DBG: X1 = " + X1);
//            System.out.println("DBG: X2 = " + X2);
//            System.out.println("DBG: B5 = " + B5);
//            System.out.println("DBG: True Temperature = " + (((B5 + 8) >> 4) / 10.0)  + " C");
//        }
//        // Pressure Calculations
//        B6 = B5 - 4000;
//        X1 = (this.cal_B2 * (B6 * B6) >> 12) >> 11;
//        X2 = (this.cal_AC2 * B6) >> 11;
//        X3 = X1 + X2;
//        B3 = (((this.cal_AC1 * 4 + X3) << this.mode) + 2) / 4;
//        if (verbose)
//        {
//            System.out.println("DBG: B6 = " + B6);
//            System.out.println("DBG: X1 = " + X1);
//            System.out.println("DBG: X2 = " + X2);
//            System.out.println("DBG: X3 = " + X3);
//            System.out.println("DBG: B3 = " + B3);
//        }
//        X1 = (this.cal_AC3 * B6) >> 13;
//        X2 = (this.cal_B1 * ((B6 * B6) >> 12)) >> 16;
//        X3 = ((X1 + X2) + 2) >> 2;
//        B4 = (this.cal_AC4 * (X3 + 32768)) >> 15;
//        B7 = (UP - B3) * (50000 >> this.mode);
//        if (verbose)
//        {
//            System.out.println("DBG: X1 = " + X1);
//            System.out.println("DBG: X2 = " + X2);
//            System.out.println("DBG: X3 = " + X3);
//            System.out.println("DBG: B4 = " + B4);
//            System.out.println("DBG: B7 = " + B7);
//        }
//        if (B7 < 0x80000000)
//            p = (B7 * 2) / B4;
//        else
//            p = (B7 / B4) * 2;
//
//        if (verbose)
//            System.out.println("DBG: X1 = " + X1);
//
//        X1 = (p >> 8) * (p >> 8);
//        X1 = (X1 * 3038) >> 16;
//        X2 = (-7357 * p) >> 16;
//        if (verbose)
//        {
//            System.out.println("DBG: p  = " + p);
//            System.out.println("DBG: X1 = " + X1);
//            System.out.println("DBG: X2 = " + X2);
//        }
//        p = p + ((X1 + X2 + 3791) >> 4);
//        if (verbose)
//            System.out.println("DBG: Pressure = " + p + " Pa");
//
//        return p;
//    }

    public String readChipId() throws Exception {
        int raw = readU8(BMP280_CHIP_ID);
        CharSequence chipId = Integer.toHexString(raw & 0xFFFF);
        if (verbose)
            System.out.println("DBG: Chip Id 0x" + chipId + " (" + raw + ")");
        return "0x" + chipId;
    }

    private int standardSeaLevelPressure = 101325;

    public void setStandardSeaLevelPressure(int standardSeaLevelPressure) {
        this.standardSeaLevelPressure = standardSeaLevelPressure;
    }

//    public double readAltitude() throws Exception
//    {
//        // "Calculates the altitude in meters"
//        double altitude = 0.0;
//        float pressure = readPressure();
//        altitude = 44330.0 * (1.0 - Math.pow(pressure / standardSeaLevelPressure, 0.1903));
//        if (verbose)
//            System.out.println("DBG: Altitude = " + altitude);
//        return altitude;
//    }

    protected static void waitfor(long howMuch) {
        try {
            Thread.sleep(howMuch);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }


}