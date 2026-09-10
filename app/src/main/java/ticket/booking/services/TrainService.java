package ticket.booking.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ticket.booking.entities.Train;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TrainService {

    private List<Train> trainList;

    private static final String TRAINS_PATH = "D:/My_Workspace/Java/IRCTC/app/src/main/java/ticket/booking/localDB/trains.json";

    private ObjectMapper objectMapper = new ObjectMapper();

    public TrainService() throws IOException{
        File trains = new File(TRAINS_PATH);
        trainList = objectMapper.readValue(trains, new TypeReference<List<Train>>() {});
    }

    public List<Train> searchTrains(String source, String dest) {
        return trainList.stream().filter(train -> validTrain(train, source, dest)).collect(Collectors.toList());
    }

    public Optional<Train> getTrainById(String trainId) {
        return trainList.stream()
                .filter(train -> train.getTrainId().equalsIgnoreCase(trainId))
                .findFirst();
    }

    public void addTrain(Train newTrain){
        Optional<Train> existingTrain = trainList.stream().filter(train -> train.getTrainId().equalsIgnoreCase(newTrain.getTrainId())).findFirst();

        if(existingTrain.isPresent()){
            updateTrain(newTrain);
        }else{
            trainList.add(newTrain);
            saveTrainListToFile();
        }
    }


    public void updateTrain(Train updatedTrain){
        OptionalInt index = IntStream.range(0, trainList.size())
                .filter(i -> trainList.get(i).getTrainId().equalsIgnoreCase(updatedTrain.getTrainId())).findFirst();

        if(index.isPresent()){
            trainList.set(index.getAsInt(), updatedTrain);
            saveTrainListToFile();
        }else{
            addTrain(updatedTrain);
        }
    }

    private void saveTrainListToFile(){
        try{
            objectMapper.writeValue(new File(TRAINS_PATH), trainList);
        }catch (IOException ex){
            ex.printStackTrace();
        }
    }

    private boolean validTrain(Train train, String source, String dest){
        List<String> stationOrder = train.getStations();

        int sourceIndex = stationOrder.indexOf(source.toLowerCase());
        int destIndex = stationOrder.indexOf(dest.toLowerCase());

        return sourceIndex != -1 && destIndex != -1 && sourceIndex < destIndex;
    }
}
