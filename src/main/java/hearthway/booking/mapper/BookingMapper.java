package hearthway.booking.mapper;

import hearthway.booking.application.dto.BookedDateDTO;
import hearthway.booking.application.dto.NewBookingDTO;
import hearthway.booking.domain.Booking;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    Booking newBookingToBooking(NewBookingDTO newBookingDTO);

    BookedDateDTO bookingToCheckAvailability(Booking booking);
}
