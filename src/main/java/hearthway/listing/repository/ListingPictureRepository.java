package hearthway.listing.repository;

import hearthway.listing.domain.ListingPicture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ListingPictureRepository extends JpaRepository<ListingPicture, Long> {

    Optional<PictureFile> findFileByPublicId(UUID publicId);
}
