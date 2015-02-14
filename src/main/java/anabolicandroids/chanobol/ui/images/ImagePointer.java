package anabolicandroids.chanobol.ui.images;


import org.parceler.Parcel;

import anabolicandroids.chanobol.api.data.Post;

@Parcel
public class ImagePointer {
    public String id;
    public String ext;

    @SuppressWarnings("UnusedDeclaration") public ImagePointer() {}

    public ImagePointer(String imageId, String imageExtension) {
        id = imageId;
        ext = imageExtension;
    }

    public static ImagePointer from(Post post) {
        return new ImagePointer(post.imageId, post.imageExtension);
    }
}
