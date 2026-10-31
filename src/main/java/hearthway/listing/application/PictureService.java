package hearthway.listing.application;

import hearthway.listing.application.dto.sub.PictureDTO;
import hearthway.listing.domain.Listing;
import hearthway.listing.domain.ListingPicture;
import hearthway.listing.mapper.ListingPictureMapper;
import hearthway.listing.repository.ListingPictureRepository;
import hearthway.listing.repository.PictureFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class PictureService {

    /**
     * Widths a client may request. A fixed set keeps resizing work bounded and lets
     * browsers and proxies cache a handful of variants per picture.
     */
    public static final Set<Integer> ALLOWED_WIDTHS = Set.of(320, 640, 1280);

    private final ListingPictureRepository listingPictureRepository;

    private final ListingPictureMapper listingPictureMapper;

    public PictureService(ListingPictureRepository listingPictureRepository, ListingPictureMapper listingPictureMapper) {
        this.listingPictureRepository = listingPictureRepository;
        this.listingPictureMapper = listingPictureMapper;
    }

    /**
     * Saves pictures in the given order; the first one becomes the cover.
     */
    public List<PictureDTO> saveAll(List<PictureDTO> pictures, Listing listing) {
        List<ListingPicture> listingPictures = listingPictureMapper.pictureDTOsToListingPictures(pictures);

        for (int i = 0; i < listingPictures.size(); i++) {
            ListingPicture listingPicture = listingPictures.get(i);
            listingPicture.setCover(i == 0);
            listingPicture.setListing(listing);
        }

        listingPictureRepository.saveAll(listingPictures);
        return listingPictureMapper.listingPictureToPictureDTO(listingPictures);
    }

    /**
     * The picture's bytes, scaled down to {@code width} as JPEG when a width is given
     * and the image can be resized; otherwise the original.
     */
    @Transactional(readOnly = true)
    public Optional<PictureContent> getContent(UUID publicId, Integer width) {
        if (width != null && !ALLOWED_WIDTHS.contains(width)) {
            throw new IllegalArgumentException("Unsupported width " + width + "; use one of " + ALLOWED_WIDTHS);
        }
        return listingPictureRepository.findFileByPublicId(publicId)
                .map(picture -> width == null ? original(picture) : scaled(picture, width));
    }

    private static PictureContent original(PictureFile picture) {
        return new PictureContent(picture.getFile(), picture.getFileContentType());
    }

    private static PictureContent scaled(PictureFile picture, int width) {
        return ImageResizer.scaleToJpeg(picture.getFile(), width)
                .map(jpeg -> new PictureContent(jpeg, "image/jpeg"))
                .orElseGet(() -> original(picture));
    }

    public record PictureContent(byte[] bytes, String contentType) {
    }
}
