class TimedAction extends Action{
	private long start_time;
	private float length;

	public TimedAction(Robot robot, float speed, float length){
		super(robot, speed);
		this.length = length;
	}

    @Override
	public void start() {
		this.start_time = System.currentTimeMillis();
		for (int i = 0; i < 4; i++) {
			this.robot.set_wheel(i + 1, wheels[i][0] * speed, wheels[i][1]);
		}
	}

    @Override
	public boolean is_ready(){
        if ((this.start_time + (long) this.length) >= System.currentTimeMillis()) {
            System.out.println("end time: " + (this.start_time + (long) this.length) + ", current time: " + System.currentTimeMillis());
        }
		return (long) (this.start_time + this.length) < System.currentTimeMillis();
	}

}