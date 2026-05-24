package com.flightbooking.repository;

import com.flightbooking.model.Booking;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class BookingRepository {

    private final ConcurrentHashMap<String, Booking> store = new ConcurrentHashMap<>();

    public Booking save(Booking booking) {
        store.put(booking.getBookingId(), booking);
        return booking;
    }

    public Optional<Booking> findById(String bookingId) {
        return Optional.ofNullable(store.get(bookingId));
    }
}

