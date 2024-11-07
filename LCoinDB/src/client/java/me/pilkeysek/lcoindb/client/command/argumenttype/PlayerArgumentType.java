package me.pilkeysek.lcoindb.client.command.argumenttype;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class PlayerArgumentType implements ArgumentType<String> {
    public static final DynamicCommandExceptionType INVALID_NAME = new DynamicCommandExceptionType(o -> Text.literal("Invalid playername: " + o));

    public static PlayerArgumentType player() {
        return new PlayerArgumentType();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        int argBeginning = reader.getCursor();
        if(!reader.canRead()) {
            reader.skip();
        }
        while (reader.canRead() && (Character.isLetterOrDigit(reader.peek()) || reader.peek() == '-' || reader.peek() == '_')) {
            reader.skip();
        }

        String playername = reader.getString().substring(argBeginning, reader.getCursor());
        if(!playername.matches("[a-zA-Z0-9_-]+$")) {
           throw INVALID_NAME.createWithContext(reader, "Name contains non a-zA-Z0-9_- characters");
        }
        return playername;
    }

    public static <S> String getName(String name, CommandContext<S> context) {
        return context.getArgument(name, String.class);
    }

    @Override
    public <S>CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        final String remaining = builder.getRemaining();
        if(context.getSource() instanceof FabricClientCommandSource clientCommandSource) {
            for(AbstractClientPlayerEntity player : clientCommandSource.getWorld().getPlayers()) {
                String name = player.getNameForScoreboard();
                if(!name.equals(clientCommandSource.getPlayer().getNameForScoreboard()) && CommandSource.shouldSuggest(remaining, name)) {
                    builder.suggest(name);
                }
            }
        }
        return builder.buildFuture();
    }
}
