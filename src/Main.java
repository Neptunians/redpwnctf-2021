package net.redpwn;

import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.DirectoryCodeResolver;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.plugin.rendering.template.JavalinJte;
import java.nio.file.Path;

public class Main {
  public static Database db;
  
  public static String flag;
  
  public static void main(String[] args) {
    String adminUser = System.getenv("adminUser");
    String adminPassword = System.getenv("adminPassword");
    flag = System.getenv("flag");
    String javalinEnv = System.getenv("javalinEnv");
    db = new Database(adminUser, adminPassword);
    db.initializeDatabase();
    JavalinJte.configure(createTemplateEngine(javalinEnv));
    Javalin app = Javalin.create().start(8080);
    app.get("/", ctx -> ctx.render("index.jte"));
    app.get("/createUser", Handlers::createUser);
    app.get("/testAPI", Handlers::testAPI);
  }
  
  private static TemplateEngine createTemplateEngine(String javalinEnv) {
    if (javalinEnv != null && javalinEnv.equals("dev")) {
      DirectoryCodeResolver codeResolver = new DirectoryCodeResolver(Path.of("src", new String[] { "main", "jte" }));
      return TemplateEngine.create((CodeResolver)codeResolver, ContentType.Html);
    } 
    return TemplateEngine.createPrecompiled(Path.of("jte-classes", new String[0]), ContentType.Html);
  }
}
