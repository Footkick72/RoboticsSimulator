// This implements a Vector2 class that is PROBABLY included in some standard library but like I just wrote it cause it wasn't that hard. 
// It's modeled after the Godot Vector2 class and deals with both vector operations and is also a convenient way of representing coordinates.

public class Vector2 {
    public float x;
    public float y;

    public Vector2() {
        this.x = 0;
        this.y = 0;
    }

    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2(double x, double y) {
        this.x = (float) x;
        this.y = (float) y;
    }

    public Vector2(int x, int y) {
        this.x = (float) x;
        this.y = (float) y;
    }

    public Vector2(float[] desc) {
        this.x = desc[0];
        this.y = desc[1];
    }

     public Vector2(String desc) {
        String[] z = desc.split(" ", 0);
        System.out.println(z[0]);
        this.x = Float.parseFloat(z[0]);
        this.y = Float.parseFloat(z[1]);
    }

    public String toString() {
        return String.valueOf(this.x + " " + this.y);
    }

    public Vector2 rotated(float a) {
        return new Vector2(length() * Math.cos(a + angle()), length() * Math.sin(a + angle()));
    }

    public float length() {
        return distance_to(new Vector2(0,0));
    }

    public Vector2 rotated(double angle) {
        return rotated((float) angle);
    }

    public Vector2 sub(Vector2 other) {
        return new Vector2(this.x - other.x, this.y - other.y);
    }

    public Vector2 add(Vector2 other) {
        return new Vector2(this.x + other.x, this.y + other.y);
    }

    public float dot(Vector2 other) {
        return this.x * other.x + this.y * other.y;
    }
    
    public float distance_to(Vector2 other) {
        return (float) Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2));
    }

    public float angle() {
        return (float) Math.atan2(this.y, this.x);
    }

}
