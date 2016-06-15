package com.khmelenko.lab.miband.model;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Battery info
 *
 * @author Dmytro Khmelenko
 */
public final class BatteryInfo {

    enum Status {
        UNKNOWN, LOW, FULL, CHARGING, NOT_CHARGING;

        public static Status fromByte(byte b) {
            switch (b) {
                case 1:
                    return LOW;
                case 2:
                    return CHARGING;
                case 3:
                    return FULL;
                case 4:
                    return NOT_CHARGING;

                default:
                    return UNKNOWN;
            }
        }
    }

    private int mLevel;
    private int mCycles;
    private Status mStatus;
    private Calendar mLastChargedDate;

    private BatteryInfo() {

    }

    /**
     * Creates an instance of the battery info from byte data
     *
     * @param data Byte data
     * @return Battery info or null, if data are invalid
     */
    public static BatteryInfo fromByteData(byte[] data) {
        if (data.length < 10) {
            return null;
        }
        BatteryInfo info = new BatteryInfo();

        info.mLevel = data[0];
        info.mStatus = Status.fromByte(data[9]);
        info.mCycles = 0xffff & (0xff & data[7] | (0xff & data[8]) << 8);
        info.mLastChargedDate = Calendar.getInstance();

        info.mLastChargedDate.set(Calendar.YEAR, data[1] + 2000);
        info.mLastChargedDate.set(Calendar.MONTH, data[2]);
        info.mLastChargedDate.set(Calendar.DATE, data[3]);

        info.mLastChargedDate.set(Calendar.HOUR_OF_DAY, data[4]);
        info.mLastChargedDate.set(Calendar.MINUTE, data[5]);
        info.mLastChargedDate.set(Calendar.SECOND, data[6]);

        return info;
    }

    public String toString() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS", Locale.getDefault());
        String formattedDate = formatter.format(getLastChargedDate().getTime());
        return "cycles:" + getCycles()
                + ",level:" + getLevel()
                + ",status:" + getStatus()
                + ",last:" + formattedDate;
    }

    /**
     * Gets battery level
     *
     * @return Battery level
     */
    public int getLevel() {
        return mLevel;
    }

    /**
     * Gets cycles
     *
     * @return Cycles
     */
    public int getCycles() {
        return mCycles;
    }

    /**
     * Gets battery status
     *
     * @return Battery status
     */
    public Status getStatus() {
        return mStatus;
    }

    /**
     * Gets last charging date
     *
     * @return Last charging date
     */
    public Calendar getLastChargedDate() {
        return mLastChargedDate;
    }

}
