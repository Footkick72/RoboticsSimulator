public class StoppingAction extends TimedAction {
    public StoppingAction(Robot robot, float speed, float length) {
        super(robot, speed, length);
        this.wheels = new float[][] {{0,0.017f},{0,0.017f},{0,0.017f},{0,0.017f}};
        this.type = "stop";
    }
}
