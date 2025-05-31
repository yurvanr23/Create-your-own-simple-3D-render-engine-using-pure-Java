import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
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

                // Define tetrahedron triangles with colours
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

                // Create our Rotation Matrix
                double heading = Math.toRadians(headingSlider.getValue());
                Matrix3 headingTransform = new Matrix3(new double[] {
                        Math.cos(heading), 0, Math.sin(heading),
                        0, 1, 0,
                        -Math.sin(heading), 0, Math.cos(heading) });

                double pitch = Math.toRadians(pitchSlider.getValue());
                Matrix3 pitchTransform = new Matrix3(new double[] {
                        1, 0, 0,
                        0, Math.cos(pitch), Math.sin(pitch),
                        0, -Math.sin(pitch), Math.cos(pitch) });

                /*
                 * Matrix3 transform = new Matrix3(new double[] {
                 * Math.cos(heading), 0, -Math.sin(heading),
                 * 0, 1, 0,
                 * Math.sin(heading), 0, Math.cos(heading)
                 * });
                 */

                Matrix3 transform = headingTransform.multiply(pitchTransform);

                /*
                 * Up to this point, we were only drawing the wireframe for our shape,
                 * now we need to start filling up the faces of our tetrahedron with some
                 * substance.
                 * First we need to "rasterize" the triangle (convert it to a list of pixels on
                 * screen that it occupies)
                 * We will use rasterization via 'Barycentric Coordinates'
                 * 
                 * IDEA: To compute barycentric coord. for each pixel that could possibly lie
                 * inside the triangle
                 * and discard those that are outside.
                 */

                // create image buffer to fill coloured triangles
                BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);

                // introduce the concept of a 'z-buffer/depth buffer'
                /*
                 * IDEA: To build an intermediate array during rasterization
                 * that will store depth of the last seen element at any given pixel.
                 * 
                 * When raterizing triangles, we will be checking that pixel depth is less than
                 * previously seen,
                 * AND only colour the pixel if it is above others.
                 */
                double[] zBuffer = new double[img.getWidth() * img.getHeight()];

                // initiliase array with extremely far away depths
                for (int i = 0; i < zBuffer.length; i++) {
                    zBuffer[i] = Double.NEGATIVE_INFINITY;
                } // end loop

                // Create a Tetrahedron in Orthographic Projection
                /*
                 * for (Triangle t : tris) {
                 * Path2D path = new Path2D.Double();
                 * path.moveTo(t.v1.x, t.v1.y);
                 * path.lineTo(t.v2.x, t.v2.y);
                 * path.lineTo(t.v3.x, t.v3.y);
                 * path.closePath();
                 * g2.draw(path);
                 * } // end loop
                 * 
                 * g2.translate(getWidth() / 2, getHeight() / 2);
                 * g2.setColor(Color.WHITE);
                 */

                // Create a Tetrahedron in 3D (Wireframe Drawing)
                g2.translate(getWidth() / 2, getHeight() / 2);
                for (Triangle t : tris) {
                    Vertex v1 = transform.transform(t.v1);
                    Vertex v2 = transform.transform(t.v2);
                    Vertex v3 = transform.transform(t.v3);
                    Path2D path = new Path2D.Double();

                    path.moveTo(v1.x, v1.y);
                    path.lineTo(v2.x, v2.y);
                    path.lineTo(v3.x, v3.y);
                    path.closePath();

                    g2.setColor(Color.WHITE);
                    g2.draw(path);
                } // end loop

                for (Triangle t : tris) {
                    Vertex v1 = transform.transform(t.v1);
                    Vertex v2 = transform.transform(t.v2);
                    Vertex v3 = transform.transform(t.v3);

                    /*
                     * In real life, perceived color of the surface varies with light source
                     * positions.
                     * If only a small amount of light is incident to the surface, we perceive that
                     * surface as being darker.
                     * In computer graphics, we can achieve similar effect by using so-called
                     * "shading"
                     * (altering the color of the surface based on its angle and distance to lights)
                     * 
                     * Simplest form of shading is flat shading.
                     * It takes into account only the angle between surface normal and direction of
                     * the light source.
                     * We just need to find cosine of angle between those two vectors and multiply
                     * the color by the resulting value.
                     * Such approach is very simple and cheap,
                     * so it is often used for high-speed rendering when more advanced shading
                     * technologies are too computationally expensive.
                     */

                    Vertex ab = new Vertex(v2.x - v1.x, v2.y - v1.y, v2.z - v1.z);
                    Vertex ac = new Vertex(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z);
                    Vertex norm = new Vertex(
                            ab.y * ac.z - ab.z * ac.y,
                            ab.z * ac.x - ab.x * ac.z,
                            ab.x * ac.y - ab.y * ac.x);

                    double normalLength = Math.sqrt(norm.x * norm.x + norm.y * norm.y + norm.z * norm.z);
                    norm.x /= normalLength;
                    norm.y /= normalLength;
                    norm.z /= normalLength;

                    /*
                     * Now we need to calculate cosine between triangle normal and light direction.
                     * For simplicity, we will assume that our light is positioned directly behind
                     * the camera at some infinite distance
                     * (such configuration is called "directional light")
                     * 
                     * So our light source direction will be [0 0 1]
                     */
                    double angleCos = Math.abs(norm.z);

                    // since we are NOT using Graphics2D anymore,
                    // we have to do translation manually
                    v1.x += getWidth() / 2;
                    v1.y += getHeight() / 2;

                    v2.x += getWidth() / 2;
                    v2.y += getHeight() / 2;

                    v3.x += getWidth() / 2;
                    v3.y += getHeight() / 2;

                    // compute rectangular bounds for triangle
                    int minX = (int) Math.max(0, Math.ceil(Math.min(v1.x, Math.min(v2.x, v3.x))));
                    int maxX = (int) Math.min(img.getWidth() - 1, Math.floor(Math.max(v1.x, Math.max(v2.x, v3.x))));

                    int minY = (int) Math.max(0, Math.ceil(Math.min(v1.y, Math.min(v2.y, v3.y))));
                    int maxY = (int) Math.min(img.getHeight() - 1, Math.floor(Math.max(v1.y, Math.max(v2.y, v3.y))));

                    double triangleArea = (v1.y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - v1.x);

                    for (int y = minY; y <= maxY; y++) {
                        for (int x = minX; x <= maxX; x++) {
                            double b1 = ((y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - x)) / triangleArea;
                            double b2 = ((y - v1.y) * (v3.x - v1.x) + (v3.y - v1.y) * (v1.x - x)) / triangleArea;
                            double b3 = ((y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - x)) / triangleArea;

                            if (b1 >= 0 && b1 <= 1 && b2 >= 0 && b2 <= 1 && b3 >= 0 && b3 <= 1) {
                                // handle rasterization = for each rasterized pixel:
                                double depth = b1 * v1.z + b2 * v2.z + b3 * v3.z;
                                int zIndex = y * img.getWidth() + x;

                                if (zBuffer[zIndex] < depth) {
                                    img.setRGB(x, y, getShade(t.color, angleCos).getRGB());
                                    zBuffer[zIndex] = depth;
                                } // end inner if
                            } // end outer if

                        } // end inner loop
                    } // end outer loop
                } // end loop

                g2.drawImage(img, -getWidth() / 2, -getHeight() / 2, null);

            } // end paintComponent
        };

        pane.add(renderPanel, BorderLayout.CENTER);

        // add listeners on heading and pitch sliders to force redraw when you drag
        headingSlider.addChangeListener(e -> renderPanel.repaint());
        pitchSlider.addChangeListener(e -> renderPanel.repaint());

        frame.setSize(400, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    } // end main

    public static Color getShade(Color color, double shade) {
        // convert each color from scaled to linear format and apply shade
        double redLinear = Math.pow(color.getRed(), 2.4) * shade;
        double greenLinear = Math.pow(color.getGreen(), 2.4) * shade;
        double blueLinear = Math.pow(color.getBlue(), 2.4) * shade;

        // then convert back to scaled format
        int red = (int) Math.pow(redLinear, 1 / 2.4);
        int green = (int) Math.pow(greenLinear, 1 / 2.4);
        int blue = (int) Math.pow(blueLinear, 1 / 2.4);

        return new Color(red, green, blue);
    } // end method getShade
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