import javafx.geometry.Point3D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CollisionMesh {
    private final List<Point3D[]> triangles = new ArrayList<>();
    private final BvhNode bvhRoot;

    public CollisionMesh(String objPath, int maxDepth) throws IOException {
        List<Point3D> vertices = new ArrayList<>();
        File objFile = new File(objPath);
        try (BufferedReader br = new BufferedReader(new FileReader(objFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] tok = line.split("\\s+");
                switch (tok[0]) {
                    case "v":
                        double x = -Double.parseDouble(tok[1]);
                        double y = -Double.parseDouble(tok[2]);
                        double z = Double.parseDouble(tok[3]);
                        vertices.add(new Point3D(x, y, z));
                        break;
                    case "f":
                        int faceVerts = tok.length - 1;
                        int[] idx = new int[faceVerts];
                        for (int j = 0; j < faceVerts; j++) {
                            idx[j] = parseIndex(tok[j + 1]);
                        }
                        for (int j = 1; j < faceVerts - 1; j++) {
                            Point3D a = vertices.get(idx[0]);
                            Point3D b = vertices.get(idx[j]);
                            Point3D c = vertices.get(idx[j + 1]);
                            triangles.add(new Point3D[]{ a, b, c });
                        }
                        break;
                }
            }
        }

        this.bvhRoot = new BvhNode(triangles, maxDepth);
    }

    private int parseIndex(String token) {
        String[] parts = token.split("/");
        return Integer.parseInt(parts[0]) - 1;
    }

    public BvhNode getBvhRoot() {
        return bvhRoot;
    }
}
