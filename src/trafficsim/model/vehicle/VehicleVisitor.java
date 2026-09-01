package trafficsim.model.vehicle;

/**
 * Visitor pattern for the sealed {@link Vehicle} hierarchy.
 *
 * <p>Lets external code perform per-type operations without a chain of
 * {@code instanceof} checks — the compiler enforces that every concrete
 * vehicle type has a matching {@code visit…} method.
 *
 * @param <R> result type of the visit
 */
public interface VehicleVisitor<R> {
    R visitCar(Car car);
    R visitTruck(Truck truck);
    R visitBus(Bus bus);
    R visitEmergency(EmergencyVehicle emergency);
}
