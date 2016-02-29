package com.theironyard;

import com.sun.tools.internal.xjc.reader.xmlschema.bindinfo.BIConversion;
import org.h2.command.Prepared;
import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    static HashMap<String, User> users = new HashMap<>();

    public static void main(String[] args) throws SQLException {

        Connection conn = DriverManager.getConnection("jdbc:h2:./main");

        Statement stmt = conn.createStatement();

        stmt.execute("CREATE TABLE IF NOT EXISTS games (id IDENTITY, name VARchar, genre VARCHAR, platform VARCHAR, releaseYear INT)");

        Spark.externalStaticFileLocation("public");
    Spark.init();
    Spark.get(
            "/",

            ((request, response) -> {
                User user = getUserFromSession(request.session());
//                Session session = request.session();
//                String name = session.attribute("userName");
//                User user = users.get(name);

                HashMap m = new HashMap();
                m.put("games", selectGames(conn));
                if (user == null) {
                    return new ModelAndView(m, "login.html");
                }
                else {
                    return new ModelAndView(m, "home.html");

                }
            }),
            new MustacheTemplateEngine()

    );

        Spark.post(
                "/create-user",
                ((request, response) ->  {
                    String name = request.queryParams("loginName");
                    if (name == null) {
                        throw new Exception("login name is null.");
                    }
                    User user = users.get(name);
                    if (user == null) {
                        user = new User(name);
                        users.put(name, user);

                    }
                    //else { check password


                        Session session = request.session();
                        session.attribute("userName", name);

                    response.redirect("/");
                    return "";

                })
        );

        Spark.post(
                "/create-game",
                ((request, response) ->  {
                    User user = getUserFromSession(request.session());
                    if (user == null) {
                        //throw new Exception("User is not logged in"); (gives different error code)
                        Spark.halt(403); //error code when not allowed to access certain route
                    }


                    String gameName = request.queryParams("gameName");
                    String gameGenre = request.queryParams("gameGenre");
                    String gamePlatform = request.queryParams("gamePlatform");
                    int gameYear = Integer.valueOf(request.queryParams("gameYear"));
                    if (gameName == null || gameGenre == null || gamePlatform == null) {
                        throw new Exception("didn't receive all query parameters.");
                    }
                    Game game = new Game(1, gameName, gameGenre, gamePlatform, gameYear);

                    insertGame(conn, game);






                    //user.games.add(game); //legit user, adds to AL

                    response.redirect("/");
                    return "";
                })
        );
        Spark.post(
                "/logout",
                ((request, response) ->  {
                    Session session = request.session();
                    session.invalidate();
                    response.redirect("/");
                    return "";
                })
        );

        Spark.get(
                "/edit",
                ((request3, response3) ->  {
                    //HashMap m = new HashMap();

                    String editId = request3.queryParams("editId");
                    int id = Integer.valueOf(editId);
                    User user = getUserFromSession(request3.session());
                    Game game = selectGame(conn, id);


                    return new ModelAndView(game, "edit.html");
                }),
                new  MustacheTemplateEngine()
        );
        Spark.post(
                "/edit-game",
                ((request2, response2) ->  {
                    User user = getUserFromSession(request2.session());
                    int gameId = Integer.valueOf(request2.queryParams("id"));
                    Game edit = user.games.get(gameId);
                    String editGameName = request2.queryParams("gameName");
                    edit.name = editGameName;
                    String editGameGenre = request2.queryParams("gameGenre");
                    edit.genre = editGameGenre;
                    String editGamePlatform = request2.queryParams("gamePlatform");
                    edit.platform = editGamePlatform;
                    int gameYear = Integer.valueOf(request2.queryParams("gameYear"));
                    Game game = new Game (gameId, editGameName, editGameGenre, editGamePlatform, gameYear);
                    updateGame(conn, game);
                    response2.redirect("/");
                    return "";
                })
        );

        Spark.get(
                "/delete-game",
                ((request, response) ->  {
                    //Session session = request.session();
                    //String name = session.attribute("userName");
                    //User user = users.get(name);
                    String deleteId = request.queryParams("id");
                    int id = Integer.valueOf(deleteId);

                    deleteGame(conn, id);
                    response.redirect("/");
                    return "";
                })
        );

        //conn.close();
    }

    public static void insertGame(Connection conn, Game game) throws SQLException {
        PreparedStatement stmt2 = conn.prepareStatement("INSERT INTO games VALUES(Null, ?, ?, ?, ?)");
        stmt2.setString(1, game.name);
        stmt2.setString(2, game.genre);
        stmt2.setString(3, game.platform);
        stmt2.setInt(4, game.releaseYear);
        stmt2.execute();

    }

    public static void deleteGame(Connection conn, int id) throws SQLException {
        PreparedStatement stmt3 = conn.prepareStatement("DELETE FROM games WHERE id=?");
        stmt3.setInt(1, id);
        stmt3.execute();
    }

    public static ArrayList<Game> selectGames(Connection conn) throws SQLException {
        ArrayList<Game> allGames = new ArrayList<>();
        Statement stmt2 = conn.createStatement();       //("SELECT * FROM games WHERE userName = ?");
        ResultSet results = stmt2.executeQuery("SELECT * FROM games");
        while (results.next()) {
            int id = results.getInt("id");
            String name = results.getString("name");
            String genre = results.getString("genre");
            String platform = results.getString("platform");
            int releaseYear = results.getInt("releaseYear");
            Game game1 = new Game(id, name, genre, platform, releaseYear);
            allGames.add(game1);


        }
        return allGames;


    }

    public  static Game selectGame(Connection conn, int idNum) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM games WHERE id=?");
        stmt.setInt( 1, idNum);
        ResultSet results = stmt.executeQuery();
        if (results.next()) {
            int id = results.getInt("id");
            String name = results.getString("name");
            String genre = results.getString("genre");
            String platform = results.getString("platform");
            int releaseYear = results.getInt("releaseYear");
            Game game1 = new Game(id, name, genre, platform, releaseYear);
            return game1;

        } else {
            return null;
        }

    }

    public static void updateGame(Connection conn, Game game) throws SQLException {
        PreparedStatement stmt4 = conn.prepareStatement("UPDATE players SET name = ?, genre =?, platform = ?, releaseYear = ? WHERE id=?");
        stmt4.setString(1, game.name);
        stmt4.setString(2, game.genre);
        stmt4.setString(3, game.platform);
        stmt4.setInt(4, game.releaseYear);
        stmt4.setInt(5, game.id);
        stmt4.execute();
    }


    //to pull out of the session and query HM
    static User getUserFromSession(Session session) {
        String name = session.attribute("userName");
        return users.get(name);
    }
}
