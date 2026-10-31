package hearthway.listing.application;

import hearthway.listing.application.dto.DisplayCardListingDTO;
import hearthway.listing.application.dto.DisplayListingDTO;
import hearthway.listing.application.dto.SearchDTO;
import hearthway.listing.application.dto.sub.LandlordListingDTO;
import hearthway.listing.domain.BookingCategory;
import hearthway.listing.domain.Listing;
import hearthway.listing.mapper.ListingMapper;
import hearthway.listing.repository.ListingRepository;
import hearthway.sharedkernel.service.State;
import hearthway.user.application.UserService;
import hearthway.user.application.dto.ReadUserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class TenantService {

    private final ListingRepository listingRepository;

    private final ListingMapper listingMapper;

    private final UserService userService;


    public TenantService(ListingRepository listingRepository, ListingMapper listingMapper, UserService userService) {
        this.listingRepository = listingRepository;
        this.listingMapper = listingMapper;
        this.userService = userService;
    }

    public Page<DisplayCardListingDTO> getAllByCategory(Pageable pageable, BookingCategory category) {
        Page<Listing> allOrBookingCategory;
        if (category == BookingCategory.ALL) {
            allOrBookingCategory = listingRepository.findAllWithCoverOnly(pageable);
        } else {
            allOrBookingCategory = listingRepository.findAllByBookingCategoryWithCoverOnly(pageable, category);
        }

        return allOrBookingCategory.map(listingMapper::listingToDisplayCardListingDTO);
    }

    @Transactional(readOnly = true)
    public State<DisplayListingDTO, String> getOne(UUID publicId) {
        Optional<Listing> listingByPublicIdOpt = listingRepository.findByPublicId(publicId);

        if (listingByPublicIdOpt.isEmpty()) {
            return State.<DisplayListingDTO, String>builder()
                    .forError(String.format("Listing doesn't exist for publicId: %s", publicId));
        }

        DisplayListingDTO displayListingDTO = listingMapper.listingToDisplayListingDTO(listingByPublicIdOpt.get());

        ReadUserDTO readUserDTO = userService.getByPublicId(listingByPublicIdOpt.get().getLandlordPublicId()).orElseThrow();
        LandlordListingDTO landlordListingDTO = new LandlordListingDTO(readUserDTO.firstName(), readUserDTO.imageUrl());
        displayListingDTO.setLandlord(landlordListingDTO);

        return State.<DisplayListingDTO, String>builder().forSuccess(displayListingDTO);
    }


    @Transactional(readOnly = true)
    public Page<DisplayCardListingDTO> search(Pageable pageable, SearchDTO newSearch) {
        return listingRepository.searchAvailable(pageable, newSearch.location(),
                        newSearch.infos().baths().value(),
                        newSearch.infos().bedrooms().value(),
                        newSearch.infos().guests().value(),
                        newSearch.infos().beds().value(),
                        newSearch.dates().startDate(),
                        newSearch.dates().endDate())
                .map(listingMapper::listingToDisplayCardListingDTO);
    }
}
