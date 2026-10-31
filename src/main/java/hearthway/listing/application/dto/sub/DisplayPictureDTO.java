package hearthway.listing.application.dto.sub;

import java.util.UUID;

/**
 * A picture as clients see it: an id to load it from /api/pictures/{publicId},
 * not the image bytes themselves.
 */
public record DisplayPictureDTO(UUID publicId, boolean isCover) {
}
