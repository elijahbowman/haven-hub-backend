package hearthway.listing.repository;

/**
 * Projection that loads only a picture's bytes and content type.
 */
public interface PictureFile {

    byte[] getFile();

    String getFileContentType();
}
