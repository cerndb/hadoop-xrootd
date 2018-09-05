package ch.cern.eos;

public class XRootDInstrumentation {

    private static Long timeElapsedReadOps = 0L;

    public void incrementTimeElapsedReadOps(Long incrementTime) {
        timeElapsedReadOps += incrementTime;
    }

    // This is a public and static method so it can be read from clients calling
    // ch.cern.eos.XRootDInstrumentation.getTimeElapsedReadOps()
    public static long getTimeElapsedReadOps() {
        return timeElapsedReadOps;
    }

}

