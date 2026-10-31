package hearthway.booking.application;

/**
 * The requested nights overlap an existing booking for the listing.
 */
public class BookingConflictException extends RuntimeException {

    public BookingConflictException(String message) {
        super(message);
    }
}
