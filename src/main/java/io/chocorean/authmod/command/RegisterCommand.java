package io.chocorean.authmod.command;

import io.chocorean.authmod.AuthMod;
import io.chocorean.authmod.event.Handler;
import io.chocorean.authmod.exception.InvalidEmailException;
import io.chocorean.authmod.exception.PlayerAlreadyExistException;
import io.chocorean.authmod.exception.RegistrationException;
import io.chocorean.authmod.exception.WrongPasswordConfirmation;
import io.chocorean.authmod.guard.datasource.IDataSourceStrategy;
import io.chocorean.authmod.guard.payload.RegistrationPayload;
import io.chocorean.authmod.guard.registration.Registrator;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import org.apache.logging.log4j.Logger;

public class RegisterCommand implements ICommand {
  private static final Logger LOGGER = AuthMod.LOGGER;
  private final List<String> aliases;
  private final Registrator registrator;
  private final Handler handler;
  private final boolean emailRequired;

  public RegisterCommand(Handler handler, IDataSourceStrategy strategy, boolean emailRequired) {
    this.handler = handler;
    aliases = new ArrayList<>();
    aliases.add("reg");
    this.registrator = new Registrator(strategy);
    this.emailRequired = emailRequired;
  }

  public RegisterCommand(Handler handler, IDataSourceStrategy strategy) {
    this(handler, strategy, false);
  }

  @Override
  public String getName() {
    return "register";
  }

  @Override
  public String getUsage(ICommandSender sender) {
    return this.handler.getMessage(this.getName() + ".usage").getUnformattedComponentText();
  }

  @Override
  public List<String> getAliases() {
    return aliases;
  }

  @Override
  public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
    EntityPlayer player = (EntityPlayer) sender;
    LOGGER.info(player.getDisplayNameString() + " is registering");
    if (args.length == (this.emailRequired ? 3 : 2)) {
      if (!this.handler.isLogged(player)) {
        RegistrationPayload payload = this.createPayload(player, args);
        try {
          this.registrator.register(payload);
          this.handler.authorizePlayer(player);
          sender.sendMessage(this.handler.getMessage(this.getName() + ".success"));
        } catch (InvalidEmailException e) {
          sender.sendMessage(this.handler.getMessage(this.getName() + ".invalidEmail"));
        } catch (PlayerAlreadyExistException e) {
          sender.sendMessage(this.handler.getMessage(this.getName() + ".exist"));
        } catch (WrongPasswordConfirmation e) {
          sender.sendMessage(this.handler.getMessage(this.getName() + ".wrongPasswordConfirmation"));
        } catch (RegistrationException e) {
          LOGGER.error(e.getMessage());
          sender.sendMessage(this.handler.getMessage("error"));
        }
      } else {
        sender.sendMessage(new TextComponentString(this.getUsage(sender)));
      }
    }
  }

  @Override
  public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
    return true;
  }

  @Override
  public List<String> getTabCompletions(
      MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
    return new ArrayList<>();
  }

  @Override
  public boolean isUsernameIndex(String[] args, int index) {
    return true;
  }

  @Override
  public int compareTo(ICommand iCommand) {
    return this.getName().compareTo(iCommand.getName());
  }

  private RegistrationPayload createPayload(EntityPlayer player, String[] args) {
    RegistrationPayload payload = new RegistrationPayload();
    payload.setEmailRequired(this.emailRequired);
    payload.setEmail(this.emailRequired ? args[0] : null);
    payload.setPassword(this.emailRequired ? args[1] : args[0]);
    payload.setPasswordConfirmation(this.emailRequired ? args[2] : args[1]);
    payload.setUsername(player.getDisplayNameString());
    return payload.setUuid(EntityPlayer.getUUID(player.getGameProfile()).toString());
  }
}
