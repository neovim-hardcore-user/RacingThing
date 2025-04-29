import javafx.geometry.Point3D;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

public class Car {
    private final CollisionSphere[] chassisCollision;
    private final Mesh chassisMesh;
    private final CollisionMesh collisionMesh;
    private final List<Stick> sticks = new ArrayList<>();
    private final List<Sphere> sphereViews = new ArrayList<>();

    private final Point3D initialCenter;
    private final List<Point3D> initialOffsets;

    public Car(Point3D startPos,
               String collisionObjPath,
               String collisionDataPath,
               String chassisObjPath,
               String wheelObjPath,
               int bvhLeafThreshold,
               Group world) throws IOException {
        chassisMesh = new Mesh(chassisObjPath);
        world.getChildren().add(chassisMesh);
        
        collisionMesh = new CollisionMesh(collisionObjPath, bvhLeafThreshold);

        List<double[]> vertexData = new ArrayList<>();
        List<int[]> connections = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(collisionDataPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("v ")) {
                    String[] t = line.split("\\s+");
                    vertexData.add(new double[]{
                        -Double.parseDouble(t[1]),
                        -Double.parseDouble(t[3]),
                         Double.parseDouble(t[2]),
                         Double.parseDouble(t[4])
                    });
                } else if (line.startsWith("c ")) {
                    String[] t = line.split("\\s+");
                    connections.add(new int[]{
                        Integer.parseInt(t[1]),
                        Integer.parseInt(t[2])
                    });
                }
            }
        }


        List<CollisionSphere> spheres = new ArrayList<>();
        Point3D sum = new Point3D(0,0,0);
        for (double[] v : vertexData) {
            CollisionSphere cs = new CollisionSphere(
                startPos.add(new Point3D(v[0], v[1], v[2])), v[3]
            );
            spheres.add(cs);
            sum = sum.add(cs.pos);
        }
        initialCenter = sum.multiply(1.0 / spheres.size());

        
        initialOffsets = new ArrayList<>();
        for (CollisionSphere cs : spheres) {
            initialOffsets.add(cs.pos.subtract(sum).add(new Point3D(0, 0, 0)));
        }


        for (CollisionSphere cs : spheres) {
            Sphere view = new Sphere(cs.radius);
            PhongMaterial mat = new PhongMaterial(Color.RED);
            mat.setSpecularColor(Color.ORANGE);
            view.setMaterial(mat);
            world.getChildren().add(view);
            sphereViews.add(view);
        }
        chassisCollision = spheres.toArray(new CollisionSphere[0]);


        for (int[] c : connections) {
            sticks.add(new Stick(
                spheres.get(c[0]), spheres.get(c[1])
            ));
        }
    }

    public void update() {
        for (CollisionSphere s : chassisCollision) {
            s.applyForce(new Point3D(0, 0.0001, 0));
            s.verlet();
            s.collideMeshBVH(collisionMesh.getBvhRoot());
        }
        Random rng = new Random();
        for (int i = 0; i < 200; i++) {
            Collections.shuffle(sticks, rng);
            for (Stick stick : sticks) stick.constrain();
        }


        Point3D sum = new Point3D(0,0,0);
        List<Point3D> currentOffsets = new ArrayList<>();
        for (CollisionSphere s : chassisCollision) {
            sum = sum.add(s.pos);
        }
        Point3D currentCenter = sum.multiply(1.0 / chassisCollision.length);
        for (CollisionSphere s : chassisCollision) {
            currentOffsets.add(s.pos.subtract(currentCenter));
        }

        double[] quat = computeKabschQuaternion(initialOffsets, currentOffsets);
        
        double w = quat[0], x = quat[1], y = quat[2], z = quat[3];
        double angle = 2 * Math.acos(w);
        double s = Math.sqrt(1 - w*w);
        Point3D axis = (s < 1e-6)
            ? new Point3D(1,0,0)
            : new Point3D(x/s, y/s, z/s);


        chassisMesh.getTransforms().clear();
        Rotate rot = new Rotate(
            Math.toDegrees(angle),
            currentCenter.getX(),
            currentCenter.getY(),
            currentCenter.getZ(),
            axis
        );
        Translate tr = new Translate(
            currentCenter.getX(),
            currentCenter.getY(),
            currentCenter.getZ()
        );
        chassisMesh.getTransforms().addAll(rot, tr);


        for (int i = 0; i < sphereViews.size(); i++) {
            Sphere v = sphereViews.get(i);
            Point3D p = chassisCollision[i].pos;
            v.setTranslateX(p.getX());
            v.setTranslateY(p.getY());
            v.setTranslateZ(p.getZ());
        }
    }

    private double[] computeKabschQuaternion(
        List<Point3D> P, List<Point3D> Q
    ) {
        int n = P.size();
        double Sxx=0, Sxy=0, Sxz=0;
        double Syx=0, Syy=0, Syz=0;
        double Szx=0, Szy=0, Szz=0;
        for (int i = 0; i < n; i++) {
            Point3D p = P.get(i), q = Q.get(i);
            Sxx += p.getX()*q.getX(); Sxy += p.getX()*q.getY(); Sxz += p.getX()*q.getZ();
            Syx += p.getY()*q.getX(); Syy += p.getY()*q.getY(); Syz += p.getY()*q.getZ();
            Szx += p.getZ()*q.getX(); Szy += p.getZ()*q.getY(); Szz += p.getZ()*q.getZ();
        }
        double[][] K = new double[4][4];
        K[0][0]=Sxx+Syy+Szz; K[0][1]=Syz-Szy;   K[0][2]=Szx-Sxz;   K[0][3]=Sxy-Syx;
        K[1][0]=K[0][1];      K[1][1]=Sxx-Syy-Szz; K[1][2]=Sxy+Syx;   K[1][3]=Szx+Sxz;
        K[2][0]=K[0][2];      K[2][1]=K[1][2];      K[2][2]=-Sxx+Syy-Szz;K[2][3]=Syz+Szy;
        K[3][0]=K[0][3];      K[3][1]=K[1][3];      K[3][2]=K[2][3];      K[3][3]=-Sxx-Syy+Szz;
        double[] q = {1,0.1,0.1,0.1};
        for (int it=0; it<10000; it++) {
            double[] q2 = new double[4];
            for (int r=0; r<4; r++) for (int c=0; c<4; c++) q2[r]+=K[r][c]*q[c];
            double norm=0; for(double v:q2) norm+=v*v; norm=Math.sqrt(norm);
            for(int i=0;i<4;i++) q[i]=q2[i]/norm;
        }
        return q;
    }
}
