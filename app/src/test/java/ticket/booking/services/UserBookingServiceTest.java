package ticket.booking.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ticket.booking.TestDataFactory;
import ticket.booking.entities.Ticket;
import ticket.booking.entities.Train;
import ticket.booking.entities.User;
import ticket.booking.utils.UserServiceUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class UserBookingServiceTest {

    @TempDir
    Path tempDir;

    private Path usersFile;

    @BeforeEach
    void setUp() throws IOException {
        usersFile = tempDir.resolve("users.json");

        Files.writeString(usersFile, "[]");
    }

    @Test
    void shouldSignUpUserSuccessfully() throws IOException {

        User user = new User(
                "Sadek",
                "user-001",
                "password123",
                UserServiceUtil.hashPassword("password123"),
                new java.util.ArrayList<>()
        );

        UserBookingService service =
                new UserBookingService(usersFile.toString());

        assertTrue(service.signUp(user));

        String savedUsers = Files.readString(usersFile);

        assertTrue(savedUsers.contains("Sadek"));
        assertTrue(savedUsers.contains("user-001"));
    }

    @Test
    void shouldLoginWithValidCredentials() throws IOException {

        String hashedPassword =
                UserServiceUtil.hashPassword("password123");

        String userJson = """
                [
                  {
                    "name": "Sadek",
                    "user_id": "user-001",
                    "password": "password123",
                    "hashed_password": "%s",
                    "tickets_booked": []
                  }
                ]
                """.formatted(hashedPassword);

        Files.writeString(usersFile, userJson);

        User loginUser = new User();
        loginUser.setName("Sadek");
        loginUser.setPassword("password123");

        UserBookingService service =
                new UserBookingService(loginUser, usersFile.toString());

        assertTrue(service.loginUser());
    }

    @Test
    void shouldRejectInvalidPassword() throws IOException {

        String hashedPassword =
                UserServiceUtil.hashPassword("password123");

        String userJson = """
                [
                  {
                    "name": "Sadek",
                    "user_id": "user-001",
                    "password": "password123",
                    "hashed_password": "%s",
                    "tickets_booked": []
                  }
                ]
                """.formatted(hashedPassword);

        Files.writeString(usersFile, userJson);

        User loginUser = new User();
        loginUser.setName("Sadek");
        loginUser.setPassword("wrongPassword");

        UserBookingService service =
                new UserBookingService(loginUser, usersFile.toString());

        assertFalse(service.loginUser());
    }

    @Test
    void shouldRejectUnknownUser() throws IOException {

        String hashedPassword =
                UserServiceUtil.hashPassword("password123");

        String userJson = """
                [
                  {
                    "name": "Sadek",
                    "user_id": "user-001",
                    "password": "password123",
                    "hashed_password": "%s",
                    "tickets_booked": []
                  }
                ]
                """.formatted(hashedPassword);

        Files.writeString(usersFile, userJson);

        User loginUser = new User();
        loginUser.setName("UnknownUser");
        loginUser.setPassword("password123");

        UserBookingService service =
                new UserBookingService(loginUser, usersFile.toString());

        assertFalse(service.loginUser());
    }

    @Test
    void shouldBookAvailableSeatSuccessfully() throws IOException {

        // Arrange: create isolated train data
        Path trainsFile = tempDir.resolve("trains.json");

        String trainJson = """
            [
              {
                "train_id": "bacs",
                "train_num": null,
                "station_times": {
                  "bangalore": "13:50:00",
                  "jaipur": "13:50:00",
                  "delhi": "13:50:00"
                },
                "stations": [
                  "bangalore",
                  "jaipur",
                  "delhi"
                ],
                "seats": [
                  [1, 0, 0, 0, 0, 0],
                  [0, 0, 0, 0, 0, 0],
                  [0, 0, 1, 0, 0, 0],
                  [0, 0, 0, 0, 0, 0]
                ]
              }
            ]
            """;

        Files.writeString(trainsFile, trainJson);

        // Create a test user
        User user = new User(
                "Sadek",
                "user-001",
                "password123",
                UserServiceUtil.hashPassword("password123"),
                new ArrayList<>()
        );

        // Create TrainService using temporary trains.json
        TrainService trainService =
                new TrainService(trainsFile.toString());

        // Create UserBookingService using temporary files
        UserBookingService bookingService =
                new UserBookingService(
                        user,
                        usersFile.toString(),
                        trainService
                );

        // Get the train
        Train train = trainService
                .getTrainById("bacs")
                .orElseThrow();

        // Act: book row 1, seat 1
        boolean result = bookingService.bookTrainSeat(
                train,
                1,
                1,
                "bangalore",
                "delhi",
                "2026-09-11"
        );

        // Assert
        assertTrue(result);

        // Verify seat is now booked
        Train updatedTrain = trainService
                .getTrainById("bacs")
                .orElseThrow();

        assertEquals(1, updatedTrain.getSeats().get(1).get(1));

        // Verify ticket was added to user
        assertEquals(1, user.getTicketsBooked().size());

        assertEquals("bacs",
                user.getTicketsBooked().get(0).getTrainId());

        assertEquals(1,
                user.getTicketsBooked().get(0).getRow());

        assertEquals(1,
                user.getTicketsBooked().get(0).getSeat());
    }

    @Test
    void shouldRejectBookingForAlreadyOccupiedSeat() throws IOException {

        Path trainsFile = tempDir.resolve("trains.json");

        String trainJson = """
            [
              {
                "train_id": "bacs",
                "train_num": null,
                "station_times": {
                  "bangalore": "13:50:00",
                  "jaipur": "13:50:00",
                  "delhi": "13:50:00"
                },
                "stations": [
                  "bangalore",
                  "jaipur",
                  "delhi"
                ],
                "seats": [
                  [1, 0, 0, 0, 0, 0],
                  [0, 0, 0, 0, 0, 0],
                  [0, 0, 1, 0, 0, 0],
                  [0, 0, 0, 0, 0, 0]
                ]
              }
            ]
            """;

        Files.writeString(trainsFile, trainJson);

        User user = new User(
                "Sadek",
                "user-001",
                "password123",
                UserServiceUtil.hashPassword("password123"),
                new ArrayList<>()
        );

        TrainService trainService =
                new TrainService(trainsFile.toString());

        UserBookingService bookingService =
                new UserBookingService(
                        user,
                        usersFile.toString(),
                        trainService
                );

        Train train = trainService
                .getTrainById("bacs")
                .orElseThrow();

        // Seat [0][0] is already occupied
        boolean result = bookingService.bookTrainSeat(
                train,
                0,
                0,
                "bangalore",
                "delhi",
                "2026-09-11"
        );

        assertFalse(result);

        // No ticket should have been created
        assertEquals(0, user.getTicketsBooked().size());

        // Seat must remain occupied
        Train unchangedTrain = trainService
                .getTrainById("bacs")
                .orElseThrow();

        assertEquals(1, unchangedTrain.getSeats().get(0).get(0));
    }

    @Test
    void shouldRejectInvalidSeatCoordinates() throws IOException {

        Path trainsFile = tempDir.resolve("trains.json");

        String trainJson = """
            [
              {
                "train_id": "bacs",
                "train_num": null,
                "station_times": {
                  "bangalore": "13:50:00",
                  "jaipur": "13:50:00",
                  "delhi": "13:50:00"
                },
                "stations": [
                  "bangalore",
                  "jaipur",
                  "delhi"
                ],
                "seats": [
                  [1, 0, 0, 0, 0, 0],
                  [0, 0, 0, 0, 0, 0],
                  [0, 0, 1, 0, 0, 0],
                  [0, 0, 0, 0, 0, 0]
                ]
              }
            ]
            """;

        Files.writeString(trainsFile, trainJson);

        User user = new User(
                "Sadek",
                "user-001",
                "password123",
                UserServiceUtil.hashPassword("password123"),
                new ArrayList<>()
        );

        TrainService trainService =
                new TrainService(trainsFile.toString());

        UserBookingService bookingService =
                new UserBookingService(
                        user,
                        usersFile.toString(),
                        trainService
                );

        Train train = trainService
                .getTrainById("bacs")
                .orElseThrow();

        // Row 10 does not exist
        boolean result = bookingService.bookTrainSeat(
                train,
                10,
                0,
                "bangalore",
                "delhi",
                "2026-09-11"
        );

        assertFalse(result);

        // No ticket should be created
        assertEquals(0, user.getTicketsBooked().size());
    }

    @Test
    void shouldRejectNegativeSeatCoordinates() throws IOException {

        Path trainsFile = tempDir.resolve("trains.json");

        String trainJson = """
            [
              {
                "train_id": "bacs",
                "train_num": null,
                "station_times": {
                  "bangalore": "13:50:00",
                  "jaipur": "13:50:00",
                  "delhi": "13:50:00"
                },
                "stations": [
                  "bangalore",
                  "jaipur",
                  "delhi"
                ],
                "seats": [
                  [1, 0, 0, 0, 0, 0],
                  [0, 0, 0, 0, 0, 0],
                  [0, 0, 1, 0, 0, 0],
                  [0, 0, 0, 0, 0, 0]
                ]
              }
            ]
            """;

        Files.writeString(trainsFile, trainJson);

        User user = new User(
                "Sadek",
                "user-001",
                "password123",
                UserServiceUtil.hashPassword("password123"),
                new ArrayList<>()
        );

        TrainService trainService =
                new TrainService(trainsFile.toString());

        UserBookingService bookingService =
                new UserBookingService(
                        user,
                        usersFile.toString(),
                        trainService
                );

        Train train = trainService
                .getTrainById("bacs")
                .orElseThrow();

        // Negative row
        boolean result = bookingService.bookTrainSeat(
                train,
                -1,
                0,
                "bangalore",
                "delhi",
                "2026-09-11"
        );

        assertFalse(result);

        // No ticket should be created
        assertEquals(0, user.getTicketsBooked().size());
    }

    @Test
    void shouldCancelBookingSuccessfully() throws IOException {

        Path trainsFile = tempDir.resolve("trains.json");

        String trainJson = """
            [
              {
                "train_id":"bacs",
                "train_num":null,
                "station_times":{
                  "bangalore":"13:50:00",
                  "jaipur":"13:50:00",
                  "delhi":"13:50:00"
                },
                "stations":[
                  "bangalore",
                  "jaipur",
                  "delhi"
                ],
                "seats":[
                  [1,0,0,0,0,0],
                  [0,1,0,0,0,0],
                  [0,0,1,0,0,0],
                  [0,0,0,0,0,0]
                ]
              }
            ]
            """;

        Files.writeString(trainsFile, trainJson);

        User user = TestDataFactory.createUser();

        Ticket ticket = TestDataFactory.createTicket();

        user.getTicketsBooked().add(ticket);

        TrainService trainService =
                new TrainService(trainsFile.toString());

        UserBookingService bookingService =
                new UserBookingService(
                        user,
                        usersFile.toString(),
                        trainService
                );

        boolean cancelled =
                bookingService.cancelBooking("ticket-001");

        assertTrue(cancelled);

        assertEquals(0, user.getTicketsBooked().size());

        Train updatedTrain = trainService
                .getTrainById("bacs")
                .orElseThrow();

        assertEquals(0,
                updatedTrain.getSeats().get(1).get(1));
    }

    @Test
    void shouldRejectCancellationForUnknownTicket() throws IOException {

        Path trainsFile = tempDir.resolve("trains.json");

        String trainJson = """
            [
              {
                "train_id": "bacs",
                "train_num": null,
                "station_times": {
                  "bangalore": "13:50:00",
                  "jaipur": "13:50:00",
                  "delhi": "13:50:00"
                },
                "stations": [
                  "bangalore",
                  "jaipur",
                  "delhi"
                ],
                "seats": [
                  [1, 0, 0, 0, 0, 0],
                  [0, 1, 0, 0, 0, 0],
                  [0, 0, 1, 0, 0, 0],
                  [0, 0, 0, 0, 0, 0]
                ]
              }
            ]
            """;

        Files.writeString(trainsFile, trainJson);

        User user = TestDataFactory.createUser();

        Ticket ticket = TestDataFactory.createTicket();
        user.getTicketsBooked().add(ticket);

        TrainService trainService =
                new TrainService(trainsFile.toString());

        UserBookingService bookingService =
                new UserBookingService(
                        user,
                        usersFile.toString(),
                        trainService
                );

        boolean cancelled =
                bookingService.cancelBooking("unknown-ticket");

        assertFalse(cancelled);

        // Existing ticket must remain
        assertEquals(1, user.getTicketsBooked().size());

        // Seat must remain booked
        Train train = trainService
                .getTrainById("bacs")
                .orElseThrow();

        assertEquals(1, train.getSeats().get(1).get(1));
    }

    @Test
    void shouldRejectCancellationWhenTrainDoesNotExist() throws IOException {

        Path trainsFile = tempDir.resolve("trains.json");

        // Empty train database
        Files.writeString(trainsFile, "[]");

        User user = TestDataFactory.createUser();

        Ticket ticket = TestDataFactory.createTicket();
        user.getTicketsBooked().add(ticket);

        TrainService trainService =
                new TrainService(trainsFile.toString());

        UserBookingService bookingService =
                new UserBookingService(
                        user,
                        usersFile.toString(),
                        trainService
                );

        boolean cancelled =
                bookingService.cancelBooking("ticket-001");

        assertFalse(cancelled);

        // Ticket should remain because cancellation failed
        assertEquals(1, user.getTicketsBooked().size());

        assertEquals(
                "ticket-001",
                user.getTicketsBooked().get(0).getTicketId()
        );
    }

    @Test
    void shouldRejectCancellationForInvalidSeatInformation() throws IOException {

        Path trainsFile = tempDir.resolve("trains.json");

        String trainJson = """
            [
              {
                "train_id": "bacs",
                "train_num": null,
                "station_times": {
                  "bangalore": "13:50:00",
                  "jaipur": "13:50:00",
                  "delhi": "13:50:00"
                },
                "stations": [
                  "bangalore",
                  "jaipur",
                  "delhi"
                ],
                "seats": [
                  [1, 0, 0, 0, 0, 0],
                  [0, 1, 0, 0, 0, 0],
                  [0, 0, 1, 0, 0, 0],
                  [0, 0, 0, 0, 0, 0]
                ]
              }
            ]
            """;

        Files.writeString(trainsFile, trainJson);

        User user = TestDataFactory.createUser();

        Ticket ticket = new Ticket(
                "ticket-invalid-seat",
                "user-001",
                "bangalore",
                "delhi",
                "2026-09-11",
                "bacs",
                null,
                10,   // invalid row
                1
        );

        user.getTicketsBooked().add(ticket);

        TrainService trainService =
                new TrainService(trainsFile.toString());

        UserBookingService bookingService =
                new UserBookingService(
                        user,
                        usersFile.toString(),
                        trainService
                );

        boolean cancelled =
                bookingService.cancelBooking("ticket-invalid-seat");

        assertFalse(cancelled);

        // Ticket must remain because cancellation failed
        assertEquals(1, user.getTicketsBooked().size());

        assertEquals(
                "ticket-invalid-seat",
                user.getTicketsBooked().get(0).getTicketId()
        );
    }

    @Test
    void shouldRejectCancellationForEmptyTicketId() throws IOException {

        Path trainsFile = tempDir.resolve("trains.json");

        Files.writeString(trainsFile, "[]");

        TrainService trainService =
                new TrainService(trainsFile.toString());

        User user = TestDataFactory.createUser();

        UserBookingService bookingService =
                new UserBookingService(
                        user,
                        usersFile.toString(),
                        trainService
                );

        boolean cancelled =
                bookingService.cancelBooking("");

        assertFalse(cancelled);

        assertEquals(0, user.getTicketsBooked().size());
    }
}