package hearthway.listing.mapper;

import hearthway.listing.application.dto.sub.PictureDTO;
import hearthway.listing.domain.ListingPicture;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface ListingPictureMapper {


    List<ListingPicture> pictureDTOsToListingPictures(List<PictureDTO> pictureDTOs);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "listing", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "cover", source = "isCover")
    ListingPicture pictureDTOToListingPicture(PictureDTO pictureDTO);

    List<PictureDTO> listingPictureToPictureDTO(List<ListingPicture> listingPictures);

    @Mapping(target = "isCover", source = "cover")
    PictureDTO convertToPictureDTO(ListingPicture listingPicture);

    /**
     * Returns the picture flagged as cover. Some queries load every picture of a
     * listing, so the set can't be assumed to contain only the cover. Falls back to
     * the earliest saved picture (lowest id) for listings without a cover flag.
     */
    @Named("extract-cover")
    default PictureDTO extractCover(Set<ListingPicture> pictures) {
        return pictures.stream()
                .filter(ListingPicture::isCover)
                .findFirst()
                .or(() -> pictures.stream().min(Comparator.comparing(ListingPicture::getId,
                        Comparator.nullsLast(Comparator.naturalOrder()))))
                .map(this::convertToPictureDTO)
                .orElseThrow();
    }

}
