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

    /**
     * .OAM
     */
    private final static int CS2 = 16;

    /**
     * .Signaling
     */
    private final static int CS3 = 24;

    /**
     * .Realtime
     */
    private final static int CS4 = 32;

    /**
     * .Broadcast video
     */
    private final static int CS5 = 40;

    /**
     * .Network Control
     */
    private final static int CS6 = 48;

    /**
     * .UnSpecified
     */
    private final static int CS7 = 56;

    private final static int ProbabilityLow = 2;
    private final static int ProbabilityMedium = 4;
    private final static int ProbabilityHigh = 6;


    private static int getDSCP(int class_selector, int probability)
    {
        return class_selector + probability;
    }

    public static int getAudioDSCP()
    {
        return getDSCP(CS4, ProbabilityHigh);
    }
    public static int getVideoDSCP()
    {
        return getDSCP(CS4, ProbabilityHigh);
    }
    public static int getSipDSCP()
    {
        return getDSCP(CS3, ProbabilityMedium);
    }

    public static int getDefaultDSCP()
    {
        return CS0;
    }
}
