public class MovementAction extends Action {
	private float[] goal_state;
	private int sets; //number of expected false sensor matches due to feild symetry
	private int seen_sets = 0; //number of matches encountered
	private boolean is_matching = false;
	private float closest_match;
	
	public MovementAction(Robot robot, float speed, float[] target, int sets){
		super(robot, speed);
		this.goal_state = target;
		this.sets = sets;
		this.closest_match = 10e10f;
    }

    @Override
	public void start() {
		for (int i = 0; i < 4; i++) {
			this.robot.set_wheel(i + 1, wheels[i][0] * speed, wheels[i][1]);
        }
    }

    @Override
	public boolean is_ready() {
		// returns true when the robot either gets very close to the target position or, if the movement
		// is slightly off and that doesn't happen, when the robot has passed the point of closest aproach
        float[] sensors = robot.get_sensor_readings();
		// if sum_squares_differences(sensors, goal_state) < 1:
        float sensor_margin = robot.sensor_margin;
        for (float s: sensors) {
            System.out.println(s);
        }
        System.out.println();
        for (float s: goal_state) {
            System.out.println(s);
        }
        System.out.println();
        System.out.println(sum_squares_differences(sensors, goal_state));
        System.out.println();
        System.out.println();
        System.out.println();
        
		if (sum_squares_differences(sensors, goal_state) <= sensor_margin) {
			if (!this.is_matching) {
                this.seen_sets += 1;
            }
			this.is_matching = true;
			if (this.seen_sets == this.sets) {
				System.out.println(get_type() + " matched with " + sensors + this.goal_state + sum_squares_differences(sensors, this.goal_state));
                return true; //we have gotten very close
            }
        }
		else if (get_type().equals("drive") && sum_squares_differences(sensors, this.goal_state) <= sensor_margin * robot.extra_drive_margin) {
			if (sum_squares_differences(sensors, this.goal_state) <= this.closest_match) {
                this.closest_match = sum_squares_differences(sensors, this.goal_state);
            }
			else {
				if (!this.is_matching) {
                    this.seen_sets += 1;
                }
				this.is_matching = true;
				if (this.seen_sets == this.sets) {
					System.out.println(get_type() + " passed with " + sensors + this.goal_state + sum_squares_differences(sensors, this.goal_state));
                    return true; //we have passed the point of closest approach
                }
            }
        }
		else {
			this.is_matching = false;
		}
		return false;
    }

	public float sum_squares_differences(float[] list1, float[] list2) { //uses the sum of squares of differences aglorithm to compare two numeric iterables
		float sum = 0;
		for (int i = 0; i < list1.length; i++) {
            sum += Math.pow(list1[i] - list2[i], 2);
        }
        return sum;
    }
}
