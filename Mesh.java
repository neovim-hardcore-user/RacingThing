import javafx.scene.Group;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.transform.*;
import javafx.animation.AnimationTimer;
import javafx.geometry.Point3D;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


public class Mesh extends Group {
    private final Map<String, PhongMaterial> materials = new HashMap<>();
    private final Map<String, List<String[]>> facesByMat = new LinkedHashMap<>();

    
    public Mesh(String objPath) throws IOException {
        List<Point3D> verts = new ArrayList<>();
        List<Point3D> norms = new ArrayList<>();
        List<Point3D> texs  = new ArrayList<>();
        
        String currentMat = "default";
        facesByMat.put(currentMat, new ArrayList<>());
        
        File objFile = new File(objPath);
        
        try (BufferedReader br = new BufferedReader(new FileReader(objFile))) {
            String line;
            
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                
                String[] tokens = line.split("\\s+");
                switch (tokens[0]) {
                    case "mtllib":
                        loadMTL(new File(objPath).getParent() + File.separator + tokens[1]);
                        break;
                    case "usemtl":
                        currentMat = tokens[1];
                        facesByMat.computeIfAbsent(currentMat, k -> new ArrayList<>());
                        break;
                    case "v":
                        verts.add(new Point3D(
                            -Double.parseDouble(tokens[1]),
                            -Double.parseDouble(tokens[2]),
                            Double.parseDouble(tokens[3])))
                        ;
                        break;
                    case "vt":
                        texs.add(new Point3D(
                            Double.parseDouble(tokens[1]),
                            Double.parseDouble(tokens[2]),0))
                        ;
                        break;
                    case "vn":
                        norms.add(new Point3D(
                            Double.parseDouble(tokens[1]),
                            Double.parseDouble(tokens[2]),
                            Double.parseDouble(tokens[3])))
                        ;
                        break;
                    case "f":
                        facesByMat.get(currentMat).add(Arrays.copyOfRange(tokens,1,tokens.length));
                        break;
                }
            }
        }
        
        for (var entry : facesByMat.entrySet())  {
            String matName = entry.getKey();
            var faceList = entry.getValue();
            if (faceList.isEmpty()) continue;
            
            TriangleMesh mesh = new TriangleMesh();
            Map<String, Integer> idxMap = new HashMap<>();
            List<Float> points = new ArrayList<>();
            List<Float> texes = new ArrayList<>();
            List<Integer> faces = new ArrayList<>();
            
            if (texs.isEmpty()) {
                texes.addAll(Arrays.asList(0f, 0f));
            }
            
            for (String[] face : faceList) {
                for (int i = 1; i < face.length - 1; i++) {
                    String[] tri = {face[0], face[i], face[i+1]};
                    for (String vDef : tri) {
                        String key = vDef;
                        int meshIdx = idxMap.computeIfAbsent(key, k -> {
                            String[] parts = k.split("/");
                            int vi = Integer.parseInt(parts[0])-1;
                            Point3D vp = verts.get(vi);
                            points.add((float)vp.getX()); points.add((float)vp.getY()); points.add((float)vp.getZ());
                            int ti=0;
                            if (parts.length>1 && !parts[1].isEmpty()) {
                                Point3D vt = texs.get(Integer.parseInt(parts[1])-1);
                                texes.add((float)vt.getX()); texes.add((float)vt.getY());
                                ti = texes.size()/2 -1;
                            }
                            return idxMap.size();
                        });
                        faces.add(meshIdx);
                        faces.add(meshIdx);
                    }
                }
            }
            
            mesh.getPoints().setAll(toFloat(points));
            mesh.getTexCoords().setAll(toFloat(texes));
            mesh.getFaces().setAll(toInt(faces));
            
            MeshView mv = new MeshView(mesh);
            mv.setMaterial(materials.getOrDefault(matName, new PhongMaterial()));
            mv.setCullFace(CullFace.BACK);
            this.getChildren().add(mv);
        }
    }
    
    private float[] toFloat(List<Float> l){float[] a=new float[l.size()];for(int i=0;i<a.length;i++)a[i]=l.get(i);return a;}
    private int[] toInt(List<Integer> l){int[] a=new int[l.size()];for(int i=0;i<a.length;i++)a[i]=l.get(i);return a;}
    
    private void loadMTL(String mtlPath) throws IOException {
        File mtlFile = new File(mtlPath);
        try (BufferedReader mr = new BufferedReader(new FileReader(mtlFile))) {
            String ml;
            PhongMaterial mat = null;
            String name = null;
            while ((ml = mr.readLine()) != null) {
                ml = ml.trim();
                if (ml.isEmpty() || ml.startsWith("#")) continue;
                String[] m = ml.split("\\s+");
                switch (m[0]) {
                    case "newmtl":
                        name = m[1];
                        mat = new PhongMaterial();
                        materials.put(name, mat);
                        break;
                    case "Ns": 
                        if (mat!=null) {
                            mat.setSpecularPower(255 / Float.parseFloat(m[1])); 
                        }
                        break;
                    case "Kd": 
                        if (mat!=null) {
                            mat.setDiffuseColor(new Color(
                                Double.parseDouble(m[1]),
                                Double.parseDouble(m[2]),
                                Double.parseDouble(m[3]),1))
                            ; 
                        }
                        break;
                    case "Ks": 
                        if (mat!=null) {
                            mat.setSpecularColor(new Color(
                                Double.parseDouble(m[1]),
                                Double.parseDouble(m[2]),
                                Double.parseDouble(m[3]),1))
                            ; 
                        }
                        break;
                }
            }
        }
    }
}
