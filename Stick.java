import javafx.geometry.Point3D;

public class Stick {
    private final CollisionSphere a, b;
    private final double restLength;

    public Stick(CollisionSphere a, CollisionSphere b) {
        this.a = a;
        this.b = b;
        this.restLength = a.pos.distance(b.pos);
    }


    public void constrain() {
        Point3D delta = b.pos.subtract(a.pos);
        double currentLen = delta.magnitude();
        double diff = (currentLen - restLength) / currentLen;
        Point3D offset = delta.multiply(0.5 * diff);
        a.pos = a.pos.add(offset);
        b.pos = b.pos.subtract(offset);
    }
}