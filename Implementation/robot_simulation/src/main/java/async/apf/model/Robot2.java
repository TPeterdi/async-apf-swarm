package async.apf.model;

import java.util.ArrayList;
import java.util.List;

public class Robot2 {
    int id;              // Robot azonosító
    int x, y;           // Robot pozíciója
    int targetX, targetY; // Cél pozíció

    public Robot2(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    // Frissíti a robot célpozícióját
    public void setTarget(int targetX, int targetY) {
        this.targetX = targetX;
        this.targetY = targetY;
    }

    // Robot mozgásának végrehajtása a cél felé
    public void moveToTarget() {
        if (x == targetX && y == targetY) {
            System.out.println("Robot " + id + " már elérte a célt: (" + targetX + ", " + targetY + ")");
            return;
        }

        if (targetX > x) {
            x++;
            System.out.println("Robot " + id + " jobbra mozog: (" + x + ", " + y + ")");
        } else if (targetX < x) {
            x--;
            System.out.println("Robot " + id + " balra mozog: (" + x + ", " + y + ")");
        }

        if (targetY > y) {
            y++;
            System.out.println("Robot " + id + " felfelé mozog: (" + x + ", " + y + ")");
        } else if (targetY < y) {
            y--;
            System.out.println("Robot " + id + " lefelé mozog: (" + x + ", " + y + ")");
        }
    }
}

class AutonomousPatrolFunction {
    List<Robot2> robots;

    public AutonomousPatrolFunction() {
        robots = new ArrayList<>();
    }

    // Robot hozzáadása a rendszerhez
    public void addRobot(Robot2 robot) {
        robots.add(robot);
    }

    // ApfLine algoritmus végrehajtása
    public void apfLine(Robot2 robot) {
        // Célpozíció elérése
        robot.moveToTarget();
    }

    // Rearrange funkció
    public void rearrange(Robot2 robot) {
        // Ellenőrzi a cél pozícióját, és mozgatja a robotot
        if (robot.targetX < robot.x) {
            // Balra kell mozogni
            System.out.println("Robot " + robot.id + " balra mozog a célnak megfelelően.");
            robot.x--;
        } else if (robot.targetX > robot.x) {
            // Jobbra kell mozogni
            System.out.println("Robot " + robot.id + " jobbra mozog a célnak megfelelően.");
            robot.x++;
        }

        if (robot.targetY < robot.y) {
            // Lefelé kell mozogni
            System.out.println("Robot " + robot.id + " lefelé mozog a célnak megfelelően.");
            robot.y--;
        } else if (robot.targetY > robot.y) {
            // Felfelé kell mozogni
            System.out.println("Robot " + robot.id + " felfelé mozog a célnak megfelelően.");
            robot.y++;
        }
    }

    // Robotok mozgásának szimulálása
    public void simulate() {
        for (Robot2 robot : robots) {
            apfLine(robot); // ApfLine algoritmus futtatása
            rearrange(robot); // Rearrange funkció futtatása
        }
    }

    public static void main(String[] args) {
        AutonomousPatrolFunction apf = new AutonomousPatrolFunction();

        // Robotok inicializálása
        Robot2 robot1 = new Robot2(1, 0, 0);
        robot1.setTarget(2, 2); // Cél: (2, 2)
        apf.addRobot(robot1);

        Robot2 robot2 = new Robot2(2, 1, 1);
        robot2.setTarget(0, 0); // Cél: (0, 0)
        apf.addRobot(robot2);

        // Szimuláció indítása
        apf.simulate();
    }
}
