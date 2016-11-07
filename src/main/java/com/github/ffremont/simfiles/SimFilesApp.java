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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Stream;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import org.eclipse.jetty.util.MultiPartInputStreamParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Redirect;
import spark.Request;
import static spark.Spark.before;
import static spark.Spark.delete;
import static spark.Spark.exception;
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

    private static final List<String> resourcesWhiteList = Arrays.asList(".js", ".css", ".png", ".json", ".ico", ".svg");

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

    private static void clear(Path directoryToDelete) {
        try {
            if (Files.exists(directoryToDelete)) {
                Files.walk(directoryToDelete).
                        sorted((a, b) -> b.compareTo(a)). // reverse; files before dirs
                        forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException e) {
                                LOGGER.warn("impossible de supprimer le fichier temporaire", e);
                            }
                        });
            }
        } catch (IOException ex) {
            LOGGER.error("oups", ex);
        }
    }

    public static void main(String[] args) throws IOException {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("header.txt");
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            System.out.print(scanner.useDelimiter("\\A").next());
            System.out.println("\n\n");
        }

        String confFile = System.getProperty("conf");
        if (confFile == null) {
            USERS = Arrays.asList(User.getDefault());
        } else {
            LOGGER.info("Chargement du fichier de configuration {}", confFile);
            byte[] jsonB = Files.readAllBytes(Paths.get(confFile));
            USERS = Arrays.asList(gson.fromJson(new String(jsonB, StandardCharsets.UTF_8), User[].class));
        }

        final int port = System.getProperty("port") == null ? 4567 : Integer.valueOf(System.getProperty("port"));
        LOGGER.info("Démarrage du serveur sur le port {}", port);
        port(port);
        final List<String> unsecurePaths = Arrays.asList("/explorer", "/resources");

        exception(Exception.class, (exception, request, response) -> {
            LOGGER.error("oups", exception);
        });

        before((request, response) -> {
            if ("/".equals(request.uri())) {
                response.redirect("/explorer", Redirect.Status.MOVED_PERMANENTLY.intValue());
            }

            if (unsecurePaths.stream().anyMatch(prefixe -> request.uri().startsWith(prefixe))) {
                LOGGER.debug("no problemo {}", request.uri());
            } else if (request.headers("Authorization") == null) {
                response.header("WWW-Authenticate", "Basic");
                halt(401);
            } else {
                User user = null;
                for (User u : USERS) {
                    String basic = "Basic " + Base64.encode((u.getId() + ":" + u.getPassword()).getBytes(StandardCharsets.UTF_8));
                    if (request.headers("Authorization").endsWith(basic)) {
                        user = u;
                        break;
                    }
                }

                if (user == null) {
                    halt(403);
                } else {
                    LOGGER.debug("Connexion de {}", user.getId());
                }
            }
        });
        delete("/files", (request, response) -> {
            MyContext myContext = MyContext.get(request);
            if (!Files.exists(myContext.path)) {
                halt(404);
            }
            if (myContext.path.toFile().isFile()) {
                Files.delete(myContext.path);
            } else {
                clear(myContext.path);
            }

            return "";
        });
        post("/folder", (request, response) -> {
            MyContext myContext = MyContext.get(request);
            String folderName = request.queryParams("name");
            if (folderName != null) {
                folderName = folderName.replace("..", "");
            } else {
                LOGGER.debug(("Nom de répertoire invalide"));
                halt(400);
            }

            Path dir = Paths.get(myContext.path.toAbsolutePath().toString(), folderName);
            if (!dir.toAbsolutePath().toString().startsWith(myContext.user.getDirectory())) {
                halt(403);
            }

            if (Files.exists(dir)) {
                halt(204);
            } else if (!Files.exists(dir.getParent())) {
                LOGGER.debug("Le répertoire parent n'existe pas");
                halt(400);
            } else {
                Files.createDirectory(dir);
                halt(204);
            }

            return "";
        });
        post("/files", (request, response) -> {
            MyContext myContext = MyContext.get(request);

            if (Files.notExists(myContext.path)) {
                halt(404);
            }

            long maxFileSize = 100000000;  // the maximum size allowed for uploaded files
            long maxRequestSize = 100000000;  // the maximum size allowed for multipart/form-data requests
            int fileSizeThreshold = 10240;  // the size threshold after which files will be written to disk
            Path tmpFolder = Files.createTempDirectory("simfiles_");
            MultipartConfigElement multipartConfigElement = new MultipartConfigElement(tmpFolder.toAbsolutePath().toString(), maxFileSize, maxRequestSize, fileSizeThreshold);
            request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);

            Collection<Part> parts = request.raw().getParts();
            try {
                for (Part part : parts) {
                    try {
                        if (Files.exists(Paths.get(myContext.path.toAbsolutePath().toString(), part.getSubmittedFileName()))) {
                            halt(409);
                        }

                        FileOutputStream fos = new FileOutputStream(Paths.get(myContext.path.toAbsolutePath().toString(), part.getSubmittedFileName()).toFile());
                        try (InputStream is = part.getInputStream()) {
                            byte[] buffer = new byte[10240]; // 10ko
                            int read;
                            while (-1 != (read = is.read(buffer))) {
                                fos.write(buffer, 0, read);
                            }
                            fos.flush();
                        }

                    } finally {
                        part.delete();
                    }
                }
            } finally {
                clear(tmpFolder);
                Files.deleteIfExists(tmpFolder);
            }

            return "";
        });
        get("/file", (request, response) -> {
            MyContext myContext = MyContext.get(request);
            if (Files.notExists(myContext.path)) {
                halt(404);
            }
            if (Files.isDirectory(myContext.path)) {
                halt(405);
            }

            response.header("Content-Type", Files.probeContentType(myContext.path));
            response.header("Content-Disposition", "attachment; filename=" + myContext.path.toFile().getName());

            return Files.newInputStream(myContext.path);
        });
        get("/file/view", (request, response) -> {
            MyContext myContext = MyContext.get(request);
            if (Files.notExists(myContext.path)) {
                halt(404);
            }
            if (Files.isDirectory(myContext.path)) {
                halt(405);
            }

            response.header("Content-Type", Files.probeContentType(myContext.path));

            return Files.newInputStream(myContext.path);
        });
        get("/files", (request, response) -> {
            MyContext myContext = MyContext.get(request);

            List<SimFile> files = new ArrayList<>();

            Stream.concat(
                    Files.list(myContext.path)
                    .filter((Path p) -> p.toFile().isDirectory())
                    .sorted(),
                    Files.list(myContext.path)
                    .filter((Path p) -> !p.toFile().isDirectory())
                    .sorted()
            ).forEach((Path p) -> {
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

            return new SimFolder(myContext.path.toAbsolutePath().toString(), myContext.path.toFile().getFreeSpace(), myContext.path.toFile().getTotalSpace(), files);
        }, new JsonTransformer());
        get("/explorer", (request, response) -> {
            response.header("Content-Type", "text/html ;charset=utf-8");

            return Thread.currentThread().getContextClassLoader().getResourceAsStream("explorer.html");
        });
        get("/resources/*", (request, response) -> {
            if (!resourcesWhiteList.stream().anyMatch(prefixe -> request.uri().endsWith(prefixe))) {
                halt(403);
            }

            if (request.uri().contains(".")) {
                String mime = Files.probeContentType(Paths.get(request.uri()));

                response.header("Content-Type", mime);
                return Thread.currentThread().getContextClassLoader().getResourceAsStream(request.uri().replace("/resources/", ""));
            } else {
                halt(421);
            }

            return "";
        });
    }

    private static class MyContext {

        private MyContext(User user, Path path) {
            this.user = user;
            this.path = path;
        }

        public User user;
        private Path path;

        public static MyContext get(Request request) {
            String path = request.queryParams("path") == null ? "" : request.queryParams("path").replace("..", "");
            User user = getUser(request);
            Path dir = Paths.get(user.getDirectory(), path);

            if (!dir.toAbsolutePath().toString().startsWith(user.getDirectory())) {
                halt(403);
            }

            return new MyContext(user, dir);
        }
    }

}
