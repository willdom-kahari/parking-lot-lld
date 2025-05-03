import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:developer.wadu@gmail.com">Willdom Kahari</a>
 */
public final class ParkingLot {
    private final List<ParkadeLevel> parkadeLevels = new ArrayList<>();

    private ParkingLot() {
    }

    private void initializeParkadeLevels(int levels, int levelCapacity){
        for ( int i = 1; i <= levels; i++ ) {
            ParkadeLevel parkadeLevel = new ParkadeLevel(i, levelCapacity);
            parkadeLevels.add(parkadeLevel);
        }
    }

    public static ParkingLot getInstance(int levels, int levelCapacity){
        ParkingLot instance = ParkingLotHolder.INSTANCE;
        if (!instance.parkadeLevels.isEmpty()) {
            return instance;
        }

        instance.initializeParkadeLevels(levels, levelCapacity);
        return instance;
    }

    static class ParkingLotHolder {
        static final ParkingLot INSTANCE = new ParkingLot();
    }
}
