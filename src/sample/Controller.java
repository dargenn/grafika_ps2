package sample;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.util.Iterator;

public class Controller {
    @FXML
    public Button loadFileButton;
    @FXML
    public Label fileNameLabel;
    @FXML
    public ImageView imageViewContainer;
    @FXML
    public Slider slider;


    private File selectedFile = null;

    @FXML
    public void loadFile(ActionEvent event) throws IOException {
        Node node = (Node) event.getSource();
        Scene scene = node.getScene();
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.ppm", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(scene.getWindow());
        fileNameLabel.setText(file.getName());
        selectedFile = file;
        if (file.getName().endsWith(".jpg") || file.getName().endsWith(".jpeg"))
            imageViewContainer.setImage(new Image(new FileInputStream(file)));
        if (file.getName().endsWith(".ppm"))
            displayPPM(file);
    }

    @FXML
    public void saveJPG() throws IOException {
        File compressedImageFile = new File("compressed_" + selectedFile.getName());
        InputStream inputStream = new FileInputStream(selectedFile);
        OutputStream outputStream = new FileOutputStream(compressedImageFile);

        float quality = (float) slider.getValue() / 100;
        System.out.println("quality: " + quality);

        BufferedImage image = ImageIO.read(inputStream);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = writers.next();
        ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream);
        writer.setOutput(imageOutputStream);
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        writer.write(null, new IIOImage(image, null, null), param);

        inputStream.close();
        outputStream.close();
    }

    private void displayPPM(File file) {
        try {
            switch (getPpmFileType(file)) {
                case P3:
                    displayP3(file);
                    break;
                case P6:
                    displayP6(file);
                    break;
                default:
                    new Alert(Alert.AlertType.ERROR, "Wrong file format").show();
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error in file").show();
        } catch (NumberFormatException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Wrong number format").show();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Unrecognized error").show();
        }
    }


    private void displayP3(File file) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        bufferedReader.readLine();
        String line = null;
        int width = 0, height = 0, currentWidth = 0, currentHeight = 0;
        BufferedImage img = null;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.startsWith("#")) {
                line = bufferedReader.readLine();
                String[] dimensions = line.split(" ");
                width = Integer.valueOf(dimensions[0]);
                height = Integer.valueOf(dimensions[1]);
                img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                bufferedReader.readLine();
            }

            if (line.split(" ").length >= 3) {
                String currentLine = bufferedReader.readLine();
                currentLine = currentLine.replaceAll("\\s+", " ");
                if (currentLine.startsWith(" "))
                    currentLine = currentLine.substring(1);
                String[] pixels = currentLine.split(" ");
                for (int index = 0; index < pixels.length; index += 3) {
                    int r = Integer.valueOf(pixels[index]);
                    int g = Integer.valueOf(pixels[index + 1]);
                    int b = Integer.valueOf(pixels[index + 2]);
                    int rgb = (r << 16) | (g << 8) | b;
                    img.setRGB(currentWidth, currentHeight, rgb);
                    currentWidth++;
                    currentWidth %= width;
                    if (currentWidth == 0)
                        currentHeight++;
                }
            }
        }

        BufferedImage imageRGB = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.OPAQUE);
        Graphics2D graphics = imageRGB.createGraphics();

        graphics.drawImage(img, 0, 0, null);
        graphics.dispose();
        imageViewContainer.setImage(SwingFXUtils.toFXImage(imageRGB, null));
    }

    private void displayP6(File file) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        bufferedReader.readLine();
        String line = null;
        BufferedImage img = null;
        int width = 0, height = 0, maxValue = 0, r, g, b;
        byte[] pixels = null;
        int counter = 0;
        boolean alreadyVisited = false;
        while ((line = bufferedReader.readLine()) != null) {
            if (!alreadyVisited) {
                alreadyVisited = true;
                String[] dimensions = line.split(" ");
                width = Integer.valueOf(dimensions[0]);
                height = Integer.valueOf(dimensions[1]);
                pixels = new byte[width * height * 3 * 500];
                maxValue = Integer.valueOf(bufferedReader.readLine());
                continue;
            }

            byte[] bytes = line.getBytes();
            for (int i = 0; i < bytes.length; i += 3) {
                if (i + 2 >= bytes.length)
                    break;
                r = bytes[i] & 0xff;
                g = bytes[i + 1] & 0xff;
                b = bytes[i + 2] & 0xff;
                pixels[counter++] = (byte) r;
                pixels[counter++] = (byte) g;
                pixels[counter++] = (byte) b;
            }
        }

        BufferedImage newImg = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        WritableRaster writableRaster = newImg.getRaster();
        writableRaster.setDataElements(0, 0, width, height, pixels);
        newImg.setData(writableRaster);

        Graphics2D graphics = newImg.createGraphics();

        graphics.drawImage(newImg, 0, 0, null);
        graphics.dispose();

        imageViewContainer.setImage(SwingFXUtils.toFXImage(newImg, null));
    }

    private PpmFileType getPpmFileType(File file) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String type = bufferedReader.readLine();
        bufferedReader.close();
        if (type.equals("P3"))
            return PpmFileType.P3;
        if (type.equals("P6"))
            return PpmFileType.P6;
        return PpmFileType.UNRECOGNIZED;
    }
}
