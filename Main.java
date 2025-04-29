import javafx.application.Application;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Stage;
import javafx.scene.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.transform.*;
import javafx.animation.AnimationTimer;
import javafx.geometry.Point3D;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

public class Main extends Application {
    private double anchorX, anchorY;
    private double anchorAngleX = 0;
    private double anchorAngleY = 0;
    private final Rotate rotateX = new Rotate(20, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(-20, Rotate.Y_AXIS);
    private final Translate translate = new Translate(0, 0, -150);

    Car car;

    @Override
    public void start(Stage primaryStage) {
        Group world = new Group();
        world.getTransforms().addAll(rotateX, rotateY);

        AmbientLight ambient = new AmbientLight(Color.color(0.1, 0.1, 0.1));
        world.getChildren().add(ambient);

        Group lightGroup = new Group();

        PointLight redLight   = new PointLight(Color.color(0.3, 0, 0));
        PointLight greenLight = new PointLight(Color.color(0, 0.3, 0));
        PointLight blueLight  = new PointLight(Color.color(0, 0, 0.3));


        lightGroup.getChildren().addAll(redLight, greenLight, blueLight);

        world.getChildren().add(lightGroup);

        new AnimationTimer() {
            private final long startNano = System.nanoTime();
            @Override
            public void handle(long now) {
                double t = (now - startNano) / 1e9;

                redLight.setTranslateX(300 * Math.cos(t));
                redLight.setTranslateY(-500);
                redLight.setTranslateZ(300 * Math.sin(t));

                greenLight.setTranslateX(300 * Math.cos(t + 2));
                greenLight.setTranslateY(-500);
                greenLight.setTranslateZ(300 * Math.sin(t + 2));

                blueLight.setTranslateX(300 * Math.cos(t + 4));
                blueLight.setTranslateY(-500);
                blueLight.setTranslateZ(300 * Math.sin(t + 4));
            }
        }.start();

        try {
            car = new Car(
                new Point3D(0.724999, -4.63841, 95.6295), 
                "assets/Maps/TestMap/TestMap.obj",
                "assets/Models/Car/ChassisCollision.txt",
                "assets/Models/Car/Tofu_Car_Chassis.obj",
                "assets/Models/Car/Tofu_Car_Wheel.obj",
                1,
                world
            );

            Mesh a = new Mesh("assets/Maps/TestMap/TestMap.obj");
            world.getChildren().add(a);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(0.2);
        camera.setFarClip(1000.0);
        camera.setFieldOfView(60);
        camera.getTransforms().addAll(translate);

        Scene scene = new Scene(world, 1600, 900, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.GRAY);
        scene.setCamera(camera);

        scene.setOnMousePressed(event -> {
                    anchorX = event.getSceneX();
                    anchorY = event.getSceneY();
                    anchorAngleX = rotateX.getAngle();
                    anchorAngleY = rotateY.getAngle();
            });

        scene.setOnMouseDragged(event -> {
                    rotateX.setAngle(anchorAngleX + (event.getSceneY() - anchorY) * 0.2);
                    rotateY.setAngle(anchorAngleY - (event.getSceneX() - anchorX) * 0.2);
            });

        scene.addEventHandler(ScrollEvent.SCROLL, (ScrollEvent ev) -> {
                    double delta = ev.getDeltaY();
                    translate.setZ(translate.getZ() + delta * 0.1);
            });

        primaryStage.setScene(scene);
        primaryStage.setTitle("Racing Game");
        primaryStage.show();

        new AnimationTimer() {
            @Override public void handle(long now) {
                for (int step = 0; step < 1; step++) {
                    car.update();
                }
            }
        }.start();
    }
}
