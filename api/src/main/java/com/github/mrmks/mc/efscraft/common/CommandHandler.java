package com.github.mrmks.mc.efscraft.common;

import com.github.mrmks.mc.efscraft.common.packet.*;

import java.io.File;
import java.util.*;

public class CommandHandler<ENTITY, PLAYER extends ENTITY, SERVER, SENDER, WORLD> {

    private final Adaptor<ENTITY, PLAYER, SERVER, SENDER, WORLD> adaptor;
    private final ServerRegistryMap registry;
    private final String port, portVersion;
    private final Map<UUID, PacketHello.State> clients;
    public CommandHandler(Adaptor<ENTITY, PLAYER, SERVER, SENDER, WORLD> adaptor, File file, String port, String portVersion, Map<UUID, PacketHello.State> clients) {
        this.adaptor = adaptor;
        this.registry = new ServerRegistryMap(file);
        this.port = port;
        this.portVersion = portVersion;
        this.clients = clients;
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

    private void sendNearby(SERVER server, SENDER sender, Collection<PLAYER> targets, NetworkPacket message, float x, float y, float z, int dist) {

        int chunkX = floorInt(x) >> 4, chunkY = floorInt(y) >> 4, chunkZ = floorInt(z) >> 4;

        for (PLAYER player : targets) {
            if (player == null || clients.get(adaptor.getClientUUID(player)) != PacketHello.State.COMPLETE) continue;
            float[] pos = adaptor.getEntityPosAngle(player);
            if (pos == null || pos.length < 3) continue;

            int cx = floorInt(pos[0]) >> 4, cy = floorInt(pos[1]) >> 4, cz = floorInt(pos[2]) >> 4;

            if (inClamp(chunkX, cx, dist) && inClamp(chunkY, cy, dist) && inClamp(chunkZ, cz, 10))
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

    private static int parseInt(String p) throws CommandException {
        try {
            return Integer.parseInt(p);
        } catch (NumberFormatException e) {
            throw new CommandException("commands.generic.num.invalid", p);
        }
    }

    private static float parseFloat(String p, float base) throws CommandException {
        if (p.length() > 0) {

            if (p.charAt(0) == '~') {
                return base + (p.length() == 1 ? 0 : parseFloat(p.substring(1)));
            } else {
                return parseFloat(p);
            }

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
                    case "trigger": executeTrigger(subArgs, server, sender); break;
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
            return getListMatchLastArg(args, "play", "stop", "trigger", "clear", "reload", "version");
        } else if (args.length > 1) {
            String sub = args[0];
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

            switch (sub) {
                case "play":    return completePlay(subArgs, server, sender);
                case "trigger": return completeTrigger(subArgs, server, sender);
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
                    NetworkPacket message = new PacketBuilder(registry.get(effect))
                            .consumeOptions(options)
                            .buildPlayWith(effect, emitter, entityId);

                    WORLD world = adaptor.getEntityWorld(entity);
                    Collection<PLAYER> players = adaptor.getPlayersInWorld(server, sender, world);
                    int dist = adaptor.getViewDistance(world);

                    sendNearby(server, sender, players, message, pos[0], pos[1], pos[2], dist);

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

                    NetworkPacket message = new PacketBuilder(registry.get(effect))
                            .consumeOptions(options)
                            .buildPlayAt(effect, emitter, x, y, z, yaw, pitch);

                    Collection<PLAYER> players = adaptor.getPlayersInWorld(server, sender, world);
                    int dist = adaptor.getViewDistance(world);

                    sendNearby(server, sender, players, message, x, y, z, dist);
                }
            } else {
                throw new EffectNotFoundException(effect);
            }
        }
    }

    private List<String> completePlay(String[] args, SERVER server, SENDER sender) {
        if (args.length < 5) {
            return completeBasic(args, 0, server, sender);
        } else if (args.length < 10) {
            if ("at".equals(args[2])) return getListMatchLastArg(args, "~");
            else return Collections.emptyList();
        } else {
            return Collections.emptyList();
        }
    }

    private void executeTrigger(String[] args, SERVER server, SENDER sender) throws CommandException {
        // effek trigger 1 effect emitter on entity
        // effek trigger 1 effect emitter at world posx posy posz
        if (args.length < 4) {
            throw new WrongUsageException("commands.effek.trigger.usage");
        } else {
            final int triggerId = parseInt(args[0]);
            ActionOn actionOn = (effect, emitter, id, followings) -> new SPacketTrigger(effect, emitter, triggerId);
            ActionAt actionAt = (effect, emitter, posAngle, followings) -> new SPacketTrigger(effect, emitter, triggerId);

            try {
                executeBasic(args, 1, server, sender, false, actionAt, actionOn);
            } catch (WrongUsageException wue) {
                if (wue == WrongUsageException.PLACEHOLDER)
                    throw new WrongUsageException("commands.effek.trigger.usage");

                throw wue;
            }
        }
    }

    private List<String> completeTrigger(String[] args, SERVER server, SENDER sender) {
        if (args.length <= 1) {
            return getListMatchLastArg(args, "0", "1", "2", "3");
        } else if (args.length < 6) {
            return completeBasic(args, 1, server, sender);
        } else if (args.length < 9 && "at".equals(args[3])) {
            return getListMatchLastArg(args, "~");
        } else return Collections.emptyList();
    }

    private void executeStop(String[] args, SERVER server, SENDER sender) throws CommandException {
        if (args.length < 4) {
            throw new WrongUsageException("commands.effek.stop.usage");
        } else {

            ActionOn actionOn = (effect, emitter, entity, followings) -> new SPacketStop(effect, emitter);
            ActionAt actionAt = (effect, emitter, posAngle, followings) -> new SPacketStop(effect, emitter);

            try {
                executeBasic(args, 0, server, sender, false, actionAt, actionOn);
            } catch (WrongUsageException wue) {
                if (wue == WrongUsageException.PLACEHOLDER)
                    throw new WrongUsageException("commands.effek.stop.usage");

                throw wue;
            }
        }
    }

    private List<String> completeStop(String[] args, SERVER server, SENDER sender) {
        if (args.length < 5) {
            return completeBasic(args, 0, server, sender);
        } else if (args.length < 8 && "at".equals(args[2])) {
            return getListMatchLastArg(args, "~");
        } else {
            return Collections.emptyList();
        }
    }

    @FunctionalInterface
    private interface ActionAt {
        NetworkPacket accept(String effect, String emitter, float[] posAngle, String[] followings);
    }

    @FunctionalInterface
    private interface ActionOn {
        NetworkPacket accept(String effect, String emitter, int entity, String[] followings);
    }

    private void executeBasic(String[] args, int index, SERVER server, SENDER sender, boolean hasRot, ActionAt actionAt, ActionOn actionOn) throws CommandException {

        if (args.length < index + 4)
            throw WrongUsageException.PLACEHOLDER;

        String effect = args[index], emitter = args[index + 1], action = args[index + 2];

        if (!registry.isExist(effect))
            throw new EffectNotFoundException(effect);

        if ("on".equalsIgnoreCase(action)) {
            ENTITY entity = adaptor.findEntity(server, sender, args[index + 3]);
            float[] pos = entity == null ? null : adaptor.getEntityPosAngle(entity);

            if (pos == null || pos.length < 5)
                throw new EntityNotFoundException(args[index + 3]);

            int entityId = adaptor.getEntityId(entity);

            String[] followings = Arrays.copyOfRange(args, index + 4, args.length);

            NetworkPacket message = actionOn.accept(effect, emitter, entityId, followings);

            WORLD world = adaptor.getEntityWorld(entity);
            Collection<PLAYER> players = adaptor.getPlayersInWorld(server, sender, world);
            int dist = adaptor.getViewDistance(world);

            sendNearby(server, sender, players, message, pos[0], pos[1], pos[2], dist);
        } else if ("at".equalsIgnoreCase(action)) {

            if (args.length < index + 7)
                throw WrongUsageException.PLACEHOLDER;

            WORLD world = adaptor.findWorld(server, sender, args[index + 3]);
            if (world == null)
                throw new WorldNotFoundException(args[index + 3]);

            float x, y, z;
            float[] pos = adaptor.getSenderPosAngle(sender);
            if (pos == null || pos.length < 3) {
                x = parseFloat(args[index + 4]); y = parseFloat(args[index + 5]);  z = parseFloat(args[index + 6]);
            } else {
                x = parseFloat(args[index + 4], pos[0]); y = parseFloat(args[index + 5], pos[1]); z = parseFloat(args[index + 6], pos[2]);
            }

            NetworkPacket message;
            if (hasRot) {
                if (args.length < index + 9)
                    throw WrongUsageException.PLACEHOLDER;

                float yaw, pitch;
                if (pos == null || pos.length < 5) {
                    yaw = parseFloat(args[index + 7]); pitch = parseFloat(args[index + 8]);
                } else {
                    yaw = parseFloat(args[index + 7], pos[3]); pitch = parseFloat(args[index + 8], pos[4]);
                }

                float[] posAngle = {x, y, z, yaw, pitch};
                String[] followings = Arrays.copyOfRange(args, index + 9, args.length);
                message = actionAt.accept(effect, emitter, posAngle, followings);
            } else {
                float[] posAngle = {x, y, z, 0, 0};
                String[] followings = Arrays.copyOfRange(args, index + 7, args.length);
                message = actionAt.accept(effect, emitter, posAngle, followings);
            }

            Collection<PLAYER> players = adaptor.getPlayersInWorld(server, sender, world);
            int dist = adaptor.getViewDistance(world);

            sendNearby(server, sender, players, message, x, y, z, dist);
        } else {
            throw WrongUsageException.PLACEHOLDER;
        }
    }

    private List<String> completeBasic(String[] args, int index, SERVER server, SENDER sender) {
        if (args.length == index + 1) {
            return getListMatchLastArg(args, registry.keySets());
        } else if (args.length == index + 2) {
            return args[index + 1].isEmpty() ? Collections.singletonList("emitter") : Collections.emptyList();
        } else if (args.length == index + 3) {
            return getListMatchLastArg(args, "on", "at");
        } else if (args.length == index + 4) {
            if ("on".equals(args[index + 2])) {
                return getListMatchLastArg(args, adaptor.completePlayers(server));
            } else if ("at".equals(args[index + 2])) {
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
        UUID getClientUUID(PLAYER sender);
        void sendPacketTo(SERVER server, PLAYER player, NetworkPacket message);
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

        int getViewDistance(WORLD world);

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

        public static final WrongUsageException PLACEHOLDER = new WrongUsageException("<PLACEHOLDER>");

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
