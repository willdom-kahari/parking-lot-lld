/**
 * @author <a href="mailto:developer.wadu@gmail.com">Willdom Kahari</a>
 */
public final class ParkingSpot {
    private final Object lock = new Object();
    private final int id;
    private final VehicleType vehicleType;
    private boolean occupied;
    public ParkingSpot(int id, VehicleType vehicleType) {
        this.id = id;
        this.vehicleType = vehicleType;
        this.occupied = false;
    }
    public int getId() {
        return id;
    }
    public VehicleType getVehicleType() {
        return vehicleType;
    }
    public boolean isOccupied() {
        synchronized (lock){
            return occupied;
        }
    }
    public void enter(Vehicle vehicle) throws InterruptedException {
        synchronized (lock){
            if (vehicle.vehicleType() != vehicleType) {
                throw new IllegalArgumentException("Vehicle type mismatch");
            }
            while (occupied){
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
            this.occupied = true;
        }

    }
    public void exit() {
        synchronized (lock){
            this.occupied = false;
            lock.notifyAll();
        }
    }
}
