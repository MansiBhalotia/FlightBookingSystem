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

    @Autowired
    MockMvc mockMvc;

    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    static String createdBookingId;

    // ── Flights ──────────────────────────────────────────────────────────────

    @Test @Order(1)
    void createFlight_returns201() throws Exception {
        CreateFlightRequest req = new CreateFlightRequest();
        req.setFlightNumber("TS999");
        req.setOrigin("Origin City");
        req.setDestination("Dest City");
        req.setDepartureTime(LocalDateTime.now().plusDays(5));
        req.setTotalSeats(2);

        mockMvc.perform(post("/api/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flightNumber").value("TS999"))
                .andExpect(jsonPath("$.availableSeats").value(2))
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
    void getAllFlights_includesSeededFlights() throws Exception {
        mockMvc.perform(get("/api/flights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(4)))); // 3 seeded + TS999
    }

    // ── Bookings ─────────────────────────────────────────────────────────────

    @Test @Order(6)
    void createBooking_returns201() throws Exception {
        long version = getFlightVersion("TS999");
        CreateBookingRequest req = buildBookingRequest("TS999", version, "Alice", "Smith", "P001");

        MvcResult result = mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andReturn();

        createdBookingId = mapper.readTree(result.getResponse().getContentAsString())
                .get("bookingId").asText();
    }

    @Test @Order(7)
    void createBooking_lastSeat_succeeds() throws Exception {
        // TS999 has 2 total seats; 1 already booked — must read fresh version
        long version = getFlightVersion("TS999");
        CreateBookingRequest req = buildBookingRequest("TS999", version, "Bob", "Jones", "P002");

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @Order(8)
    void createBooking_overbook_returns409() throws Exception {
        // TS999 is now full — still passes current version but no seats left
        long version = getFlightVersion("TS999");
        CreateBookingRequest req = buildBookingRequest("TS999", version, "Carol", "White", "P003");

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Not enough seats")));
    }

    @Test @Order(9)
    void createBooking_staleVersion_returns409() throws Exception {
        // Client passes an intentionally outdated version (simulates "browsing user")
        long staleVersion = getFlightVersion("TS999") - 1; // definitely stale
        CreateBookingRequest req = buildBookingRequest("TS999", staleVersion, "Eve", "Brown", "P005");

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("changed since you last viewed")));
    }

    @Test @Order(10)
    void createBooking_unknownFlight_returns404() throws Exception {
        CreateBookingRequest req = buildBookingRequest("NOPE", 0L, "X", "Y", "P999");

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test @Order(11)
    void createBooking_missingVersion_returns400() throws Exception {
        // flightVersion field omitted entirely
        String body = """
                {"flightNumber":"TS999","passengers":[{"firstName":"X","lastName":"Y","passportNumber":"P0"}]}
                """;
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @Order(12)
    void cancelBooking_returns200() throws Exception {
        mockMvc.perform(delete("/api/bookings/" + createdBookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test @Order(13)
    void cancelBooking_alreadyCancelled_returns409() throws Exception {
        mockMvc.perform(delete("/api/bookings/" + createdBookingId))
                .andExpect(status().isConflict());
    }

    @Test @Order(14)
    void cancelBooking_unknownId_returns404() throws Exception {
        mockMvc.perform(delete("/api/bookings/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test @Order(15)
    void cancelBooking_releasesSeats_versionBumps_allowsRebook() throws Exception {
        // Alice's booking was cancelled in test 12, freeing 1 seat — version also bumped
        long version = getFlightVersion("TS999");
        CreateBookingRequest req = buildBookingRequest("TS999", version, "Dave", "Green", "P004");

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Read the current version for a flight via the GET endpoint. */
    private long getFlightVersion(String flightNumber) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/flights/" + flightNumber))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = mapper.readTree(result.getResponse().getContentAsString());
        return node.get("version").asLong();
    }

    private CreateBookingRequest buildBookingRequest(String flightNumber, long flightVersion,
                                                     String firstName, String lastName,
                                                     String passport) {
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


