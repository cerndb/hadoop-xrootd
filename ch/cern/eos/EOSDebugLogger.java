package ch.cern.eos;

class EOSDebugLogger {

    private boolean debugEnabled;

    public EOSDebugLogger(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public print(String e) {
        if (this.debugEnabled) {
            System.out.println(e);
        }
    }

    public printStackTrace(Exception e) {
        if (this.debugEnabled) {
            e.printStackTrace();
        }
    }
}