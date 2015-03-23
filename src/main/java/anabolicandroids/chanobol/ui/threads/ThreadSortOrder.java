package anabolicandroids.chanobol.ui.threads;

public enum ThreadSortOrder {
    Bump("bump"), Replies("replies"), Images("images"), Date("date");

    public final String string;
    ThreadSortOrder(String string) {
        this.string = string;
    }
}
