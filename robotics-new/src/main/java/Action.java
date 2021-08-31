public class Action {
    protected Robot robot;
    protected float speed;
    public String type;
    public float[][] wheels; //list of format [speed, steering(godot only), brake(maybe godot only)]

    public Action(Robot robot, float speed) {
        this.robot = robot;
        this.speed = speed;
    }
    
    public String get_type() {
        return this.type;
    }
    
    public boolean is_ready() {
        return true;
    }

    public void start() {
        // does nothing
    }
}
