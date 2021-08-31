public class DriveAction extends MovementAction {
	public Vector2 pos;
	
	public DriveAction(Robot robot, float speed, float[] target, Vector2 pos) {
        super(robot, speed, target, 1);
		this.wheels = new float[][] {{1,0},{1,0},{1,0},{1,0}};
		this.pos = pos;
        this.type = "drive";
    }	
}
