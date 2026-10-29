package com.endlessmutation.command;

import java.util.List;
import java.util.UUID;

import com.endlessmutation.config.ModConfig;
import com.endlessmutation.entity.EntityHuntress;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

/** /endlessmutation <summon|info|remove|debugfeed [refeicoes] [energia]> (somente operadores). */
public class CommandEndless extends CommandBase {
    @Override public String getCommandName() { return "endlessmutation"; }
    @Override public int getRequiredPermissionLevel() { return 2; }
    @Override public String getCommandUsage(ICommandSender s) { return "/endlessmutation <summon|info|remove|debugfeed [refeicoes] [energia]>"; }

    private static void say(ICommandSender s, String m) { s.addChatMessage(new ChatComponentText(m)); }

    private EntityHuntress nearest(ICommandSender s) {
        World w = s.getEntityWorld();
        BlockPos p = s.getPosition();
        List<EntityHuntress> l = w.getEntitiesWithinAABB(EntityHuntress.class,
                AxisAlignedBB.fromBounds(p.getX() - 128, 0, p.getZ() - 128, p.getX() + 128, 256, p.getZ() + 128));
        EntityHuntress best = null; double bd = Double.MAX_VALUE;
        for (EntityHuntress e : l) {
            double d = e.getDistanceSq(p);
            if (d < bd) { bd = d; best = e; }
        }
        return best;
    }

    @Override
    public void processCommand(ICommandSender s, String[] a) throws CommandException {
        if (a.length < 1) throw new WrongUsageException(getCommandUsage(s));
        World w = s.getEntityWorld();
        String sub = a[0].toLowerCase();
        if (sub.equals("summon")) {
            BlockPos p = s.getPosition();
            EntityHuntress e = new EntityHuntress(w);
            e.setLocationAndAngles(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0f, 0f);
            w.spawnEntityInWorld(e);
            say(s, "Predadora Mutante invocada.");
        } else if (sub.equals("info")) {
            EntityHuntress e = nearest(s);
            say(s, e == null ? "Nenhuma Predadora a 128 blocos." : e.describe());
        } else if (sub.equals("remove")) {
            EntityHuntress e = nearest(s);
            if (e == null) say(s, "Nenhuma Predadora a 128 blocos.");
            else { e.forceRemove(); say(s, "Predadora removida."); }
        } else if (sub.equals("debugfeed")) {
            if (!ModConfig.enableDebugCommands) throw new CommandException("Habilite enableDebugCommands na configuracao (uso de teste; nao e alimentacao real).");
            EntityHuntress e = nearest(s);
            if (e == null) { say(s, "Nenhuma Predadora a 128 blocos."); return; }
            int n = a.length > 1 ? parseInt(a[1], 1, 100000) : 1;
            double en = a.length > 2 ? parseDouble(a[2], 0.001, 1.0e300) : 10.0;
            int ok = 0;
            for (int i = 0; i < n; i++) if (e.debugMeal(en)) ok++;
            say(s, "[DEBUG] " + ok + " refeicoes simuladas. " + e.describe());
        } else {
            throw new WrongUsageException(getCommandUsage(s));
        }
    }
}
