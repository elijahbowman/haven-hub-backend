package hearthway.listing.application.dto;

import hearthway.listing.application.dto.sub.PictureDTO;
import hearthway.listing.application.dto.vo.PriceVO;
import hearthway.listing.domain.BookingCategory;

import java.util.UUID;

public record DisplayCardListingDTO(PriceVO price,
                                    String location,
                                    PictureDTO cover,
                                    BookingCategory bookingCategory,
                                    UUID publicId) {
}
