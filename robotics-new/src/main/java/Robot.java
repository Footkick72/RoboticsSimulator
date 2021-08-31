import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.net.URI;

public class Robot {
    // constants
    public final float self_width = 1.5f; // width of the robot
    public final float field_width = 12.0f; // width of the field
    public final float sensor_margin = 0.03f; // margin for how close sensor values need to be for it to be considerd a match
    public final float extra_drive_margin = 50; // scaling factor - this times sensor_margin is the margin at which we start considering a "pass match"
    private WebsocketSimInterface testing_interface; // only used for testing on the simulator to communicate with Godot
    private boolean is_testing = true; // is the code going to be interfacing with a real robot or using the simulator (true means the simulator)
    
    // dynamic stuff
    public Vector2 coords = new Vector2(0, 0);
    public float angle = (float) (Math.PI / 2);
    public float[] sensor_readings; // continously updated sensors (since communication with the robot has some latency, its better to remember past values than expect a successful request every time)
    private ArrayList<String[]> action_queue = new ArrayList<>();
    private Action current_action = null;
    private boolean has_established_initial_coords = false;

    public Robot() {
        try {
            this.testing_interface = new WebsocketSimInterface(new URI("ws://localhost:9080"), this);
            this.testing_interface.connect();
        } catch (Exception e) {
            System.out.println("there was an error establishing the connection");
        }
        request_sensor_update();
    }

    private void process() { // this is called every time we get a new sensor update
        if (this.has_established_initial_coords) {
            if (this.current_action != null && !this.action_queue.isEmpty() && this.current_action.is_ready()) {
                if (this.current_action.get_type().equals("drive")) {
                    this.coords = ((DriveAction) this.current_action).pos;
                }
                else if (current_action.get_type().equals("rotate")) {
                    this.angle = ((RotateAction) this.current_action).rot;
                }
                else if (current_action.get_type().equals("rest")) {
                    System.out.println(this.angle + " " + this.coords);
                    calibrate();
                    System.out.println(this.angle + " " + this.coords);
                    System.out.println();
                }
                action_queue.remove(0);
                this.current_action = null;
            }
            if (!this.action_queue.isEmpty() && this.current_action == null) {
                start_next_action();
            }
        }
        request_sensor_update(); // request the next update from the sensors
    }

    public void move_to(Vector2 pos, float speed) { // queues a rotation and a movement to get to the target pos
        queue_look_at(pos, speed);
        queue_drive(pos, speed);
    }

    private Action drive_to(Vector2 pos, float speed) { // assumes we are facing at the target
        System.out.println("moving towards " + pos);
        return new DriveAction(this, speed, get_expected_distances(pos, this.angle), pos);
    }

    private Action look_at(Vector2 pos, float speed) {
        Vector2 to_target = pos.sub(this.coords);
        float target_angle = to_target.angle();
        System.out.println("looking at " + pos + " at angle " + target_angle + " from rotation " + this.angle);
        System.out.println("rotation of " + Math.abs(this.angle - target_angle));
        System.out.println(String.valueOf(get_num_false_matches(target_angle)) + " expected matches");
        return new RotateAction(this, speed, get_expected_distances(this.coords, target_angle), target_angle, get_num_false_matches(target_angle));
    }

    private void queue_stop() {
        this.action_queue.add(new String[]{"stop"});
        this.action_queue.add(new String[]{"rest"});
    }

    private void queue_drive(Vector2 pos, float speed){
        this.action_queue.add(new String[] {"drive", String.valueOf(pos), String.valueOf(speed)});
        queue_stop();
    }

    private void queue_look_at(Vector2 facing, float speed) {
        this.action_queue.add(new String[] {"rotate", String.valueOf(facing), String.valueOf(speed * 3)}); // the 3 is a random scaler to make rotation be a bit faster for a speed of 1
        queue_stop();
    }

    private void start_next_action() {
        String[] next = this.action_queue.get(0);
        if (next[0].equals("drive")) {
            this.current_action = drive_to(new Vector2(next[1]), Float.parseFloat(next[2]));
        }
        else if (next[0].equals("rotate")) {
            this.current_action = look_at(new Vector2(next[1]), Float.parseFloat(next[2]));
        }
        else if (next[0].equals("stop")) {
            this.current_action = new StoppingAction(this, 1f, 5000f);
        }
        else if (next[0].equals("rest")) {
            this.current_action = new RestAction(this, 1f, 1000f);
        }
        this.current_action.start();
    }

    private int get_num_false_matches(float target){
        // This calculates the number of false sensor matches expected for a rotation to
        // a given angle using the current angle and position as a starting point. It does
        // this by literally running a simulation of rotating and seeing how many times the
        // sensors match. This is then used during the rotation to avoid stopping prematurely.
        float rot = angle;
        
        float[] posibilities = { (float) (target - 2 * Math.PI), target, (float) (target + 2 * Math.PI) };
        ArrayList<Float> deltas = new ArrayList<>();
        for (float r : posibilities) {
            deltas.add(r - rot);
        }
        ArrayList<Float> x = new ArrayList<>();
        for (float d : deltas) {
            x.add(Math.abs(d));
        }
        int i = x.indexOf(Collections.min(x));
        float delta = deltas.get(i);
        
        float[] goal_state = get_expected_distances(this.coords, target);
        int nsteps = 100;
        float step = delta/nsteps;
        int matches = 0;
        boolean is_matching = false;
        for (int j = 0; j < nsteps; j++) {
            rot += step;
            float[] sensors = get_expected_distances(this.coords, rot);
            if (sum_squares_differences(sensors, goal_state) <= this.sensor_margin) {
                if (!is_matching) {
                    matches += 1;
                }
                is_matching = true;
            }
            else {
                is_matching = false;
            }
        }
        return matches;
    }

    private float[] get_expected_distances(Vector2 pos, float angle) { // get the expected sensor readings for a specific position and angle
        // do this like a top down coordinate grid with the sensor axes considered as lines
        // the way this works is to calculate the intersection wtih each wall for
        // each axis of the robot (front-back, left-right) and take the smallest
        // value since that will be the axis that is actually pointed at the wall
        // instead of "hitting" the other wall far away, somewhere outside of the field
        Vector2[][] x = get_intersection_points(pos, angle);
        Vector2[] left_right = x[0];
        Vector2[] front_back = x[1];
        float[] distances = {0,0,0,0};
        Vector2 left_vector = new Vector2(0,1).rotated(angle);
        Vector2 front_vector = new Vector2(1,0).rotated(angle);
        if (is_in_direction(left_right[0], left_vector)) {
            distances[0] = left_right[0].distance_to(new Vector2(pos.x,pos.y)) - this.self_width/2;
            distances[1] = left_right[1].distance_to(new Vector2(pos.x,pos.y)) - this.self_width/2;
        }
        else {
            distances[0] = left_right[1].distance_to(new Vector2(pos.x,pos.y)) - this.self_width/2;
            distances[1] = left_right[0].distance_to(new Vector2(pos.x,pos.y)) - this.self_width/2;
        }
        if (is_in_direction(front_back[0], front_vector)) {
            distances[2] = front_back[0].distance_to(new Vector2(pos.x,pos.y)) - this.self_width/2;
            distances[3] = front_back[1].distance_to(new Vector2(pos.x,pos.y)) - this.self_width/2;
        }
        else {
            distances[2] = front_back[1].distance_to(new Vector2(pos.x,pos.y)) - this.self_width/2;
            distances[3] = front_back[0].distance_to(new Vector2(pos.x,pos.y)) - this.self_width/2;
        }
        return distances;
    }
        
    private Vector2[][] get_intersection_points(Vector2 pos, float angle) { // returns the intersection points for both axis of the robot with the walls
        ArrayList<Vector2> left_right = new ArrayList<>(); // left to right axis
        ArrayList<Vector2> front_back = new ArrayList<>(); // front to back axis
        float[] slopes = new float[]{(float) Math.tan(angle), (float) Math.tan(angle + Math.PI/2)};
        for (int i = 0; i < 2; i++) {
            float slope = slopes[i];
            float offset = pos.y - pos.x * slope;
            // model the line and calculate intersections with each wall
            Vector2 left = new Vector2(0, offset);
            Vector2 right = new Vector2(field_width, field_width * slope + offset);
            // default values for front and back assume no intersection
            Vector2 front = new Vector2(Float.POSITIVE_INFINITY, field_width);
            Vector2 back = new Vector2(Float.POSITIVE_INFINITY, 0);
            if (slope != 0) { // avoid division by zero error when line is flat (no intersection with top and bottom of field)
                front = new Vector2((field_width - offset) / slope, field_width);
                back = new Vector2(-offset / slope, 0);
            }
            if (i == 1) {
                Vector2[] points = new Vector2[] {left, right, front, back};
                for (int j = 0; j < 4; j++) {
                    if (in_field(points[j])) {
                        left_right.add(points[j]);
                    }
                }
                if (!is_in_direction(left_right.get(0), new Vector2(0,1).rotated(angle))) {
                    left_right = new ArrayList<>(Arrays.asList(left_right.get(1), left_right.get(0)));
                }
            }
            else if (i == 0) {
                Vector2[] points = new Vector2[] {left, right, front, back};
                for (int j = 0; j < 4; j++) {
                    if (in_field(points[j])) {
                        front_back.add(points[j]);
                    }
                }
                if (!is_in_direction(front_back.get(0), new Vector2(1,0).rotated(angle))) {
                    front_back = new ArrayList<>(Arrays.asList(front_back.get(1), front_back.get(0)));
                }
            }
        }
        return new Vector2[][]{left_right.toArray(new Vector2[0]), front_back.toArray(new Vector2[0]) };
    }

    private boolean is_in_direction(Vector2 pos, Vector2 dir) { // helper method - returns true if a point is to [direction] of the robot using dot products
        return pos.sub(coords).dot(dir) > 0;
    }

    private boolean in_field(Vector2 pos) { // helper method - returns true if a point is in the bounds of the field
        return 0 <= pos.x && pos.x <= field_width && 0 <= pos.y && pos.y <= field_width;
    }

    private void calibrate() { // called after we finish a movement action, calibrates the position and rotation.
        // after a movement, we are unsure about our exact position because of lag, momentum, slipping, etc.
        // this is similarly true for our rotation, which may have changed due to one wheel rotating faster
        // or slipping more than the others. However, we DO have a pretty good guess for our position and angle - 
        // and we only need to calibrate that. The solution is clear - brute force :) We iterate through all
        // angles, +-5 degrees in angle_step degree increments, and the do the same procedure to iterate through
        // all positions in a 4 inch by 4 inch grid in grid_step inch increments. Then, we pick the best fit.
        float angle_step = (float) (0.5 * Math.PI / 180);
        float grid_step = 0.5f;
        float min_angle = (float) (angle - 5 * Math.PI / 180);
        float max_angle = (float) (angle + 5 * Math.PI / 180);
        // 0.1666s are cause 4 inches (grid size) / 2 is 2 inches, and 2/12 is 0.1666 (everything is in feet)
        float min_x = coords.x - 2.0f/12.0f;
        float max_x = coords.x + 2.0f/12.0f;
        float min_y = coords.y - 2.0f/12.0f;
        float max_y = coords.y + 2.0f/12.0f;
        System.out.println(coords);
        System.out.println(angle);
        float closest_angle = 0;
        Vector2 closest_pos = new Vector2(0, 0);
        float closest_match_value = 10e10f;
        for (float a = min_angle * (1/angle_step); a <= max_angle * (1/angle_step); a++) {
            for (float x = min_x * (1/grid_step) * 12; x <= max_x * (1/grid_step) * 12; x++) {
                for (float y = min_y * (1/grid_step) * 12; y <= max_y * (1/grid_step) * 12; y++) {
                    float[] expected = get_expected_distances(new Vector2(x * (grid_step / 12), y * (grid_step / 12)), a * angle_step);
                    float[] observed = get_sensor_readings();
                    float fit = sum_squares_differences(expected, observed);
                    if (fit < closest_match_value) {
                        closest_angle = a * angle_step;
                        closest_pos = new Vector2(x * (grid_step / 12), y * (grid_step / 12));
                        closest_match_value = fit;
                    }
                }
            }
        }
    	// system.out.println(angle, " ", coords)
    	// system.out.println(closest_fit)
        this.angle = closest_angle;
        this.coords = closest_pos;
        System.out.println("calibrating, matched at " + closest_match_value);
        if (closest_match_value > sensor_margin) {
            calibrate(); // do another calibration step if we didn't get a good match (our initial guess for out position wasn't quite good enough)
        }
    }

    private float sum_squares_differences(float[] list1, float[] list2) { // uses the sum of squares of differences algorithm to compare two numeric iterables
        float sum = 0;
        for (int i = 0; i < list1.length; i++) {
            sum += Math.pow(list1[i] - list2[i], 2);
        }
        return sum;
    }

    public float[] get_sensor_readings() { // returns the last known sensor values - follows the convention of left, right, front, back
        return this.sensor_readings;
        // This is the old godot code if anyone cares. You can delete this if you want.

        // $RangeFinderLeft.force_raycast_update()
        // $RangeFinderRight.force_raycast_update()
        // $RangeFinderFront.force_raycast_update()
        // $RangeFinderBack.force_raycast_update()
        // return [$RangeFinderLeft.get_collision_point().distance_to($RangeFinderLeft.global_transform.origin),
        //     $RangeFinderRight.get_collision_point().distance_to($RangeFinderRight.global_transform.origin),
        //     $RangeFinderFront.get_collision_point().distance_to($RangeFinderFront.global_transform.origin),
        //     $RangeFinderBack.get_collision_point().distance_to($RangeFinderBack.global_transform.origin)]
    }

    private void request_sensor_update() {
        if (is_testing) { // this code uses the WebsocketSimInterface and is simulator-specific
            this.testing_interface.request_sensor_update();
        }
        else {
            // TODO: this needs to actually, like, query the physical robot sensors. Not really sure how to do that at the moment.
        }
    }

    public void recieve_sensor_update(float[] update) {
        this.sensor_readings = update;
        if (!this.has_established_initial_coords) {
            this.coords.x = get_sensor_readings()[0] + this.self_width/2;
            this.coords.y = get_sensor_readings()[3] + this.self_width/2;
        }
        this.has_established_initial_coords = true;
        process();
    }
    
    public void set_wheel(int wheel, float engine_force, float brake) { // sets wheel value - if speed is negative, rotates the wheel 180 (godot only)
        if (is_testing) { // this code uses the WebsocketSimInterface and is simulator-specific
            this.testing_interface.send_wheel_values(wheel, engine_force, brake);
        }
        else {
            // TODO: make this set the wheel values for the actual robot. Again, not really sure how to do that.
        }
        // This is the old godot code if anyone cares. You can delete this if you want.

        // get_node("VehicleWheel" + str(wheel)).engine_force = abs(engine_force)
        // get_node("VehicleWheel" + str(wheel)).steering = -PI if engine_force < 0 else 0
        // get_node("VehicleWheel" + str(wheel)).brake = brake
    }

}
