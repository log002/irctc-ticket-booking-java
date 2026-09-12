package ticket.booking.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ticket.booking.entities.Ticket;
import ticket.booking.entities.Train;
import ticket.booking.entities.User;
import ticket.booking.utils.UserServiceUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class UserBookingService {

    private User user;

    private List<User> userList;

    private ObjectMapper objectMapper = new ObjectMapper();

    private final TrainService trainService;

    private final String usersPath;

    public UserBookingService() throws IOException{
        this.usersPath = "D:/My_Workspace/Java/IRCTC/app/src/main/java/ticket/booking/localDB/users.json";
        this.trainService = new TrainService();
        loadUserListFromFile();
    }

    public UserBookingService(User user) throws IOException {
        this.user = user;
        this.usersPath = "D:/My_Workspace/Java/IRCTC/app/src/main/java/ticket/booking/localDB/users.json";
        this.trainService = new TrainService();
        loadUserListFromFile();
    }

    public UserBookingService(String usersPath) throws IOException{
        this.usersPath = usersPath;
        this.trainService = new TrainService();
        loadUserListFromFile();
    }

    public UserBookingService(User user, String usersPath) throws IOException{
        this.user = user;
        this.usersPath = usersPath;
        this.trainService = new TrainService();
        loadUserListFromFile();
    }

    public UserBookingService(String usersPath, TrainService trainService) throws IOException {
        this.usersPath = usersPath;
        this.trainService = trainService;
        loadUserListFromFile();
    }

    public UserBookingService(User user, String usersPath, TrainService trainService) throws IOException {
        this.user = user;
        this.usersPath = usersPath;
        this.trainService = trainService;
        loadUserListFromFile();
    }

    private void loadUserListFromFile() throws IOException{
        userList = objectMapper.readValue(new File(usersPath), new TypeReference<List<User>>() {});
    }

    public Boolean loginUser(){
        Optional<User> foundUser = userList.stream().filter(user1 -> {
            return user1.getName().equals(user.getName()) && UserServiceUtil.checkPassword(user.getPassword(), user1.getHashedPassword());
        }).findFirst();

        if(foundUser.isPresent()){
            user = foundUser.get();
            return true;
        }
        return false;
    }

    public Boolean signUp(User user1){
        try{
            userList.add(user1);
            saveUserListToFile();
            return Boolean.TRUE;
        }catch (IOException ex){
            return Boolean.FALSE;
        }
    }

    private void saveUserListToFile() throws IOException{
        File userFile = new File(usersPath);
        objectMapper.writeValue(userFile, userList);
    }

    public void fetchBooking(){
        Optional<User> userFetched = userList.stream().filter(user1 -> {
            return user1.getName().equals(user.getName()) && UserServiceUtil.checkPassword(user.getPassword(), user1.getHashedPassword());
        }).findFirst();
        if(userFetched.isPresent()){
            userFetched.get().printTickets();
        }
    }

    public Boolean cancelBooking(){
        Scanner sc = new Scanner(System.in);

        System.out.println("Enter the ticket id to cancel: ");
        String ticketId = sc.nextLine();

        if(ticketId == null || ticketId.isEmpty()){
            System.out.println("Ticket ID cannot be null or empty");
            return Boolean.FALSE;
        }

        Optional<Ticket> ticketToCancel = this.user.getTicketsBooked()
                .stream()
                .filter(ticket -> ticket.getTicketId().equals(ticketId))
                .findFirst();

        if(ticketToCancel.isEmpty()){
            System.out.println("No ticket found with ID " + ticketId);
            return Boolean.FALSE;
        }

        Ticket ticket = ticketToCancel.get();

        try{
          Optional<Train> train = trainService.getTrainById(ticket.getTrainId());

          if(train.isEmpty()){
              System.out.println("Train not found for this ticket.");
              return Boolean.FALSE;
          }

          Train bookedTrain = train.get();

          int row = ticket.getRow();
          int seat = ticket.getSeat();

          List<List<Integer>> seats = bookedTrain.getSeats();

          if(row < 0 || row >= seats.size() ||
                seat < 0 || seat >= seats.get(row).size()){
              System.out.println("Invalid seat information in ticket.");
              return Boolean.FALSE;
          }

          // relase the booked seat
          seats.get(row).set(seat, 0);
          bookedTrain.setSeats(seats);

          // save updated train
          trainService.updateTrain(bookedTrain);

          // remove ticket from user
          this.user.getTicketsBooked().remove(ticket);

          // save updated user list
          saveUserListToFile();

          System.out.println("Ticket with ID " + ticketId + " has been cancelled.");
          System.out.println("Seat row " + row + ", column " + seat + " is now available.");

          return Boolean.TRUE;

        }catch(IOException ex){
            System.out.println("Unable to cancel booking");
            return Boolean.FALSE;
        }
    }

    public List<Train> getTrains(String source, String dest){
        return trainService.searchTrains(source, dest);
    }

    public List<List<Integer>> fetchSeats(Train train){
        return train.getSeats();
    }

    public Boolean bookTrainSeat(Train train, int row, int seat, String source, String dest, String dateOfTravel){
        try {
            List<List<Integer>> seats = train.getSeats();

            if (row >= 0 && row < seats.size() && seat >= 0 &&
                    seat < seats.get(row).size()) {
                if (seats.get(row).get(seat) == 0) {
                    // mark seat as booked
                    seats.get(row).set(seat, 1);
                    train.setSeats(seats);

                    // save updated train
                    trainService.addTrain(train);

                    // create ticket
                    Ticket ticket = new Ticket(
                            UUID.randomUUID().toString(),
                            user.getUserId(),
                            source,
                            dest,
                            dateOfTravel,
                            train.getTrainId(),
                            train.getTrainNum(),
                            row,
                            seat
                    );

                    // add ticket to logged in user
                    user.getTicketsBooked().add(ticket);

                    // save updated user
                    saveUserListToFile();

                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }catch(IOException ex){
            return Boolean.FALSE;
        }
    }
}
