package anabolicandroids.chanobol.ui.media;


import org.parceler.Parcel;

import anabolicandroids.chanobol.api.data.Post;

@Parcel
public class MediaPointer {
    public String id;
    public String ext;
    public int w;
    public int h;
    public Post post;

    @SuppressWarnings("UnusedDeclaration") public MediaPointer() {}

    public MediaPointer(Post post, String imageId, String imageExtension, int width, int height) {
        id = imageId;
        ext = imageExtension;
        w = width;
        h = height;
        this.post = post;
    }

    public static MediaPointer from(Post post) {
        return new MediaPointer(post, post.mediaId, post.mediaExtension, post.mediaWidth, post.mediaHeight);
    }

    public boolean isWebm() {
        return ".webm".equals(ext);
    }
}
