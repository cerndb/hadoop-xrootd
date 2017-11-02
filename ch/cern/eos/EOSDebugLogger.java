package ch.cern.eos;

public class EOSDebugLogger {

    private boolean debugEnabled;

    public EOSDebugLogger(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public void print(String e) {
        if (this.debugEnabled) {
            System.out.println(e);
        }
    }

    public void printStackTrace(Exception e) {
        if (this.debugEnabled) {
            e.printStackTrace();
        }
    }

    public boolean isDebugEnabled() {
        return this.debugEnabled;
    }
}