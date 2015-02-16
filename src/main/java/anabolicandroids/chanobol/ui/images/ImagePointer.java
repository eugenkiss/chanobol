package anabolicandroids.chanobol.ui.images;


import org.parceler.Parcel;

import anabolicandroids.chanobol.api.data.Post;

@Parcel
public class ImagePointer {
    public String id;
    public String ext;
    public int w;
    public int h;

    @SuppressWarnings("UnusedDeclaration") public ImagePointer() {}

    public ImagePointer(String imageId, String imageExtension, int width, int height) {
        id = imageId;
        ext = imageExtension;
        w = width;
        h = height;
    }

    public static ImagePointer from(Post post) {
        return new ImagePointer(post.imageId, post.imageExtension, post.imageWidth, post.imageHeight);
    }
}
