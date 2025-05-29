import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.List;
import java.util.ArrayList;

public class DemoViewer {
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        // slider to control horizontal rotation
        JSlider headingSlider = new JSlider(0, 360, 180);
        pane.add(headingSlider, BorderLayout.SOUTH);

        // slider to control vertical rotation
        JSlider pitchSlider = new JSlider(SwingConstants.VERTICAL, -90, 90, 0);
        pane.add(pitchSlider, BorderLayout.EAST);

        // panel to display render results
        JPanel renderPanel = new JPanel() {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // rendering magic will happen here
                List<Triangle> tris = new ArrayList<>();
                tris.add(new Triangle(
                        new Vertex(100, 100, 100),
                        new Vertex(-100, -100, 100),
                        new Vertex(-100, 100, -100),
                        Color.WHITE));

                tris.add(new Triangle(
                        new Vertex(100, 100, 100),
                        new Vertex(-100, -100, 100),
                        new Vertex(100, -100, -100),
                        Color.RED));

                tris.add(new Triangle(
                        new Vertex(-100, 100, -100),
                        new Vertex(100, -100, -100),
                        new Vertex(100, 100, 100),
                        Color.GREEN));

                tris.add(new Triangle(
                        new Vertex(-100, 100, -100),
                        new Vertex(100, -100, -100),
                        new Vertex(-100, -100, 100),
                        Color.BLUE));

                // g2.translate(getWidth() / 2, getHeight() / 2);
                // g2.setColor(Color.WHITE);

                // Create a Tetrahedron in Orthographic Projection
                for (Triangle t : tris) {
                    Path2D path = new Path2D.Double();
                    path.moveTo(t.v1.x, t.v1.y);
                    path.lineTo(t.v2.x, t.v2.y);
                    path.lineTo(t.v3.x, t.v3.y);
                    path.closePath();
                    // g2.draw(path);
                } // end loop

                // Create our Rotation Matrix
                double heading = Math.toRadians(headingSlider.getValue());
                Matrix3 transform = new Matrix3(new double[] {
                        Math.cos(heading), 0, -Math.sin(heading),
                        0, 1, 0,
                        Math.sin(heading), 0, Math.cos(heading)
                });

                Matrix3 headingTransform = new Matrix3(new double[] {
                        Math.cos(heading), 0, Math.sin(heading),
                        0, 1, 0,
                        -Math.sin(heading), 0, Math.cos(heading) });

                double pitch = Math.toRadians(pitchSlider.getValue());
                Matrix3 pitchTransform = new Matrix3(new double[] {
                        1, 0, 0,
                        0, Math.cos(pitch), Math.sin(pitch),
                        0, -Math.sin(pitch), Math.cos(pitch) });

                transform = headingTransform.multiply(pitchTransform);

                g2.translate(getWidth() / 2, getHeight() / 2);
                g2.setColor(Color.WHITE);

                for (Triangle t : tris) {
                    Vertex v1 = transform.transform(t.v1);
                    Vertex v2 = transform.transform(t.v2);
                    Vertex v3 = transform.transform(t.v3);
                    Path2D path = new Path2D.Double();
                    path.moveTo(v1.x, v1.y);
                    path.lineTo(v2.x, v2.y);
                    path.lineTo(v3.x, v3.y);
                    path.closePath();
                    g2.draw(path);
                } // end loop

            } // end paintComponent
        };
        pane.add(renderPanel, BorderLayout.CENTER);

        // add listeners on heading and pitch sliders to force redraw when you drag
        headingSlider.addChangeListener(e -> renderPanel.repaint());
        pitchSlider.addChangeListener(e -> renderPanel.repaint());

        frame.setSize(400, 400);
        frame.setVisible(true);
    } // end main
} // end class DemoViewer

class Vertex {
    double x, y, z;
    /*
     * where, in a 3D co-ordinate system:
     * x = x-coord along the x-axis (left and right direction)
     * y = "" (up and down direction)
     * z = "" (towards and away from observer)
     */

    Vertex(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    } // end constructor
} // end class Vertex

class Triangle {
    Vertex v1, v2, v3;
    Color color;

    Triangle(Vertex v1, Vertex v2, Vertex v3, Color color) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.color = color;
    } // end constructor
} // end class Triangle

/*
 * Any rotation in 3D space can be expressed as combination of 3 primitive
 * rotations:
 * 1. Rotation in XY plane,
 * 2. Rotation in YZ plane, and
 * 3. Rotation in XZ plane
 * 
 * For each rotation, we can write out a Transformation Matrix:
 * XY Rotation Matrix: |cos(x) -sin(x) 0|
 * |sin(x) cos(x) 0|
 * | 0 0 1|
 * 
 * YZ Rotation Matrix: |1 0 0|
 * |0 cos(x) 0|
 * |0 -sin(x) cos(x)|
 * 
 * XZ Rotation Matrix: |cos(x) 0 -sin(x)|
 * | 0 1 0 |
 * |sin(x) 0 cos(x)|
 * 
 */

class Matrix3 {
    double[] values;

    Matrix3(double[] values) {
        this.values = values;
    } // end constructor

    Matrix3 multiply(Matrix3 other) {
        double[] result = new double[9];

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                for (int i = 0; i < 3; i++) {
                    result[row * 3 + col] += this.values[row * 3 + i] * other.values[i * 3 + col];
                }
            } // end inner loop
        } // end outer loop
        return new Matrix3(result);
    } // end multiply matrix method

    Vertex transform(Vertex in) {
        return new Vertex(
                in.x * values[0] + in.y * values[3] + in.z * values[6],
                in.x * values[1] + in.y * values[4] + in.z * values[7],
                in.x * values[2] + in.y * values[5] + in.z * values[8]);
    } // end method transformation
} // end class Matrix3