import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:developer.wadu@gmail.com">Willdom Kahari</a>
 */
public final class ParkadeLevel {
    private static final Logger logger = Logger.getLogger(ParkadeLevel.class.getName());
    private final Object lock = new Object();
    private final int level;
    private final List<ParkingSpot> parkingSpots = new ArrayList<>();
    private final Map<Integer, ParkingSpot> occupiedSpots = new HashMap<>();
    private final int maximumCapacity;

    private int freeTrucks;
    private int freeCars;
    private int freeMotorcycles;

    public int getLevel() { return level; }
    public int getMaximumCapacity() { return maximumCapacity; }
    public int getFreeCars() {
        synchronized (lock) {
            return freeCars;
        }

    }
    public  int getFreeMotorcycles() {
        synchronized (lock) {
            return freeMotorcycles;
        }
    }
    public  int getFreeTrucks() {
        synchronized (lock) {
            return freeTrucks;
        }
    }
    public List<ParkingSpot> getParkingSpots() {
        synchronized (lock) {
            return parkingSpots;
        }
    }
    public  Map<Integer, ParkingSpot> getOccupiedSpots() {
        synchronized (lock) {
            return occupiedSpots;
        }
    }


    public ParkadeLevel(int level, int maximumCapacity) {
        this.level = level;
        this.maximumCapacity = maximumCapacity;
    }


    public void addParkingSpot(ParkingSpot parkingSpot) {
        synchronized (lock) {
            if (parkingSpots.size() < maximumCapacity) {
                switch (parkingSpot.getVehicleType()) {
                    case CAR -> freeCars++;
                    case MOTORCYCLE -> freeMotorcycles++;
                    case TRUCK -> freeTrucks++;
                }
                parkingSpots.add(parkingSpot);
            }
        }
    }

    public void park(Vehicle vehicle) {
        synchronized (lock) {
            try {

                ParkingSpot parkingSpot = new ParkingSpot(vehicle.vehicleId(), vehicle.vehicleType());
                parkingSpot.enter(vehicle);
                occupiedSpots.put(vehicle.vehicleId(), parkingSpot);
                switch (vehicle.vehicleType()) {
                    case CAR -> {
                        if (freeCars <= 0) {
                            throw new RuntimeException("Parking spots for cars are full");
                        }
                        freeCars--;
                    }
                    case MOTORCYCLE -> {
                        if (freeMotorcycles <= 0) {
                            throw new RuntimeException("Parking spots for motorcycles are full");
                        }
                        freeMotorcycles--;
                    }
                    case TRUCK -> {
                        if (freeTrucks <= 0) {
                            throw new RuntimeException("Parking spots for trucks are full");
                        }
                        freeTrucks--;
                    }
                }


            } catch (InterruptedException e) {
                logger.warning("Interrupted while parking vehicle");
            }
        }
    }

    public void leave(Vehicle vehicle) {
        synchronized (lock) {
            int vehicleId = vehicle.vehicleId();
            VehicleType vehicleType = vehicle.vehicleType();
            if (!occupiedSpots.containsKey(vehicleId)) {
                return;
            }
            occupiedSpots.remove(vehicleId);
            switch (vehicleType) {
                case CAR -> freeCars++;
                case MOTORCYCLE -> freeMotorcycles++;
                case TRUCK -> freeTrucks++;
            }
        }
    }

}
