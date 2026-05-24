package com.flightbooking.dto;

import com.flightbooking.model.Booking;
import com.flightbooking.model.Passenger;

import java.time.LocalDateTime;
import java.util.List;

public class BookingResponse {

    private String bookingId;
    private String flightNumber;
    private String origin;
    private String destination;
    private LocalDateTime departureTime;
    private String status;
    private LocalDateTime bookedAt;
    private List<PassengerInfo> passengers;

    public static BookingResponse from(Booking booking) {
        BookingResponse r = new BookingResponse();
        r.bookingId      = booking.getBookingId();
        r.flightNumber   = booking.getFlightNumber();
        r.origin         = booking.getOrigin();
        r.destination    = booking.getDestination();
        r.departureTime  = booking.getDepartureTime();
        r.status         = booking.getStatus().name();
        r.bookedAt       = booking.getBookedAt();
        r.passengers     = booking.getPassengers().stream()
                .map(p -> new PassengerInfo(p.getFirstName(), p.getLastName(), p.getPassportNumber()))
                .toList();
        return r;
    }

    public String getBookingId()            { return bookingId; }
    public String getFlightNumber()         { return flightNumber; }
    public String getOrigin()               { return origin; }
    public String getDestination()          { return destination; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public String getStatus()               { return status; }
    public LocalDateTime getBookedAt()      { return bookedAt; }
    public List<PassengerInfo> getPassengers() { return passengers; }

    public record PassengerInfo(String firstName, String lastName, String passportNumber) {}
}
