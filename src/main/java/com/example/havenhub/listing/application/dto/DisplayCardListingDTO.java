package com.example.havenhub.listing.application.dto;

import com.example.havenhub.listing.application.dto.sub.PictureDTO;
import com.example.havenhub.listing.application.dto.vo.PriceVO;
import com.example.havenhub.listing.domain.BookingCategory;

import java.util.UUID;

public record DisplayCardListingDTO(PriceVO price,
                                    String location,
                                    PictureDTO cover,
                                    BookingCategory bookingCategory,
                                    UUID publicId) {
}
