package io.chocorean.authmod.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import io.chocorean.authmod.AuthMod;
import io.chocorean.authmod.command.ExceptionToMessageMapper;
import io.chocorean.authmod.util.text.ServerLanguageMap;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;

import java.nio.file.Path;
import java.util.List;
import java.util.Arrays;

import static net.minecraftforge.fml.loading.LogMarkers.FORGEMOD;

public class AuthModConfig {

  public final DatabaseConfig database;
  public final I18nConfig i18n;
  public enum DataSource {FILE, DATABASE, NONE}
  public enum Language {EN_US, FR_FR, ES_ES}
  public final ForgeConfigSpec.BooleanValue identifierRequired;
  public final ForgeConfigSpec.BooleanValue enableLogin;
  public final ForgeConfigSpec.BooleanValue enableRegister;
  public final ForgeConfigSpec.BooleanValue enableChangePassword;
  public final ForgeConfigSpec.EnumValue<Language> language;
  public final ForgeConfigSpec.IntValue delay;
  public final ForgeConfigSpec.EnumValue<DataSource> dataSource;
  public final ForgeConfigSpec.ConfigValue<List<? extends String>> commandWhitelist;

  public AuthModConfig(final ForgeConfigSpec.Builder builder) {
    builder.comment("Server configuration settings").push("server");

    this.identifierRequired = builder
      .comment("Identifier must be provided for registration and authentication")
      .define("identifierRequired", false);

    this.enableLogin = builder
      .comment("Enable or disable the /login command. If disabled, the server will be opened to everyone).")
      .define("enableLogin", false);

    this.enableRegister = builder
      .comment("Enable or disable the /register command.")
      .define("enableRegister", false);

    this.enableChangePassword = builder
      .comment("Enable or disable the /changepassword command.")
      .define("enableChangePassword", false);

    this.delay = builder
      .comment("delay in seconds a player can authenticate before being automatically kicked from the server.")
      .defineInRange("delay", 60, 1, 1024);

    this.language = builder
      .comment("lang file to use")
      .defineEnum("language", Language.EN_US);

    this.dataSource = builder.comment("The way you want to store player's data, choose between 'database' or 'file'. If the strategy is unknown, the server will be open for everyone.")
      .defineEnum("dataSource", DataSource.FILE);

    String[] whitelist = { "register", "login", "logged", "help" };
    this.commandWhitelist = builder
      .comment("Whitelisted commands (can be used without being logged)")
      .defineList("whitelist",  Arrays.asList(whitelist), x -> true);
    builder.pop();

    this.database = new DatabaseConfig(builder);
    this.i18n = new I18nConfig(builder);
  }

  private static void afterLoadedConfig() {
    ServerLanguageMap.init(SERVER.language.get().name());
    ServerLanguageMap.replaceWith(SERVER.i18n.getTranslations());
    ExceptionToMessageMapper.init();
  }

  public static void load(Path config) {
    CommentedFileConfig file = CommentedFileConfig.builder(config).build();
    file.load();
    AuthMod.LOGGER.info("Config is loaded");
    serverSpec.setConfig(file);
  }

  @SubscribeEvent
  public static void onLoad(final ModConfig.Loading configEvent) {
    afterLoadedConfig();
    LogManager.getLogger().debug(FORGEMOD, "Loaded forge config file {}", configEvent.getConfig().getFileName());
  }

  @SubscribeEvent
  public static void onFileChange(final ModConfig.Reloading configEvent) {
    afterLoadedConfig();
    LogManager.getLogger().debug(FORGEMOD, "Forge config just got changed on the file system!");
  }

  public boolean enableAuthmod() {
    return this.enableLogin.get() || this.enableRegister.get();
  }

  public static final ForgeConfigSpec  serverSpec;
  public static final AuthModConfig SERVER;
  static {
    final Pair<AuthModConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(AuthModConfig::new);
    serverSpec = specPair.getRight();
    SERVER = specPair.getLeft();
  }

}
