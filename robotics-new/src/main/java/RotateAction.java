import java.util.ArrayList;
import java.util.Collections;

public class RotateAction extends MovementAction {
    public float rot;
	
	public RotateAction(Robot robot, float speed, float[] target, float rot, int sets){
        super(robot, speed, target, sets);
        float[] posibilities = new float[]{rot - 360, rot, rot + 360};
		ArrayList<Float> deltas = new ArrayList<>();
		for (float r : posibilities){
            deltas.add(r - robot.angle);
        }
		ArrayList<Float> x = new ArrayList<>();
		for (float d : deltas) {
            x.add(Math.abs(d));
        }
		int i = x.indexOf(Collections.min(x));
		float delta = deltas.get(i);
		if (delta < 0) {
            this.wheels = new float[][]{{1,0},{1,0},{-1,0},{-1,0}}; //rotate right
        }
		else {
            this.wheels = new float[][]{{-1,0},{-1,0},{1,0},{1,0}}; //rotate left
        }
        this.rot = rot;
        this.type = "rotate";
    }
}
