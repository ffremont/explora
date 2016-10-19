/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.ffremont.simfiles;

import com.google.gson.Gson;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Redirect;
import spark.Request;
import spark.Spark;
import static spark.Spark.before;
import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.port;
import static spark.Spark.post;

/**
 *
 * @author florent
 */
public class SimFilesApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimFilesApp.class);

    private static Gson gson = new Gson();
    private static List<User> USERS;

    private final static User getUser(Request request) {
        User user = null;
        for (User u : USERS) {
            String basic = Base64.encode((u.getId() + ":" + u.getPassword()).getBytes(StandardCharsets.UTF_8));
            if (request.headers("Authorization").endsWith(basic)) {
                user = u;
                break;
            }
        }

        return user;
    }

    public static void main(String[] args) throws IOException {
        USERS = Arrays.asList(User.getDefault());
        if (args.length != 0) {
            USERS = Arrays.asList(gson.fromJson(new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8), User[].class));
        }

        final int port = System.getProperty("port") == null ? 4567 : Integer.valueOf(System.getProperty("port"));
        port(port);

        before((request, response) -> {
            if (request.headers("Authorization") == null) {
                response.header("WWW-Authenticate", "Basic");
                halt(401);
            } else {
                User user = null;
                for (User u : USERS) {
                    String basic = Base64.encode((u.getId() + ":" + u.getPassword()).getBytes(StandardCharsets.UTF_8));
                    if (request.headers("Authorization").endsWith(basic)) {
                        user = u;
                        break;
                    }
                }

                if (user == null) {
                    halt(403);
                } else {
                    if("/".equals(request.uri())){
                        response.redirect("/explorer.html", Redirect.Status.MOVED_PERMANENTLY.intValue());
                    }
                    LOGGER.debug("Connexion de {}", user.getId());
                }
            }
        });
        delete("/files", (request, response) -> {
            String path = request.queryParams("path") == null ? "" : request.queryParams("path").replace("..", "");
            User user = getUser(request);
            Path dir = Paths.get(user.getDirectory(), path);

            if (dir.toAbsolutePath().toString().startsWith(user.getDirectory())) {
                halt(403);
            }
            if (!Files.exists(dir)) {
                halt(404);
            }
            if (!dir.toFile().isFile()) {
                halt(400);
            }

            Files.delete(dir);

            return "";
        });
        post("/files", (request, response) -> {
            String path = request.queryParams("path") == null ? "" : request.queryParams("path").replace("..", "");
            User user = getUser(request);
            Path dir = Paths.get(user.getDirectory(), path);
            if (dir.toAbsolutePath().toString().startsWith(user.getDirectory())) {
                halt(403);
            }

            if (Files.notExists(dir)) {
                halt(404);
            }

            long maxFileSize = 100000000;  // the maximum size allowed for uploaded files
            long maxRequestSize = 100000000;  // the maximum size allowed for multipart/form-data requests
            int fileSizeThreshold = 10240;  // the size threshold after which files will be written to disk
            MultipartConfigElement multipartConfigElement = new MultipartConfigElement(dir.toAbsolutePath().toString(), maxFileSize, maxRequestSize, fileSizeThreshold);
            request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);

            Collection<Part> parts = request.raw().getParts();
            for (Part part : parts) {
                if (Files.exists(Paths.get(dir.toAbsolutePath().toString(), part.getSubmittedFileName()))) {
                    halt(409);
                }

                Path tempFileBinary = Files.createTempFile("simFiles", "upload");
                FileOutputStream fos = new FileOutputStream(tempFileBinary.toFile());
                try (InputStream is = part.getInputStream()) {
                    byte[] buffer = new byte[10240]; // 10ko
                    int read;
                    while (-1 != (read = is.read(buffer))) {
                        fos.write(buffer, 0, read);
                    }
                    fos.flush();
                } 

                Files.copy(tempFileBinary, Paths.get(dir.toAbsolutePath().toString(), part.getSubmittedFileName()));
            }

            return "";
        });
        get("/files", (request, response) -> {
            String path = request.queryParams("path") == null ? "" : request.queryParams("path").replace("..", "");
            User user = getUser(request);
            Path dir = Paths.get(user.getDirectory(), path);
            if(dir.toAbsolutePath().toString().startsWith(user.getDirectory())){
                halt(403);
            }
            
            List<SimFile> files = new ArrayList<>();
            Files.list(dir).
                    sorted((a, b) -> a.toFile().isDirectory() ? -1 : 1). // reverse; files before dirs
                    forEach((Path p) -> {
                        String filename = p.getFileName().toString();
                        SimFile file = new SimFile(filename, !p.toFile().isFile());
                        try {
                            file.setSize(Files.size(p));
                        } catch (IOException ex) {
                            LOGGER.error("impossible de connaître la taille du fichier {}", p.toAbsolutePath());
                        }
                        try {
                            FileTime lastMod = Files.getLastModifiedTime(p);
                            file.setModified(lastMod.to(TimeUnit.SECONDS));
                        } catch (IOException ex) {
                            LOGGER.error("impossible de connaître la date de modification du fichier {}", p.toAbsolutePath());
                        }

                        files.add(file);
                    });

            return files;
        }, new JsonTransformer());

        get("/resources/*", (request, response) -> {
            boolean isJs = request.uri().endsWith(".js"), isCss = request.uri().endsWith(".css"), isHTML = request.uri().endsWith(".html");
            if (isJs || isCss) {
                if (isJs) {
                    response.header("Content-Type", "text/javascript ;charset=utf-8");
                } else if (isCss) {
                    response.header("Content-Type", "text/css ;charset=utf-8");
                }else if (isHTML) {
                    response.header("Content-Type", "text/html ;charset=utf-8");
                }

                return Thread.currentThread().getContextClassLoader().getResourceAsStream(request.uri().replace("/resources/", "/web/"));
            }

            return null;
        });
    }

}
