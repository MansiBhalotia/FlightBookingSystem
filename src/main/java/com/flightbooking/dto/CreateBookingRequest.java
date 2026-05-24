package com.flightbooking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CreateBookingRequest {

    @NotBlank(message = "Flight number is required")
    private String flightNumber;

    /**
     * The version of the flight the client observed when they decided to book.
     * Used for optimistic concurrency control: if the flight's version has
     * advanced (another booking/cancellation happened), this request is rejected
     * so the client must refresh and re-submit with the latest version.
     */
    @NotNull(message = "flightVersion is required (use the version returned by GET /api/flights/{flightNumber})")
    @Min(value = 0, message = "flightVersion must be >= 0")
    private Long flightVersion;

    @NotEmpty(message = "At least one passenger is required")
    @Valid
    private List<PassengerRequest> passengers;

    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }

    public Long getFlightVersion() { return flightVersion; }
    public void setFlightVersion(Long flightVersion) { this.flightVersion = flightVersion; }

    public List<PassengerRequest> getPassengers() { return passengers; }
    public void setPassengers(List<PassengerRequest> passengers) { this.passengers = passengers; }
}

