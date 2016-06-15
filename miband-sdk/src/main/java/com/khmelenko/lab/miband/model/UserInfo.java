package com.khmelenko.lab.miband.model;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * User information
 *
 * @author Dmytro Khmelenko
 */
public final class UserInfo {

    private int mUid;
    private byte mGender;
    private byte mAge;
    private byte mHeight;
    private byte mWeight;
    private String mAlias = "";
    private byte mType;

    private UserInfo() {

    }

    public UserInfo(int uid, int gender, int age, int height, int weight, String alias, int type) {
        mUid = uid;
        mGender = (byte) gender;
        mAge = (byte) age;
        mHeight = (byte) (height & 0xFF);
        mWeight = (byte) weight;
        mAlias = alias;
        mType = (byte) type;
    }

    /**
     * Creates an instance of user info from byte data
     *
     * @param data Byte data
     * @return User info object or null, if data are invalid
     */
    public static UserInfo fromByteData(byte[] data) {
        if (data.length < 20) {
            return null;
        }
        UserInfo info = new UserInfo();

        info.mUid = data[3] << 24 | (data[2] & 0xFF) << 16 | (data[1] & 0xFF) << 8 | (data[0] & 0xFF);
        info.mGender = data[4];
        info.mAge = data[5];
        info.mHeight = data[6];
        info.mWeight = data[7];
        info.mType = data[8];
        try {
            info.mAlias = new String(data, 9, 8, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            info.mAlias = "";
        }

        return info;
    }

    public byte[] getBytes(String mBTAddress) {
        byte[] aliasBytes;
        try {
            aliasBytes = this.mAlias.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            aliasBytes = new byte[0];
        }
        ByteBuffer bf = ByteBuffer.allocate(20);
        bf.put((byte) (mUid & 0xff));
        bf.put((byte) (mUid >> 8 & 0xff));
        bf.put((byte) (mUid >> 16 & 0xff));
        bf.put((byte) (mUid >> 24 & 0xff));
        bf.put(this.mGender);
        bf.put(this.mAge);
        bf.put(this.mHeight);
        bf.put(this.mWeight);
        bf.put(this.mType);
        bf.put((byte) 4);
        bf.put((byte) 0);

        if (aliasBytes.length <= 8) {
            bf.put(aliasBytes);
            bf.put(new byte[8 - aliasBytes.length]);
        } else {
            bf.put(aliasBytes, 0, 8);
        }

        byte[] crcSequence = new byte[19];
        for (int u = 0; u < crcSequence.length; u++)
            crcSequence[u] = bf.array()[u];

        byte crcb = (byte) ((getCRC8(crcSequence) ^ Integer.parseInt(mBTAddress.substring(mBTAddress.length() - 2), 16)) & 0xff);
        bf.put(crcb);
        return bf.array();
    }

    private int getCRC8(byte[] seq) {
        int len = seq.length;
        int i = 0;
        byte crc = 0x00;

        while (len-- > 0) {
            byte extract = seq[i++];
            for (byte tempI = 8; tempI != 0; tempI--) {
                byte sum = (byte) ((crc & 0xff) ^ (extract & 0xff));
                sum = (byte) ((sum & 0xff) & 0x01);
                crc = (byte) ((crc & 0xff) >>> 1);
                if (sum != 0) {
                    crc = (byte) ((crc & 0xff) ^ 0x8c);
                }
                extract = (byte) ((extract & 0xff) >>> 1);
            }
        }
        return (crc & 0xff);
    }

    public String toString() {
        return "uid:" + mUid
                + ",gender:" + mGender
                + ",age:" + mAge
                + ",height:" + getHeight()
                + ",weight:" + getWeight()
                + ",alias:" + mAlias
                + ",type:" + mType;
    }

    /**
     * Gets UID
     *
     * @return UID
     */
    public int getUid() {
        return mUid;
    }

    /**
     * Gets gender
     *
     * @return Gender
     */
    public byte getGender() {
        return mGender;
    }

    /**
     * Gets age
     *
     * @return Age
     */
    public byte getAge() {
        return mAge;
    }

    /**
     * Gets height in cm
     *
     * @return Height in cm
     */
    public int getHeight() {
        return (mHeight & 0xFF);
    }

    /**
     * Gets weight in kg
     *
     * @return Weight in kg
     */
    public int getWeight() {
        return mWeight & 0xFF;
    }

    /**
     * Gets alias
     *
     * @return Alias
     */
    public String getAlias() {
        return mAlias;
    }

    /**
     * Gets type
     *
     * @return Type
     */
    public byte getType() {
        return mType;
    }
}
