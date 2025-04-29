import javafx.geometry.Point3D;
import java.util.*;

public class BvhNode {
    double 
        bpx = Double.NEGATIVE_INFINITY, 
        bnx = Double.POSITIVE_INFINITY, 
        bpy = Double.NEGATIVE_INFINITY, 
        bny = Double.POSITIVE_INFINITY, 
        bpz = Double.NEGATIVE_INFINITY, 
        bnz = Double.POSITIVE_INFINITY
    ;
        
    boolean isLeaf;
    BvhNode left, right;
    List<Point3D[]> tris;
    
    public BvhNode(List<Point3D[]> list, int maxDepth) {
        tris = new ArrayList<>(list);
        tris.forEach(t->expandByTriangle(t));
        
        if (tris.size() > maxDepth) {
            isLeaf = false;
            int axis = longestAxis();
            tris.sort(Comparator.comparingDouble(t->centroid(t, axis)));
            int mid = tris.size() / 2;
            left = new BvhNode(tris.subList(0, mid), maxDepth);
            right = new BvhNode(tris.subList(mid, tris.size()), maxDepth);
            tris = null;
        } else {
            isLeaf = true;
        }
    }
    
    static double centroid(Point3D[] t, int axis) {
        switch(axis) {
            case 0:
                return (t[0].getX() + t[1].getX() + t[2].getX()) / 3.0;
            case 1:
                return (t[0].getY() + t[1].getY() + t[2].getY()) / 3.0;
            default: 
                return (t[0].getZ() + t[1].getZ() + t[2].getZ()) / 3.0;
        }
    }
    
    int longestAxis() {
        double 
            dx = bpx - bnx, 
            dy = bpy - bny, 
            dz = bpz - bnz
        ;
        
        return dx > dy && dx > dz ? 0 : dy > dz ? 1 : 2;
    }
    
    void expandByTriangle(Point3D[] tri) {
        for (Point3D v:tri) {
            bpx = Math.max(bpx, v.getX());
            bnx = Math.min(bnx, v.getX());
            
            bpy = Math.max(bpy, v.getY());
            bny = Math.min(bny, v.getY());
            
            bpz = Math.max(bpz, v.getZ());
            bnz = Math.min(bnz, v.getZ());
        }
    }
    
    boolean AABBSphereTest(Point3D s, double r) {
        if (s.getX() > bpx + r) return false;
        if (s.getX() < bnx - r) return false;
        
        if (s.getY() > bpy + r) return false;
        if (s.getY() < bny - r) return false;
        
        if (s.getZ() > bpz + r) return false;
        if (s.getZ() < bnz - r) return false;
        
        return true;
    }
}
