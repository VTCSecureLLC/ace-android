package org.linphone.setup;

/**
 * Created by accontech-samson on 2/25/16.
 */
public class DSCPClassSelector {

    /**
     * .Default
     */
    private final static int CS0 = 0;

    /**
     * .Scavenger
     */
    private final static int CS1 = 8;

    private final static int AF11 = 10;
    private final static int AF12 = 12;
    private final static int AF13 = 14;

    /**
     * .OAM
     */
    private final static int CS2 = 16;

    private final static int AF21 = 18;
    private final static int AF22 = 20;
    private final static int AF23 = 22;

    /**
     * .Signaling
     */
    private final static int CS3 = 24;

    private final static int AF31 = 26;
    private final static int AF32 = 28;
    private final static int AF33 = 30;

    /**
     * .Realtime
     */
    private final static int CS4 = 32;

    private final static int AF41 = 36;
    private final static int AF42 = 38;
    private final static int AF43 = 40;

    /**
     * .Broadcast video
     */
    private final static int CS5 = 40;

    private final static int EF = 46;

    /**
     * .Network Control
     */
    private final static int CS6 = 48;

    /**
     * .UnSpecified
     */
    private final static int CS7 = 56;

    private final static int ProbabilityNone = 0;
    private final static int ProbabilityLow = 2;
    private final static int ProbabilityMedium = 4;
    private final static int ProbabilityHigh = 6;


    private static int getDSCP(int class_selector, int probability)
    {
        return class_selector + probability;
    }

    public static int getAudioDSCP()
    {
        return EF; //getDSCP(CS5, ProbabilityHigh);
    }
    public static int getVideoDSCP()
    {
        return EF; //getDSCP(CS5, ProbabilityHigh);
    }
    public static int getSipDSCP()
    {
        return CS3; //getDSCP(CS3, ProbabilityNone);
    }

    public static int getDefaultDSCP()
    {
        return CS0;
    }
}
