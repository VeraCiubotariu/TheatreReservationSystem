package com.example.theatrereservationsystem.persistence;

import com.example.theatrereservationsystem.domain.Administrator;
import com.example.theatrereservationsystem.domain.Show;
import com.example.theatrereservationsystem.domain.ShowType;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShowRepository extends DBRepository {
    public ShowRepository(String url, String username, String password) {
        super(url, username, password);
    }

    public Optional<Show> get(int showID){
        try{
            Connection connection = getConnection();
            connection.setAutoCommit(false);
            PreparedStatement statement = connection.prepareStatement("select * from shows s " +
                    "inner join administrators a on s.admin_id = a.id where id=?");
            statement.setInt(1, showID);

            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()){
                return Optional.of(getShow(resultSet));
            }
        } catch (SQLException e){
            throw new RuntimeException(e);
        }

        return Optional.empty();
    }

    public Optional<Show> getTodayShow(){
        try{
            Connection connection = getConnection();
            connection.setAutoCommit(false);
            PreparedStatement statement = connection.prepareStatement("select * from shows s " +
                    "inner join administrators a on s.admin_id = a.id where extract(year from date) = " +
                    "extract(year from current_date) and extract(month from date) = extract(month from current_date) " +
                    "and extract(day from date) = extract(day from current_date)");
            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()){
                return Optional.of(getShow(resultSet));
            }
        } catch (SQLException e){
            throw new RuntimeException(e);
        }

        return Optional.empty();
    }

    private List<Show> getAllTemplate(String sqlCommand){
        List<Show> shows = new ArrayList<>();

        try{
            Connection connection = getConnection();
            connection.setAutoCommit(false);
            PreparedStatement statement = connection.prepareStatement(sqlCommand);
            ResultSet resultSet = statement.executeQuery();
            while(resultSet.next()){
                shows.add(getShow(resultSet));
            }
        } catch (SQLException e){
            throw new RuntimeException(e);
        }

        return shows;
    }

    public List<Show> getAllPast(){
        return getAllTemplate("select * from shows s " +
                "inner join administrators a on s.admin_id = a.id where s.date < NOW()");
    }

    public List<Show> getAllFuture(){
        return getAllTemplate("select * from shows s " +
                "inner join administrators a on s.admin_id = a.id where s.date >= NOW()");
    }

    public Optional<Show> delete(int showID) {
        Optional<Show> show = this.get(showID);
        if(show.isEmpty()){
            return Optional.empty();
        }

        Connection con = null;

        try {
            con = DriverManager.getConnection(url, username, password);
            PreparedStatement statement = con.prepareStatement("delete from shows where id=?");
            statement.setInt(1, showID);

            int response = statement.executeUpdate();
            con.commit();
            return response == 0? Optional.empty() : show;
        } catch(Exception e) {

            try{
                assert con != null;
                con.rollback();
            } catch (SQLException ex){
                throw new RuntimeException(e);
            }

            throw new RuntimeException(e);
        } finally {
            if(con != null){
                try{
                    con.close();
                } catch (SQLException ignored){

                }
            }
        }
    }

    public Optional<Show> save(Show entity) {
        Connection con = null;

        try{
            con = DriverManager.getConnection(url, username, password);
            con.setAutoCommit(false);

            PreparedStatement statement = con.prepareStatement("insert into shows(date, name, actors, genre, " +
                    "description, duration, director, poster, admin_id) values (?,?,?,?,?,?,?,?,?)");
            setShow(statement, entity);


            int response = statement.executeUpdate();
            con.commit();
            return response == 0? Optional.of(entity) :Optional.empty();
        } catch(Exception e) {

            try{
                assert con != null;
                con.rollback();
            } catch (SQLException ex){
                throw new RuntimeException(e);
            }

            throw new RuntimeException(e);
        } finally {
            if(con != null){
                try{
                    con.close();
                } catch (SQLException ignored){

                }
            }
        }
    }

    public Optional<Show> update(Show entity) {
        Connection con = null;

        try {
            con = DriverManager.getConnection(url, username, password);
            con.setAutoCommit(false);

            PreparedStatement statement = con.prepareStatement("update shows set date=?, name=?, actors=?, " +
                    "genre=?, description=?, duration=?, director=?, poster=?, admin_id=? where id=?");
            setShow(statement, entity);
            statement.setInt(10, entity.getId());

            int response = statement.executeUpdate();
            con.commit();
            return response == 0? Optional.empty() : Optional.of(entity);
        } catch(Exception e) {

            try{
                assert con != null;
                con.rollback();
            } catch (SQLException ex){
                throw new RuntimeException(e);
            }

            throw new RuntimeException(e);
        } finally {
            if(con != null){
                try{
                    con.close();
                } catch (SQLException ignored){

                }
            }
        }
    }

    private Show getShow(ResultSet resultSet) throws SQLException {
        LocalDateTime date = resultSet.getTimestamp("date").toLocalDateTime();
        String name = resultSet.getString("name");
        String actors = resultSet.getString("actors");
        ShowType genre = ShowType.valueOf(resultSet.getString("genre"));
        String description = resultSet.getString("description");
        int duration = resultSet.getInt("duration");
        String director = resultSet.getString("director");
        Blob posterData = resultSet.getBlob("poster");
        Image poster = new Image(posterData.getBinaryStream());
        int showID = resultSet.getInt("id");

        int adminID = resultSet.getInt("admin_id");
        String username = resultSet.getString("username");
        String password = resultSet.getString("password");
        String firstName = resultSet.getString("first_name");
        String lastName = resultSet.getString("last_name");

        Administrator admin = new Administrator(username, password, firstName, lastName);
        admin.setId(adminID);

        Show show = new Show(date, name, actors, genre, description, duration, director, admin, poster);
        show.setId(showID);
        return show;
    }

    private void setShow(PreparedStatement statement, Show show) throws SQLException, IOException {
        statement.setTimestamp(1, Timestamp.valueOf(show.getDate()));
        statement.setString(2, show.getName());
        statement.setString(3, show.getActors());
        statement.setString(4, show.getGenre().toString());
        statement.setString(5, show.getDescription());
        statement.setInt(6, show.getDuration());
        statement.setString(7, show.getDirector());
        statement.setBlob(8, getBlobFromImage(show.getPoster()));
        statement.setInt(9, show.getAdmin().getId());
    }

    private Blob getBlobFromImage(Image image) throws IOException, SQLException {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        // Create a BufferedImage of the same size as the Image
        java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);

        // Obtain the PixelReader from the Image
        PixelReader pixelReader = image.getPixelReader();

        // Write the pixels of the Image to the BufferedImage
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Get the color of the pixel
                Color color = pixelReader.getColor(x, y);
                // Convert the Color to a Java AWT Color
                java.awt.Color awtColor = new java.awt.Color((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue(), (float) color.getOpacity());
                // Set the pixel in the BufferedImage
                bufferedImage.setRGB(x, y, awtColor.getRGB());
            }
        }

        // Write the BufferedImage to a byte array output stream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(bufferedImage, "png", outputStream); // You can also use "jpeg" for JPEG format
        byte[] imageBytes = outputStream.toByteArray();

        // Create a Blob from the byte array
        return new javax.sql.rowset.serial.SerialBlob(imageBytes);
    }
}