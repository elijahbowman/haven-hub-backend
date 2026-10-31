package hearthway.listing.application;

import hearthway.listing.application.dto.sub.PictureDTO;
import hearthway.listing.domain.Listing;
import hearthway.listing.domain.ListingPicture;
import hearthway.listing.mapper.ListingPictureMapper;
import hearthway.listing.repository.ListingPictureRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PictureService {

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
}
