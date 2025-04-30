import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class ParkingSpotTest {

    private ParkingSpot parkingSpot;
    private Vehicle mockVehicle;

    @BeforeEach
    void setUp() {
        parkingSpot = new ParkingSpot(1, VehicleType.CAR);
        mockVehicle = mock(Vehicle.class);
        when(mockVehicle.vehicleType()).thenReturn(VehicleType.CAR);
    }


    @Test
    @DisplayName("Constructor should initialize fields correctly")
    void constructor_InitializesFieldsCorrectly() {
        assertEquals(1, parkingSpot.getId());
        assertEquals(VehicleType.CAR, parkingSpot.getVehicleType());
        assertFalse(parkingSpot.isOccupied());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100, Integer.MAX_VALUE})
    @DisplayName("Constructor should accept valid IDs")
    void constructor_AcceptsValidIds(int id) {
        ParkingSpot spot = new ParkingSpot(id, VehicleType.CAR);
        assertEquals(id, spot.getId());
    }

    @ParameterizedTest
    @EnumSource(VehicleType.class)
    @DisplayName("Constructor should accept all vehicle types")
    void constructor_AcceptsAllVehicleTypes(VehicleType type) {
        ParkingSpot spot = new ParkingSpot(1, type);
        assertEquals(type, spot.getVehicleType());
    }


    @Test
    @DisplayName("getId should return correct ID")
    void getId_ReturnsCorrectId() {
        assertEquals(1, parkingSpot.getId());
    }


    @Test
    @DisplayName("getVehicleType should return correct type")
    void getVehicleType_ReturnsCorrectType() {
        assertEquals(VehicleType.CAR, parkingSpot.getVehicleType());
    }


    @Test
    @DisplayName("isOccupied should return false when spot is free")
    void isOccupied_ReturnsFalseWhenFree() {
        assertFalse(parkingSpot.isOccupied());
    }

    @Test
    @DisplayName("isOccupied should return true when spot is occupied")
    void isOccupied_ReturnsTrueWhenOccupied() throws InterruptedException {
        parkingSpot.enter(mockVehicle);
        assertTrue(parkingSpot.isOccupied());
    }


    @Test
    @DisplayName("enter should throw IllegalArgumentException for wrong vehicle type")
    void enter_ThrowsForWrongVehicleType() {
        Vehicle wrongTypeVehicle = mock(Vehicle.class);
        when(wrongTypeVehicle.vehicleType()).thenReturn(VehicleType.MOTORCYCLE);

        assertThrows(IllegalArgumentException.class, () -> parkingSpot.enter(wrongTypeVehicle));
        assertFalse(parkingSpot.isOccupied());
    }

    @Test
    @DisplayName("enter should set occupied to true when spot is free")
    void enter_SetsOccupiedWhenFree() throws InterruptedException {
        parkingSpot.enter(mockVehicle);
        assertTrue(parkingSpot.isOccupied());
    }

    @Test
    @DisplayName("enter should block when spot is occupied")
    void enter_BlocksWhenOccupied() throws InterruptedException {

        parkingSpot.enter(mockVehicle);


        Thread secondVehicleThread = new Thread(() -> {
            assertThrows(RuntimeException.class, () -> parkingSpot.enter(mockVehicle));
        });
        secondVehicleThread.start();


        Thread.sleep(100);
        assertEquals(Thread.State.WAITING, secondVehicleThread.getState());


        parkingSpot.exit();
        secondVehicleThread.join();
    }


    @Test
    @DisplayName("exit should set occupied to false")
    void exit_SetsOccupiedToFalse() throws InterruptedException {
        parkingSpot.enter(mockVehicle);
        parkingSpot.exit();
        assertFalse(parkingSpot.isOccupied());
    }

    @Test
    @DisplayName("exit should notify waiting threads")
    void exit_NotifiesWaitingThreads() throws InterruptedException {

        parkingSpot.enter(mockVehicle);


        CountDownLatch latch = new CountDownLatch(1);
        Thread secondVehicleThread = new Thread(() -> {
            assertDoesNotThrow(() -> parkingSpot.enter(mockVehicle));
            latch.countDown();
        });
        secondVehicleThread.start();


        Thread.sleep(100);
        assertEquals(Thread.State.WAITING, secondVehicleThread.getState());


        parkingSpot.exit();


        assertTrue(latch.await(1, TimeUnit.SECONDS));
        secondVehicleThread.join();
    }

    // Test thread safety
    @Test
    @DisplayName("should handle concurrent access correctly")
    void concurrentAccess_HandledCorrectly() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    parkingSpot.enter(mockVehicle);
                    Thread.sleep(10);
                    parkingSpot.exit();
                    latch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();
        assertFalse(parkingSpot.isOccupied());
    }

    @Test
    @DisplayName("should maintain consistency under high contention")
    void highContention_MaintainsConsistency() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        parkingSpot.enter(mockVehicle);
                        assertTrue(parkingSpot.isOccupied());
                        parkingSpot.exit();
                    }
                    latch.countDown();
                } catch (Exception e) {
                    fail("Unexpected exception: " + e);
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        assertFalse(parkingSpot.isOccupied());
    }


    @Test
    @DisplayName("enter should be interruptible while waiting")
    void enter_IsInterruptibleWhileWaiting() throws InterruptedException {
        // First vehicle enters
        parkingSpot.enter(mockVehicle);

        // Second vehicle tries to enter in separate thread
        Thread secondVehicleThread = new Thread(() -> {
            assertThrows(RuntimeException.class, () -> parkingSpot.enter(mockVehicle));
        });
        secondVehicleThread.start();

        // Wait briefly to ensure thread is blocked
        Thread.sleep(100);
        secondVehicleThread.interrupt();
        secondVehicleThread.join();
    }

    @Test
    @DisplayName("id should be immutable")
    void id_IsImmutable() throws Exception {
        var idField = ParkingSpot.class.getDeclaredField("id");
        assertTrue(java.lang.reflect.Modifier.isFinal(idField.getModifiers()));
    }

    @Test
    @DisplayName("vehicleType should be immutable")
    void vehicleType_IsImmutable() throws Exception {
        var vehicleTypeField = ParkingSpot.class.getDeclaredField("vehicleType");
        assertTrue(java.lang.reflect.Modifier.isFinal(vehicleTypeField.getModifiers()));
    }

    @Test
    @DisplayName("lock should be immutable")
    void lock_IsImmutable() throws Exception {
        var lockField = ParkingSpot.class.getDeclaredField("lock");
        assertTrue(java.lang.reflect.Modifier.isFinal(lockField.getModifiers()));
    }
}
