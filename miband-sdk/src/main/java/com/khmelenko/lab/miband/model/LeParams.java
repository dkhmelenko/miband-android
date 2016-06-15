package com.khmelenko.lab.miband.model;

/**
 * LE parameters
 *
 * @author Dmytro Khmelenko
 */
public final class LeParams {

    public int mConnIntMin;
    public int mConnIntMax;
    public int mConnInt;

    public int mLatency;
    public int mTimeout;
    public int mAdvInt;

    /**
     * Creates an instance of LeParams from byte data
     *
     * @param data Byte data
     * @return Instance of the object
     */
    public static LeParams fromByte(byte[] data) {
        LeParams params = new LeParams();

        params.mConnIntMax = 0xffff & (0xff & data[0] | (0xff & data[1]) << 8);
        params.mConnIntMax = 0xffff & (0xff & data[2] | (0xff & data[3]) << 8);
        params.mLatency = 0xffff & (0xff & data[4] | (0xff & data[5]) << 8);
        params.mTimeout = 0xffff & (0xff & data[6] | (0xff & data[7]) << 8);
        params.mConnInt = 0xffff & (0xff & data[8] | (0xff & data[9]) << 8);
        params.mAdvInt = 0xffff & (0xff & data[10] | (0xff & data[11]) << 8);

        params.mConnIntMin *= 1.25;
        params.mConnIntMax *= 1.25;
        params.mAdvInt *= 0.625;
        params.mTimeout *= 10;

        return params;
    }
}
