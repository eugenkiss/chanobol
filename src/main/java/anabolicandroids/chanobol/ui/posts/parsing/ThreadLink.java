package anabolicandroids.chanobol.ui.posts.parsing;

public class ThreadLink {
    public String board;
    public String threadNumber;
    public String postNumber;

    public ThreadLink(String board, String threadNumber, String postNumber) {
        this.board = board;
        this.threadNumber = threadNumber;
        this.postNumber = postNumber;
    }
}
