public class RestAction extends TimedAction {
    public RestAction(Robot robot, float speed, float length){
        super(robot, speed, length);
        this.wheels = new float[][] {{0,0},{0,0},{0,0},{0,0}};
        this.type = "rest";
    }
}
