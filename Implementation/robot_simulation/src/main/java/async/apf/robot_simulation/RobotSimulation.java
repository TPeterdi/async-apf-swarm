package async.apf.robot_simulation;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RobotSimulation extends Application {
    private boolean simulationRunning = false; // Jelzi, hogy fut-e a szimuláció
    private List<Circle> robots = new ArrayList<>(); // Robotokat reprezentáló körök
    private List<Circle> points = new ArrayList<>(); // Célpontokat reprezentáló pontok
    private List<Circle> startPoints = new ArrayList<>(); // Kezdeti pontok piros körök
    private List<Line> lines = new ArrayList<>(); // Vonalak a robotok útvonalához

    // Robotok színei
    private Color[] robotColors = {Color.BLUE, Color.GREEN, Color.ORANGE, Color.PURPLE, Color.YELLOW};

    // UI elemek
    private TextField robotInput;
    private TextField pointInput;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Robotok mozgásának szimulációja");

        Pane root = new Pane();

        // Robotok számának input mezője
        Label robotLabel = new Label("Robotok száma:");
        robotLabel.setLayoutX(10);
        robotLabel.setLayoutY(10);
        root.getChildren().add(robotLabel);

        robotInput = new TextField();
        robotInput.setLayoutX(150);
        robotInput.setLayoutY(10);
        root.getChildren().add(robotInput);

        // Célpontok számának input mezője
        Label pointLabel = new Label("Célpontok száma:");
        pointLabel.setLayoutX(10);
        pointLabel.setLayoutY(40);
        root.getChildren().add(pointLabel);

        pointInput = new TextField();
        pointInput.setLayoutX(150);
        pointInput.setLayoutY(40);
        root.getChildren().add(pointInput);

        Button btn = new Button();
        btn.setText("Start szimuláció");
        btn.setLayoutX(150);
        btn.setLayoutY(80);
        root.getChildren().add(btn);

        // Gombnyomás eseménykezelő
        btn.setOnAction((ActionEvent event) -> {
            if (!simulationRunning) {
                // Reset előtt töröljük a vonalakat és a robotokat
                resetSimulation(root);
                int robotCount = Integer.parseInt(robotInput.getText());
                int pointCount = Integer.parseInt(pointInput.getText());

                // Célpontok és robotok létrehozása a megadott számok alapján
                createPoints(root, pointCount);
                createRobotsAndStartPoints(root, robotCount);

                simulationRunning = true;
                btn.setText("Szimuláció folyamatban...");

                // Indítjuk az animációt
                startSimulation(root);

                // Az animáció befejeződése után állítsuk vissza a gombot
                Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
                    simulationRunning = false;
                    btn.setText("Újraindítás");
                }));
                timeline.play();
            } else {
                // Újraindítás
                resetSimulation(root);
                btn.setText("Start szimuláció");
            }
        });

        Scene scene = new Scene(root, 400, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Robotok és induló pontok létrehozása
    private void createRobotsAndStartPoints(Pane root, int robotCount) {
        for (int i = 0; i < robotCount; i++) {
            // Kezdeti pont létrehozása (piros kör)
            Circle startPoint = new Circle(5, Color.RED);
            startPoint.setCenterX(50 + i * 100);
            startPoint.setCenterY(50);
            startPoints.add(startPoint);
            root.getChildren().add(startPoint);

            // Robot létrehozása
            Circle robot = new Circle(10, robotColors[i % robotColors.length]);  // Robotok különböző színűek
            robot.setCenterX(startPoint.getCenterX());
            robot.setCenterY(startPoint.getCenterY());
            robots.add(robot);
            root.getChildren().add(robot);
        }
    }

    // Célpontok létrehozása
    private void createPoints(Pane root, int pointCount) {
        points.clear();  // Kiürítjük a korábbi pontokat
        for (int i = 0; i < pointCount; i++) {
            Circle point = new Circle(5, Color.RED);
            point.setCenterX(50 + (i % 3) * 100);  // Módosított x koordináta
            point.setCenterY(300 + (i / 3) * 50);  // Módosított y koordináta
            points.add(point);
            root.getChildren().add(point);
        }
    }

    // Szimuláció elindítása
    private void startSimulation(Pane root) {
        // Véletlenszerű célpontok kiosztása a robotok számára
        List<Circle> shuffledPoints = new ArrayList<>(points);
        Collections.shuffle(shuffledPoints);  // Véletlenszerűsítjük a célpontokat

        Set<Circle> assignedPoints = new HashSet<>();  // Követjük a már lefoglalt pontokat

        for (int i = 0; i < robots.size(); i++) {
            Circle robot = robots.get(i);
            Circle point = null;

            // Keresünk egy nem foglalt pontot a robot számára
            for (Circle shuffledPoint : shuffledPoints) {
                if (!assignedPoints.contains(shuffledPoint)) {
                    point = shuffledPoint;
                    assignedPoints.add(point);  // Lefoglaljuk a pontot
                    break; // Kilépünk, ha találtunk egy szabad pontot
                }
            }

            if (point != null) {
                // Útvonalat jelző vonal létrehozása
                Line line = new Line();
                line.setStartX(robot.getCenterX());
                line.setStartY(robot.getCenterY());
                line.setEndX(robot.getCenterX());
                line.setEndY(robot.getCenterY());
                line.setStroke(robotColors[i % robotColors.length]);  // A vonal színe a robot színével megegyező
                line.setStrokeWidth(2);

                lines.add(line);
                root.getChildren().add(line);

                Timeline timeline = new Timeline();

                // Animáljuk a robot X és Y pozícióit a célpont felé
                KeyValue kvX = new KeyValue(robot.centerXProperty(), point.getCenterX());
                KeyValue kvY = new KeyValue(robot.centerYProperty(), point.getCenterY());

                // Animáljuk a vonal végpontját is a robot mozgásával együtt
                KeyValue lineEndX = new KeyValue(line.endXProperty(), point.getCenterX());
                KeyValue lineEndY = new KeyValue(line.endYProperty(), point.getCenterY());

                // Kulcskeretek beállítása: 5 másodperc alatt érjék el a célpontokat
                KeyFrame kf = new KeyFrame(Duration.seconds(5), kvX, kvY, lineEndX, lineEndY);

                // Hozzáadjuk az animációs keretet
                timeline.getKeyFrames().add(kf);

                // Elindítjuk az animációt
                timeline.play();
            }
        }
    }

    // Szimuláció újraindítása
    private void resetSimulation(Pane root) {
        // Töröljük a meglévő vonalakat
        for (Line line : lines) {
            root.getChildren().remove(line);
        }
        lines.clear();  // Kiürítjük a vonalakat tartalmazó listát

        // Robotok visszahelyezése az eredeti pozícióba
        for (int i = 0; i < robots.size(); i++) {
            Circle robot = robots.get(i);
            Circle startPoint = startPoints.get(i);
            robot.setCenterX(startPoint.getCenterX());
            robot.setCenterY(startPoint.getCenterY());
        }

        // A simulationRunning állapot visszaállítása
        simulationRunning = false;
    }

    public static void main(String[] args) {
        launch();
    }
}
