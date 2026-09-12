package ticket.booking;

import ticket.booking.entities.Ticket;
import ticket.booking.entities.Train;
import ticket.booking.entities.User;
import ticket.booking.utils.UserServiceUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestDataFactory {

    public static User createUser() {
        return new User(
                "Sadek",
                "user-001",
                "password123",
                UserServiceUtil.hashPassword("password123"),
                new ArrayList<>()
        );
    }

    public static Train createTrain() {
        return new Train(
                "bacs",
                null,
                createSeats(),
                Map.of(
                        "bangalore", "13:50:00",
                        "jaipur", "13:50:00",
                        "delhi", "13:50:00"
                ),
                List.of(
                        "bangalore",
                        "jaipur",
                        "delhi"
                )
        );
    }

    public static Ticket createTicket() {
        return new Ticket(
                "ticket-001",
                "user-001",
                "bangalore",
                "delhi",
                "2026-09-11",
                "bacs",
                null,
                1,
                1
        );
    }

    private static List<List<Integer>> createSeats() {
        return new ArrayList<>(List.of(
                new ArrayList<>(List.of(1, 0, 0, 0, 0, 0)),
                new ArrayList<>(List.of(0, 0, 0, 0, 0, 0)),
                new ArrayList<>(List.of(0, 0, 1, 0, 0, 0)),
                new ArrayList<>(List.of(0, 0, 0, 0, 0, 0))
        ));
    }
}