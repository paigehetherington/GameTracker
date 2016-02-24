package com.theironyard;

import com.sun.tools.internal.xjc.reader.xmlschema.bindinfo.BIConversion;
import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.HashMap;

public class Main {

    static HashMap<String, User> users = new HashMap<>();

    public static void main(String[] args) {
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
                if (user == null) {
                    return new ModelAndView(m, "login.html");
                }
                else {
                    return new ModelAndView(user, "home.html");
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
                    Game game = new Game(gameName, gameGenre, gamePlatform, gameYear);

                    user.games.add(game); //legit user, adds to AL

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
    }

    //to pull out of the session and query HM
    static User getUserFromSession(Session session) {
        String name = session.attribute("userName");
        return users.get(name);
    }
}
