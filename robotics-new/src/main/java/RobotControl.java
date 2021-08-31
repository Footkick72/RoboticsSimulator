 
public class RobotControl {
    public static void main(String[] args) {
        // this class is really just a place to stick a main method for now just so I can test everything
        Robot robot = new Robot();
        robot.move_to(new Vector2(4,9), 0.1f);
        robot.move_to(new Vector2(10,7), 0.1f);
    }
}
