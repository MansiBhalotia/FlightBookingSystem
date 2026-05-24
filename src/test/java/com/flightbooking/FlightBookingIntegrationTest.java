package com.flightbooking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flightbooking.dto.CreateBookingRequest;
import com.flightbooking.dto.CreateFlightRequest;
import com.flightbooking.dto.PassengerRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlightBookingIntegrationTest {

    @Autowired MockMvc mockMvc;

    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    static String confirmedBookingId;

    // ── Flights ──────────────────────────────────────────────────────────────

    @Test @Order(1)
    void createFlight_returns201() throws Exception {
        CreateFlightRequest req = new CreateFlightRequest();
        req.setFlightNumber("TS999");
        req.setOrigin("New York (JFK)");
        req.setDestination("Los Angeles (LAX)");
        req.setDepartureTime(LocalDateTime.now().plusDays(5));
        req.setTotalSeats(3);

        mockMvc.perform(post("/api/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flightNumber").value("TS999"))
                .andExpect(jsonPath("$.availableSeats").value(3))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test @Order(2)
    void createFlight_duplicate_returns409() throws Exception {
        CreateFlightRequest req = new CreateFlightRequest();
        req.setFlightNumber("TS999");
        req.setOrigin("A"); req.setDestination("B");
        req.setDepartureTime(LocalDateTime.now().plusDays(1));
        req.setTotalSeats(10);

        mockMvc.perform(post("/api/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @Order(3)
    void getFlight_returns200_withVersion() throws Exception {
        mockMvc.perform(get("/api/flights/TS999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightNumber").value("TS999"))
                .andExpect(jsonPath("$.version").isNumber());
    }

    @Test @Order(4)
    void getFlight_unknown_returns404() throws Exception {
        mockMvc.perform(get("/api/flights/UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test @Order(5)
    void getAllFlights_includesSeededAndCreatedFlights() throws Exception {
        mockMvc.perform(get("/api/flights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(4))));
    }

    // ── Booking: one-click book → CONFIRMED ───────────────────────────────────

    @Test @Order(6)
    void book_returns201_confirmedWithFlightDetails() throws Exception {
        long version = getFlightVersion("TS999");

        MvcResult result = mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(buildRequest("TS999", version, "Alice", "Smith", "P001"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))          // confirmed in one step
                .andExpect(jsonPath("$.flightNumber").value("TS999"))
                .andExpect(jsonPath("$.origin").value("New York (JFK)"))
                .andExpect(jsonPath("$.destination").value("Los Angeles (LAX)"))
                .andExpect(jsonPath("$.departureTime").isNotEmpty())
                .andExpect(jsonPath("$.passengers[0].firstName").value("Alice"))
                .andReturn();

        confirmedBookingId = mapper.readTree(result.getResponse().getContentAsString())
                .get("bookingId").asText();
    }

    @Test @Order(7)
    void confirmEndpoint_doesNotExist() throws Exception {
        // confirm is internal — no public endpoint
        mockMvc.perform(post("/api/bookings/" + confirmedBookingId + "/confirm"))
                .andExpect(status().isNotFound());
    }

    // ── Booking: cancel ────────────────────────────────────────────────────────

    @Test @Order(8)
    void cancelBooking_returns200_andReleasesSeats() throws Exception {
        int before = getAvailableSeats("TS999");

        mockMvc.perform(delete("/api/bookings/" + confirmedBookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assert getAvailableSeats("TS999") == before + 1;
    }

    @Test @Order(9)
    void cancelBooking_alreadyCancelled_returns409() throws Exception {
        mockMvc.perform(delete("/api/bookings/" + confirmedBookingId))
                .andExpect(status().isConflict());
    }

    @Test @Order(10)
    void cancelBooking_unknownId_returns404() throws Exception {
        mockMvc.perform(delete("/api/bookings/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    // ── OCC / seat checks ──────────────────────────────────────────────────────

    @Test @Order(11)
    void book_staleVersion_returns409() throws Exception {
        long stale = getFlightVersion("TS999") - 1;
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(buildRequest("TS999", stale, "Eve", "Brown", "P005"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("changed since you last viewed")));
    }

    @Test @Order(12)
    void book_fillAllSeats_thenOverbook_returns409() throws Exception {
        int seatsToFill = getAvailableSeats("TS999");
        for (int i = 0; i < seatsToFill; i++) {
            long v = getFlightVersion("TS999");
            mockMvc.perform(post("/api/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(
                                    buildRequest("TS999", v, "P" + i, "Last", "PP" + i))))
                    .andExpect(status().isCreated());
        }
        long v = getFlightVersion("TS999");
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(buildRequest("TS999", v, "Over", "Book", "OB1"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Not enough seats")));
    }

    @Test @Order(13)
    void book_unknownFlight_returns404() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(buildRequest("NOPE", 0L, "X", "Y", "P999"))))
                .andExpect(status().isNotFound());
    }

    @Test @Order(14)
    void book_missingVersion_returns400() throws Exception {
        String body = """
                {"flightNumber":"TS999","passengers":[{"firstName":"X","lastName":"Y","passportNumber":"P0"}]}
                """;
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private long getFlightVersion(String flightNumber) throws Exception {
        MvcResult r = mockMvc.perform(get("/api/flights/" + flightNumber))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(r.getResponse().getContentAsString()).get("version").asLong();
    }

    private int getAvailableSeats(String flightNumber) throws Exception {
        MvcResult r = mockMvc.perform(get("/api/flights/" + flightNumber))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(r.getResponse().getContentAsString()).get("availableSeats").asInt();
    }

    private CreateBookingRequest buildRequest(String flightNumber, long flightVersion,
                                              String firstName, String lastName, String passport) {
        PassengerRequest p = new PassengerRequest();
        p.setFirstName(firstName);
        p.setLastName(lastName);
        p.setPassportNumber(passport);

        CreateBookingRequest req = new CreateBookingRequest();
        req.setFlightNumber(flightNumber);
        req.setFlightVersion(flightVersion);
        req.setPassengers(List.of(p));
        return req;
    }
}
