package ticket.booking.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ticket.booking.entities.Train;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TrainServiceTest {

    private TrainService trainService;

    @BeforeEach
    void setUp() throws IOException{
        trainService = new TrainService();
    }

    @Test
    void shouldReturnTrainForValidRoute(){
        List<Train> trains = trainService.searchTrains("bangalore", "delhi");

        assertFalse(trains.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForInvalidRoute(){
        List<Train> trains = trainService.searchTrains("bangalore", "jalna");

        assertTrue(trains.isEmpty());
    }

    @Test
    void shouldRejectReverseJourney(){
        Optional<Train> train = trainService.getTrainById("bacs");

        assertTrue(train.isPresent());
        assertEquals("bacs", train.get().getTrainId());
    }

    @Test
    void shouldFindTrainById(){
        Optional<Train> train = trainService.getTrainById("bacs");

        assertTrue(train.isPresent());
        assertEquals("bacs", train.get().getTrainId());
    }

    @Test
    void shouldReturnEmptyWhenTrainDoesNotExist(){
        Optional<Train> train = trainService.getTrainById("invalid-train-id");

        assertTrue(train.isEmpty());
    }
}
