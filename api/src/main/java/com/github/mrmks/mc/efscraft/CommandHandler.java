package com.github.mrmks.mc.efscraft;

import com.github.mrmks.mc.efscraft.packet.IMessage;
import com.github.mrmks.mc.efscraft.packet.SPacketClear;
import com.github.mrmks.mc.efscraft.packet.SPacketStop;

import java.io.File;
import java.util.*;

public class CommandHandler<ENTITY, PLAYER extends ENTITY, SERVER, SENDER, WORLD> {

    private final Adaptor<ENTITY, PLAYER, SERVER, SENDER, WORLD> adaptor;
    private final EffectMap registry;
    private final String port, portVersion;
    public CommandHandler(Adaptor<ENTITY, PLAYER, SERVER, SENDER, WORLD> adaptor, File file, String port, String portVersion) {
        this.adaptor = adaptor;
        this.registry = new EffectMap(file);
        this.port = port;
        this.portVersion = portVersion;
    }

    private static List<String> getListMatchLastArg(String[] args, Collection<String> collection) {
        String last = args[args.length - 1];
        ArrayList<String> list = new ArrayList<>();
        for (String str : collection)
            if (str.startsWith(last)) list.add(str);

        list.sort(String::compareTo);

        return list;
    }

    private static List<String> getListMatchLastArg(String[] args, String... collection) {
        return getListMatchLastArg(args, Arrays.asList(collection));
    }

    private void sendNearby(SERVER server, SENDER sender, Collection<PLAYER> targets, IMessage message, float x, float y, float z) {

        int chunkX = floorInt(x) >> 4, chunkY = floorInt(y) >> 4, chunkZ = floorInt(z) >> 4;

        for (PLAYER player : targets) {
            if (player == null || !adaptor.isClientValid(player)) continue;
            float[] pos = adaptor.getEntityPosAngle(player);
            if (pos == null || pos.length < 3) continue;

            int cx = floorInt(pos[0]) >> 4, cy = floorInt(pos[1]) >> 4, cz = floorInt(pos[2]) >> 4;

            if (inClamp(chunkX, cx, 10) && inClamp(chunkY, cy, 10) && inClamp(chunkZ, cz, 10))
                adaptor.sendPacketTo(server, player, message);
        }
    }

    private static int floorInt(float value) {
        return Math.round(value - 0.5f);
    }

    private static boolean inClamp(int a, int b, int d) {
        return Math.abs(a - b) <= Math.abs(d);
    }

    private static float parseFloat(String p) throws CommandException {
        try {
            return Float.parseFloat(p);
        } catch (NumberFormatException e) {
            throw new CommandException("commands.generic.num.invalid", p);
        }
    }

    private static float parseFloat(String p, float base) throws CommandException {
        if (p.length() > 0) {

            if (p.charAt(0) == '~') {
                p = p.substring(1);
            } else {
                base = 0;
            }

            return base + parseFloat(p);
        } else {
            throw new CommandException("commands.generic.num.invalid", p);
        }
    }

    public void dispatchExecute(String label, String[] args, SERVER server, SENDER sender) throws CommandException {
        if ("effek".equals(label) && adaptor.hasPermission(server, sender, "efscraft.command")) {
            if (args.length < 1) throw new WrongUsageException("commands.effek.usage");
            else {
                String sub = args[0];
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

                switch (sub) {
                    case "play": executePlay(subArgs, server, sender); break;
                    case "stop": executeStop(subArgs, server, sender); break;
                    case "clear": executeClear(subArgs, server, sender); break;
                    case "reload": executeReload(subArgs, server, sender); break;
                    case "version": executeVersion(subArgs, server, sender); break;
                    default: throw new WrongUsageException("commands.effek.usage");
                }
            }
        }
    }

    public List<String> dispatchComplete(String label, String[] args, SERVER server, SENDER sender) {
        if (!"effek".equals(label) || !adaptor.hasPermission(server, sender, "efscraft.command"))
            return Collections.emptyList();

        if (args.length == 1) {
            return getListMatchLastArg(args, "play", "stop", "clear", "reload", "version");
        } else if (args.length > 1) {
            String sub = args[0];
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

            switch (sub) {
                case "play":    return completePlay(subArgs, server, sender);
                case "stop":    return completeStop(subArgs, server, sender);
                case "clear":   return completeClear(subArgs, server, sender);
                case "reload":  return completeReload(subArgs, server, sender);
                case "version": return completeVersion(subArgs, server, sender);
                default: return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }
    }

    private void executePlay(String[] args, SERVER server, SENDER sender) throws CommandException {
        if (args.length < 4) {
            throw new WrongUsageException("commands.effek.play.usage");
        } else {
            String effect = args[0], emitter = args[1], action = args[2];

            if (registry.isExist(effect)) {
                if ("on".equals(action)) {
                    ENTITY entity = adaptor.findEntity(server, sender, args[3]);
                    float[] pos = entity == null ? null : adaptor.getEntityPosAngle(entity);

                    if (pos == null || pos.length < 5)
                        throw new EntityNotFoundException(args[3]);

                    int entityId = adaptor.getEntityId(entity);
                    String[] options = Arrays.copyOfRange(args, 4, args.length);

//                    IMessage message = registry.createPlayWith(effect, emitter, entityId, options);
                    IMessage message = new PacketBuilder(registry.getEffect(effect))
                            .consumeOptions(options)
                            .buildPlayWith(effect, emitter, entityId);

                    WORLD world = adaptor.getEntityWorld(entity);
                    Collection<PLAYER> players = adaptor.getPlayersInWorld(server, sender, world);

                    sendNearby(server, sender, players, message, pos[0], pos[1], pos[2]);

                } else if ("at".equals(action)) {
                    if (args.length < 9)
                        throw new CommandException("commands.effek.play.usage");

                    WORLD world = adaptor.findWorld(server, sender, args[3]);

                    if (world == null)
                        throw new WorldNotFoundException(args[3]);

                    float x, y, z, yaw, pitch;

                    float[] pos = adaptor.getSenderPosAngle(sender);
                    if (pos == null || pos.length < 5) {
                        x = parseFloat(args[4]); y = parseFloat(args[5]);  z = parseFloat(args[6]);
                        yaw = parseFloat(args[7]); pitch = parseFloat(args[8]);
                    } else {
                        x = parseFloat(args[4], pos[0]); y = parseFloat(args[5], pos[1]); z = parseFloat(args[6], pos[2]);
                        yaw = parseFloat(args[7], pos[3]); pitch = parseFloat(args[8], pos[4]);
                    }

                    String[] options = Arrays.copyOfRange(args, 9, args.length);

//                    IMessage message = registry.createPlayAt(effect, emitter, x, y, z, yaw, pitch, options);
                    IMessage message = new PacketBuilder(registry.getEffect(effect))
                            .consumeOptions(options)
                            .buildPlayAt(effect, emitter, x, y, z, yaw, pitch);

                    Collection<PLAYER> players = adaptor.getPlayersInWorld(server, sender, world);

                    sendNearby(server, sender, players, message, x, y, z);
                }
            } else {
                throw new EffectNotFoundException(effect);
            }
        }
    }

    private List<String> completePlay(String[] args, SERVER server, SENDER sender) {
        if (args.length == 1) {
            return getListMatchLastArg(args, registry.keySets());
        } else if (args.length == 2) {
            return args[1].isEmpty() ? Collections.singletonList("emitter") : Collections.emptyList();
        } else if (args.length == 3) {
            return getListMatchLastArg(args, "on", "at");
        } else if (args.length == 4) {
            if ("on".equals(args[2])) {
                return getListMatchLastArg(args, adaptor.completePlayers(server));
            } else if ("at".equals(args[2])) {
                return getListMatchLastArg(args, adaptor.completeWorlds(server));
            } else {
                return Collections.emptyList();
            }
        } else return Collections.emptyList();
    }

    private void executeStop(String[] args, SERVER server, SENDER sender) throws CommandException {
        if (args.length < 4) {
            throw new WrongUsageException("commands.effek.stop.usage");
        } else {
            String effect = args[0], emitter = args[1], action = args[2];

            if (registry.isExist(effect)) {
                if ("on".equals(action)) {
                    ENTITY entity = adaptor.findEntity(server, sender, args[3]);
                    float[] pos = entity == null ? null : adaptor.getEntityPosAngle(entity);
                    if (pos == null || pos.length < 5)
                        throw new EntityNotFoundException(args[3]);

                    Collection<PLAYER> players = adaptor.getPlayersInWorld(server, sender, adaptor.getEntityWorld(entity));

                    IMessage message = new SPacketStop(effect, emitter);

                    sendNearby(server, sender, players, message, pos[0], pos[1], pos[2]);
                } else if ("at".equals(action)) {

                    if (args.length < 7)
                        throw new CommandException("commands.effek.stop.usage");

                    WORLD world = adaptor.findWorld(server, sender, args[3]);

                    if (world == null)
                        throw new WorldNotFoundException(args[3]);

                    float x, y, z;

                    float[] pos = adaptor.getSenderPosAngle(sender);
                    if (pos == null || pos.length < 5) {
                        x = parseFloat(args[4]); y = parseFloat(args[5]);  z = parseFloat(args[6]);
                    } else {
                        x = parseFloat(args[4], pos[0]); y = parseFloat(args[5], pos[1]); z = parseFloat(args[6], pos[2]);
                    }

                    IMessage message = new SPacketStop(effect, emitter);
                    Collection<PLAYER> players = adaptor.getPlayersInWorld(server, sender, world);

                    sendNearby(server, sender, players, message, x, y, z);
                }
            } else {
                throw new EffectNotFoundException(effect);
            }
        }
    }

    private List<String> completeStop(String[] args, SERVER server, SENDER sender) {
        if (args.length == 1) {
            return getListMatchLastArg(args, registry.keySets());
        } else if (args.length == 2) {
            return args[1].isEmpty() ? Collections.singletonList("emitter") : Collections.emptyList();
        } else if (args.length == 3) {
            return getListMatchLastArg(args, "on", "at");
        } else if (args.length == 4) {
            if ("on".equals(args[2])) {
                return getListMatchLastArg(args, adaptor.completePlayers(server));
            } else if ("at".equals(args[2])) {
                return getListMatchLastArg(args, adaptor.completeWorlds(server));
            } else {
                return Collections.emptyList();
            }
        } else return Collections.emptyList();
    }

    private void executeClear(String[] args, SERVER server, SENDER sender) throws CommandException {
        if (args.length < 1) {
            throw new WrongUsageException("commands.effek.clear.usage");
        } else {
            PLAYER player = adaptor.findPlayer(server, sender, args[0]);
            adaptor.sendPacketTo(server, player, new SPacketClear());
        }
    }

    private List<String> completeClear(String[] args, SERVER server, SENDER sender) {
        if (args.length == 1) {
            return getListMatchLastArg(args, adaptor.completePlayers(server));
        } else return Collections.emptyList();
    }

    private void executeReload(String[] args, SERVER server, SENDER sender) throws CommandException {
        registry.reload(() -> adaptor.sendMessage(sender, "commands.effek.reload.success", new Object[0], true));
    }

    private List<String> completeReload(String[] args, SERVER server, SENDER sender) {
        return Collections.emptyList();
    }

    private void executeVersion(String[] args, SERVER server, SENDER sender) throws CommandException {
        adaptor.sendMessage(sender, "commands.effek.version.display", new Object[]{portVersion, Integer.toString(Constants.PROTOCOL_VERSION), port}, false);
    }

    private List<String> completeVersion(String[] args, SERVER server, SENDER sender) {
        return Collections.emptyList();
    }

    public interface Adaptor<ENTITY, PLAYER extends ENTITY, SERVER, SENDER, WORLD> {
        boolean hasPermission(SERVER server, SENDER sender, String node);
        boolean isClientValid(PLAYER sender);
        void sendPacketTo(SERVER server, PLAYER player, IMessage message);
        PLAYER findPlayer(SERVER server, SENDER sender, String toFound) throws CommandException;
        ENTITY findEntity(SERVER server, SENDER sender, String toFound) throws CommandException;
        Collection<PLAYER> getPlayersInWorld(SERVER server, SENDER sender, WORLD world);
        float[] getSenderPosAngle(SENDER sender); // or null if sender has no position.
        int getEntityId(ENTITY entity);
        float[] getEntityPosAngle(ENTITY entity);

        WORLD getEntityWorld(ENTITY entity);
        WORLD findWorld(SERVER server, SENDER sender, String str) throws CommandException;

        Collection<String> completePlayers(SERVER server);
        Collection<String> completeWorlds(SERVER server);

        void sendMessage(SENDER player, String msg, Object[] objects, boolean schedule);

    }

    public static class CommandException extends Exception {

        private final Object[] params;
        protected CommandException(String msg, Object... params) {
            super(msg);
            this.params = params;
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }

        public Object[] getParams() {
            return params;
        }
    }

    public static class WrongUsageException extends CommandException {
        WrongUsageException(String msg, Object... params) {
            super(msg, params);
        }
    }

    private static class WorldNotFoundException extends CommandException {
        WorldNotFoundException(String world) {
            super("commands.effek.world.notFound", world);
        }
    }

    private static class EffectNotFoundException extends CommandException {
        EffectNotFoundException(String effect) {
            super("commands.effek.effect.notFound", effect);
        }
    }

    private static class EntityNotFoundException extends CommandException {
        EntityNotFoundException(String entity) {
            super("commands.generic.entity.notFound", entity);
        }
    }

}
