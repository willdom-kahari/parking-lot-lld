import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ParkadeLevelTest {

    private ParkadeLevel parkadeLevel;
    private Vehicle mockVehicle;
    private ParkingSpot mockParkingSpot;

    @BeforeEach
    void setUp() {
        parkadeLevel = new ParkadeLevel(1, 10);
        mockVehicle = mock(Vehicle.class);
        mockParkingSpot = mock(ParkingSpot.class);
    }

    // Constructor tests
    @Test
    @DisplayName("Constructor should initialize fields correctly")
    void constructor_InitializesFieldsCorrectly() {
        assertEquals(1, parkadeLevel.getLevel());
        assertEquals(10, parkadeLevel.getMaximumCapacity());
        assertEquals(0, parkadeLevel.getFreeCars());
        assertEquals(0, parkadeLevel.getFreeMotorcycles());
        assertEquals(0, parkadeLevel.getFreeTrucks());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, Integer.MAX_VALUE})
    @DisplayName("Constructor should accept valid levels")
    void constructor_AcceptsValidLevels(int level) {
        ParkadeLevel pl = new ParkadeLevel(level, 10);
        assertEquals(level, pl.getLevel());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100, Integer.MAX_VALUE})
    @DisplayName("Constructor should accept valid capacities")
    void constructor_AcceptsValidCapacities(int capacity) {
        ParkadeLevel pl = new ParkadeLevel(1, capacity);
        assertEquals(capacity, pl.getMaximumCapacity());
    }

    // addParkingSpot tests
    @Test
    @DisplayName("addParkingSpot should add car spot and increment freeCars")
    void addParkingSpot_AddsCarSpot() {
        when(mockParkingSpot.getVehicleType()).thenReturn(VehicleType.CAR);
        parkadeLevel.addParkingSpot(mockParkingSpot);
        assertEquals(1, parkadeLevel.getFreeCars());
    }

    @Test
    @DisplayName("addParkingSpot should add motorcycle spot and increment freeMotorcycles")
    void addParkingSpot_AddsMotorcycleSpot() {
        when(mockParkingSpot.getVehicleType()).thenReturn(VehicleType.MOTORCYCLE);
        parkadeLevel.addParkingSpot(mockParkingSpot);
        assertEquals(1, parkadeLevel.getFreeMotorcycles());
    }

    @Test
    @DisplayName("addParkingSpot should add truck spot and increment freeTrucks")
    void addParkingSpot_AddsTruckSpot() {
        when(mockParkingSpot.getVehicleType()).thenReturn(VehicleType.TRUCK);
        parkadeLevel.addParkingSpot(mockParkingSpot);
        assertEquals(1, parkadeLevel.getFreeTrucks());
    }


    // park tests
    @Test
    @DisplayName("park should succeed when space is available")
    void park_SucceedsWhenSpaceAvailable() {
        when(mockVehicle.vehicleType()).thenReturn(VehicleType.CAR);
        parkadeLevel.addParkingSpot(new ParkingSpot(1, VehicleType.CAR));

        parkadeLevel.park(mockVehicle);

        assertEquals(0, parkadeLevel.getFreeCars());
        assertEquals(1, parkadeLevel.getOccupiedSpots().size());
    }

    @ParameterizedTest
    @EnumSource(VehicleType.class)
    @DisplayName("park should throw when no spots available for vehicle type")
    void park_ThrowsWhenNoSpotsAvailable(VehicleType type) {
        when(mockVehicle.vehicleType()).thenReturn(type);

        assertThrows(RuntimeException.class, () -> parkadeLevel.park(mockVehicle));
    }

    @Test
    @DisplayName("park should handle interrupted exception")
    void park_HandlesInterruptedException() throws InterruptedException {
        ParkingSpot spot = mock(ParkingSpot.class);
        when(spot.getVehicleType()).thenReturn(VehicleType.CAR);
        doThrow(new RuntimeException("Test interruption")).when(spot).enter(any());

        parkadeLevel.addParkingSpot(spot);

        assertThrows(RuntimeException.class, () -> parkadeLevel.park(mockVehicle));
        // Verify logger.warning was called
    }

    // leave tests
    @Test
    @DisplayName("leave should free up car spot")
    void leave_FreesCarSpot() {
        when(mockVehicle.vehicleType()).thenReturn(VehicleType.CAR);
        when(mockVehicle.vehicleId()).thenReturn(1);

        ParkingSpot spot = new ParkingSpot(1, VehicleType.CAR);
        parkadeLevel.addParkingSpot(spot);
        parkadeLevel.park(mockVehicle);

        parkadeLevel.leave(mockVehicle);

        assertEquals(1, parkadeLevel.getFreeCars());
        assertEquals(0, parkadeLevel.getOccupiedSpots().size());
    }

    @Test
    @DisplayName("leave should free up motorcycle spot")
    void leave_FreesMotorcycleSpot() {
        when(mockVehicle.vehicleType()).thenReturn(VehicleType.MOTORCYCLE);
        when(mockVehicle.vehicleId()).thenReturn(1);

        ParkingSpot spot = new ParkingSpot(1, VehicleType.MOTORCYCLE);
        parkadeLevel.addParkingSpot(spot);
        parkadeLevel.park(mockVehicle);

        parkadeLevel.leave(mockVehicle);

        assertEquals(1, parkadeLevel.getFreeMotorcycles());
        assertEquals(0, parkadeLevel.getOccupiedSpots().size());
    }

    @Test
    @DisplayName("leave should free up truck spot")
    void leave_FreesTruckSpot() {
        when(mockVehicle.vehicleType()).thenReturn(VehicleType.TRUCK);
        when(mockVehicle.vehicleId()).thenReturn(1);

        ParkingSpot spot = new ParkingSpot(1, VehicleType.TRUCK);
        parkadeLevel.addParkingSpot(spot);
        parkadeLevel.park(mockVehicle);

        parkadeLevel.leave(mockVehicle);

        assertEquals(1, parkadeLevel.getFreeTrucks());
        assertEquals(0, parkadeLevel.getOccupiedSpots().size());
    }

    @Test
    @DisplayName("leave should handle unknown vehicle ID")
    void leave_HandlesUnknownVehicleId() {
        when(mockVehicle.vehicleId()).thenReturn(999);

        assertDoesNotThrow(() -> parkadeLevel.leave(mockVehicle));
    }

    // Thread safety tests
    @Test
    @DisplayName("should handle concurrent parking operations")
    void concurrentParking_HandledCorrectly() {
        int threadCount = 10;
        // Add enough spots for all threads
        for (int i = 0; i < threadCount; i++) {
            parkadeLevel.addParkingSpot(new ParkingSpot(i+1, VehicleType.CAR));
        }


        for (int i = 0; i < threadCount; i++) {
            final int vehicleId = i + 1;
            Thread thread = new Thread(() -> {
                Vehicle vehicle = mock(Vehicle.class);
                when(vehicle.vehicleType()).thenReturn(VehicleType.CAR);
                when(vehicle.vehicleId()).thenReturn(vehicleId);

                parkadeLevel.park(vehicle);
                parkadeLevel.leave(vehicle);

            });
            thread.start();

        }

        assertEquals(threadCount, parkadeLevel.getFreeCars());
    }

    @Test
    @DisplayName("should maintain consistency under high contention")
    void highContention_MaintainsConsistency() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Add limited spots to create contention
        for (int i = 0; i < 5; i++) {
            parkadeLevel.addParkingSpot(new ParkingSpot(i+1, VehicleType.CAR));
        }

        for (int i = 0; i < threadCount; i++) {
            final int vehicleId = i + 1;
            executor.execute(() -> {
                try {
                    Vehicle vehicle = mock(Vehicle.class);
                    when(vehicle.vehicleType()).thenReturn(VehicleType.CAR);
                    when(vehicle.vehicleId()).thenReturn(vehicleId);

                    parkadeLevel.park(vehicle);
                    Thread.sleep(10); // Simulate work
                    parkadeLevel.leave(vehicle);
                } catch (Exception e) {
                    // Expected that some threads will get full exception
                }
                latch.countDown();
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        assertEquals(5, parkadeLevel.getFreeCars());
    }

    // Edge case tests
    @Test
    @DisplayName("should handle maximum capacity correctly")
    void maximumCapacity_HandledCorrectly() {
        ParkadeLevel smallLevel = new ParkadeLevel(1, 1);
        smallLevel.addParkingSpot(new ParkingSpot(1, VehicleType.CAR));

        ParkingSpot anotherSpot = new ParkingSpot(2, VehicleType.TRUCK);
        smallLevel.addParkingSpot(anotherSpot);

        assertEquals(1, smallLevel.getParkingSpots().size());
    }
}
