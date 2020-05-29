package io.chocorean.authmod.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.chocorean.authmod.core.*;
import io.chocorean.authmod.core.exception.AuthmodError;
import io.chocorean.authmod.event.Handler;
import io.chocorean.authmod.util.text.ServerTranslationTextComponent;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;

public class RegisterCommand implements CommandInterface, Command<CommandSource>  {

  protected final Handler handler;
  protected final GuardInterface guard;

  public RegisterCommand( Handler handler, GuardInterface guard) {
    this.handler = handler;
    this.guard = guard;
  }

  public LiteralArgumentBuilder<CommandSource> getCommandBuilder() {
    return Commands.literal("register").then(this.getParameters());
  }

  public RequiredArgumentBuilder<CommandSource, String> getParameters() {
    return Commands.argument("password", StringArgumentType.string())
      .then(Commands.argument("confirmation", StringArgumentType.string()).executes(this));
  }

  @Override
  public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
    return execute(context.getSource(), this.handler, this.guard,
      CommandInterface.toPayload(
        context.getSource().asPlayer(),
        StringArgumentType.getString(context, "password"),
        StringArgumentType.getString(context, "confirmation")));
  }

  /**
   * @return 1 if something goes wrong, 0 otherwise.
   */
  public static int execute(CommandSource source, Handler handler, GuardInterface guard, PayloadInterface payload) {
    try {
      PlayerEntity player = source.asPlayer();
      if (guard.register(payload) && !handler.isLogged(source.asPlayer())) {
        handler.authorizePlayer(player);
        source.sendFeedback(new ServerTranslationTextComponent("register.success"), true);
      }
      return 0;
    } catch (AuthmodError | CommandSyntaxException e) {
      source.sendFeedback(new ServerTranslationTextComponent(ExceptionToMessageMapper.getMessage(e), payload.getPlayer().getUsername()), false);
      return 1;
    }
  }
}

