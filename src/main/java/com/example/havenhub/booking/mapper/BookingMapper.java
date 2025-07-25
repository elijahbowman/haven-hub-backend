package com.example.havenhub.booking.mapper;

import com.example.havenhub.booking.application.dto.BookedDateDTO;
import com.example.havenhub.booking.application.dto.NewBookingDTO;
import com.example.havenhub.booking.domain.Booking;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    Booking newBookingToBooking(NewBookingDTO newBookingDTO);

    BookedDateDTO bookingToCheckAvailability(Booking booking);
}
