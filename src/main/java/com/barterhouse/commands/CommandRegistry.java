package com.barterhouse.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Registrador de comandos para /barter
 */
public class CommandRegistry {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("barter")
                .executes(ctx -> {
                    BarterCommand.execute(ctx.getSource(), new String[]{});
                    return 1;
                })
                .then(Commands.literal("create")
                    .executes(ctx -> {
                        BarterCommand.execute(ctx.getSource(), new String[]{"create"});
                        return 1;
                    })
                )
                .then(Commands.literal("list")
                    .executes(ctx -> {
                        BarterCommand.execute(ctx.getSource(), new String[]{"list"});
                        return 1;
                    })
                )
                .then(Commands.literal("accept")
                    .then(Commands.argument("id", StringArgumentType.word())
                        .executes(ctx -> {
                            BarterCommand.execute(ctx.getSource(), 
                                new String[]{"accept", StringArgumentType.getString(ctx, "id")});
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("cancel")
                    .then(Commands.argument("id", StringArgumentType.word())
                        .executes(ctx -> {
                            BarterCommand.execute(ctx.getSource(), 
                                new String[]{"cancel", StringArgumentType.getString(ctx, "id")});
                            return 1;
                        })
                    )
                )
        );
    }
}
