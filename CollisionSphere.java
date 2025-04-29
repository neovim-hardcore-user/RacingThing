import javafx.geometry.Point3D;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CollisionSphere {
    public Point3D pos;
    private Point3D prevPos;
    public final double radius;

    public CollisionSphere(Point3D initialPos, double radius) {
        this.pos = initialPos;
        this.prevPos = initialPos;
        this.radius = radius;
    }

    public void applyForce(Point3D force) {
        pos = pos.add(force);
    }

    public void verlet() {
        Point3D velocity = pos.subtract(prevPos).multiply(0.999);
        Point3D next = pos.add(velocity);
        
        double minX = -200, maxX = 200;
        double minY = -200, maxY = 100;
        double minZ = -200, maxZ = 200;

        double nextX = Math.max(minX, Math.min(maxX, next.getX()));
        double nextY = Math.max(minY, Math.min(maxY, next.getY()));
        double nextZ = Math.max(minZ, Math.min(maxZ, next.getZ()));

        prevPos = pos;
        pos = new Point3D(nextX, nextY, nextZ);
    }

    void collideMeshBVH(BvhNode node) {
        if(!node.AABBSphereTest(pos,radius)) {
            return;
        }
        if(node.isLeaf) {
            for(Point3D[] tri: node.tris) {
                Point3D c=closestPointOnTriangle(pos,tri[0],tri[1],tri[2]);
                double dist=pos.distance(c);
                if(dist<radius) {
                    Point3D n=pos.subtract(c).normalize(); 
                    pos=pos.add(n.multiply(radius-dist));
                }
            }
        } else {
            collideMeshBVH(node.left); collideMeshBVH(node.right);
        }
    }

    private static Point3D closestPointOnTriangle(Point3D p, Point3D a, Point3D b, Point3D c) {
        Point3D ab = b.subtract(a), ac = c.subtract(a), ap = p.subtract(a);
        double d1 = ab.dotProduct(ap), d2 = ac.dotProduct(ap);
        if (d1 <= 0 && d2 <= 0) return a;
        Point3D bp = p.subtract(b);
        double d3 = ab.dotProduct(bp), d4 = ac.dotProduct(bp);
        if (d3 >= 0 && d4 <= d3) return b;
        double vc = d1 * d4 - d3 * d2;
        if (vc <= 0 && d1 >= 0 && d3 <= 0) {
            double v = d1 / (d1 - d3);
            return a.add(ab.multiply(v));
        }
        Point3D cp = p.subtract(c);
        double d5 = ab.dotProduct(cp), d6 = ac.dotProduct(cp);
        if (d6 >= 0 && d5 <= d6) return c;
        double vb = d5 * d2 - d1 * d6;
        if (vb <= 0 && d2 >= 0 && d6 <= 0) {
            double w = d2 / (d2 - d6);
            return a.add(ac.multiply(w));
        }
        double va = d3 * d6 - d5 * d4;
        if (va <= 0 && (d4 - d3) >= 0 && (d5 - d6) >= 0) {
            double w = (d4 - d3) / ((d4 - d3) + (d5 - d6));
            return b.add(c.subtract(b).multiply(w));
        }
        double denom = 1.0 / (va + vb + vc);
        double v = vb * denom, w = vc * denom;
        return a.add(ab.multiply(v)).add(ac.multiply(w));
    }
}