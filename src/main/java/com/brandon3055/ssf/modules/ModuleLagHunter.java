package com.brandon3055.ssf.modules;

import com.brandon3055.ssf.LogHelper;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Level;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by brandon3055 on 27/11/2016.
 */
public class ModuleLagHunter extends SSModuleBase {

    private MonitoredEventBus eventBus = new MonitoredEventBus();
    private static MonitorThread monitor = new MonitorThread();
    private static int timeLimit = 700;

    public ModuleLagHunter() {
        super("lagHunter", "This module monitors the event system for anything that is potentially freezing the game." + "\nIf it detects that an event it taking too long it will print the current stack trace to the console." + "\nThis can then be used to figure out whats causing the issue." + "\nWarning! This is a very hacky module which may break things so it is disabled be default!");
    }

    @Override
    public void loadConfig(Configuration config) {
        timeLimit = config.get("LagHunter", "timeLimit", timeLimit, "The number of milliseconds to wait before reporting an issue.").getInt(timeLimit);
    }

    @Override
    public void initialize() {
        super.initialize();

        monitor.start();

        try {
            LogHelper.info("Here Goes Nothing!");
            LogHelper.info("Attempting EventBus injection in 3... 2... 1...");
            injectCustomBus();
            LogHelper.info("Success!!!");
        }
        catch (Exception e) {
            LogHelper.info("Well Crap....");
            e.printStackTrace();
        }
    }

    private void injectCustomBus() throws Exception {
        Field busField = ReflectionHelper.findField(MinecraftForge.class, "EVENT_BUS");
        Field fmlBusField = ReflectionHelper.findField(FMLCommonHandler.class, "eventBus");
        Field listenersField = ReflectionHelper.findField(EventBus.class, "listeners");
        Field listenerOwnersField = ReflectionHelper.findField(EventBus.class, "listenerOwners");
        Field busIDField = ReflectionHelper.findField(EventBus.class, "busID");
        Field exceptionHandlerField = ReflectionHelper.findField(EventBus.class, "exceptionHandler");

        busField.setAccessible(true);
        fmlBusField.setAccessible(true);
        listenersField.setAccessible(true);
        listenerOwnersField.setAccessible(true);
        busIDField.setAccessible(true);
        exceptionHandlerField.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(busField, busField.getModifiers() & ~Modifier.FINAL);

        eventBus.listeners = (ConcurrentHashMap<Object, ArrayList<IEventListener>>) listenersField.get(MinecraftForge.EVENT_BUS);
        eventBus.listenerOwners = (Map<Object, ModContainer>) listenerOwnersField.get(MinecraftForge.EVENT_BUS);
        eventBus.busID = busIDField.getInt(MinecraftForge.EVENT_BUS);
        eventBus.exceptionHandler = (IEventExceptionHandler) exceptionHandlerField.get(MinecraftForge.EVENT_BUS);

        busField.set(null, eventBus);

        try {
            fmlBusField.set(FMLCommonHandler.instance(), eventBus);
        }
        catch (Exception e) {
            busField.set(FMLCommonHandler.instance().bus(), eventBus);
        }
    }

    private static class MonitoredEventBus extends EventBus {

        public ConcurrentHashMap<Object, ArrayList<IEventListener>> listeners;
        public Map<Object, ModContainer> listenerOwners;

        public int busID = 0;
        public IEventExceptionHandler exceptionHandler;

        @Override
        public boolean post(Event event) {
            boolean server = FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER;

            IEventListener[] listeners = event.getListenerList().getListeners(busID);
            int index = 0;
            try {
                for (; index < listeners.length; index++) {
                    if (server) {
                        monitor.startMonitoring(Thread.currentThread(), event, listeners[index]);
                    }
                    listeners[index].invoke(event);
                    if (server) {
                        monitor.stopMonitoring();
                    }
                }
            }
            catch (Throwable throwable) {
                exceptionHandler.handleException(this, event, listeners, index, throwable);
                Throwables.propagate(throwable);
            }
            return (event.isCancelable() ? event.isCanceled() : false);
        }

        @Override
        public void register(Object target) {
            if (listeners.containsKey(target)) {
                return;
            }

            ModContainer activeModContainer = Loader.instance().activeModContainer();
            if (activeModContainer == null) {
                FMLLog.log(Level.ERROR, new Throwable(), "Unable to determine registrant mod for %s. This is a critical error and should be impossible", target);
                activeModContainer = Loader.instance().getMinecraftModContainer();
            }
            listenerOwners.put(target, activeModContainer);
            boolean isStatic = target.getClass() == Class.class;
            @SuppressWarnings("unchecked") Set<? extends Class<?>> supers = isStatic ? Sets.newHashSet((Class<?>) target) : TypeToken.of(target.getClass()).getTypes().rawTypes();
            for (Method method : (isStatic ? (Class<?>) target : target.getClass()).getMethods()) {
                if (isStatic && !Modifier.isStatic(method.getModifiers())) continue;
                else if (!isStatic && Modifier.isStatic(method.getModifiers())) continue;

                for (Class<?> cls : supers) {
                    try {
                        Method real = cls.getDeclaredMethod(method.getName(), method.getParameterTypes());
                        if (real.isAnnotationPresent(SubscribeEvent.class)) {
                            Class<?>[] parameterTypes = method.getParameterTypes();
                            if (parameterTypes.length != 1) {
                                throw new IllegalArgumentException("Method " + method + " has @SubscribeEvent annotation, but requires " + parameterTypes.length + " arguments.  Event handler methods must require a single argument.");
                            }

                            Class<?> eventType = parameterTypes[0];

                            if (!Event.class.isAssignableFrom(eventType)) {
                                throw new IllegalArgumentException("Method " + method + " has @SubscribeEvent annotation, but takes a argument that is not an Event " + eventType);
                            }

                            register(eventType, target, real, activeModContainer);
                            break;
                        }
                    }
                    catch (NoSuchMethodException e) {
                        ;
                    }
                }
            }
        }

        private void register(Class<?> eventType, Object target, Method method, final ModContainer owner) {
            try {
                Constructor<?> ctr = eventType.getConstructor();
                ctr.setAccessible(true);
                Event event = (Event) ctr.newInstance();
                final ASMEventHandler asm = new ASMEventHandler(target, method, owner, IGenericEvent.class.isAssignableFrom(eventType));

                IEventListener listener = asm;
                if (IContextSetter.class.isAssignableFrom(eventType)) {
                    listener = new IEventListener() {
                        @Override
                        public void invoke(Event event) {
                            ModContainer old = Loader.instance().activeModContainer();
                            Loader.instance().setActiveModContainer(owner);
                            asm.invoke(event);
                            Loader.instance().setActiveModContainer(old);
                        }
                    };
                }

                event.getListenerList().register(busID, asm.getPriority(), listener);

                ArrayList<IEventListener> others = listeners.get(target);
                if (others == null) {
                    others = new ArrayList<IEventListener>();
                    listeners.put(target, others);
                }
                others.add(listener);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void unregister(Object object) {
            ArrayList<IEventListener> list = listeners.remove(object);
            if (list == null) return;
            for (IEventListener listener : list) {
                ListenerList.unregisterAll(busID, listener);
            }
        }

        @Override
        public void handleException(EventBus bus, Event event, IEventListener[] listeners, int index, Throwable throwable) {
            FMLLog.log(Level.ERROR, throwable, "Exception caught during firing event %s:", event);
            FMLLog.log(Level.ERROR, "Index: %d Listeners:", index);
            for (int x = 0; x < listeners.length; x++) {
                FMLLog.log(Level.ERROR, "%d: %s", x, listeners[x]);
            }
        }
    }

    private static class MonitorThread extends Thread {

        private volatile boolean isMonitoring = false;
        private volatile long startTime = 0;
        private volatile long repeatTime = -1;
        private Thread monitoring = null;
        private IEventListener listener = null;
        private Event event = null;

        public MonitorThread() {
            setDaemon(true);
            setName("SSF:LagMonitor");
        }

        @Override
        public void run() {
            super.run();

            while (true) {
                if (isMonitoring) {
                    checkTime();
                }

                try {
                    Thread.sleep(1);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void checkTime() {

            if (repeatTime != -1 && System.currentTimeMillis() - repeatTime > timeLimit && monitoring != null) {
                StringBuilder builder = new StringBuilder();
                builder.append("\nFollowup: Handler has now taken longer than " + (System.currentTimeMillis() - startTime) + "ms to execute...\n");
                builder.append(  "---------------------------------------------------------------------------------------------------------------------------------------\n");

                StackTraceElement[] stackTrace = monitoring.getStackTrace();
                int i = 0;
                for (StackTraceElement element : stackTrace) {
                    i++;
                    builder.append(element+"\n");
                    if (element.toString().contains("com.brandon3055.ssf.modules.ModuleLagHunter$MonitoredEventBus.post")) {
                        builder.append(" +" + (stackTrace.length - i) + " more...\n");
                        break;
                    }
                }

                builder.append(  "======================================================================================================================================\n");
                LogHelper.warn(builder.toString());
                repeatTime = System.currentTimeMillis();
                return;
            }
            else if (repeatTime != -1) {
                return;
            }

            if (System.currentTimeMillis() - startTime > timeLimit && monitoring != null) {
                StringBuilder builder = new StringBuilder();

                builder.append("\n+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=\n");
                builder.append(  "======================================================================================================================================\n");
                //LogHelper.warn("LagHunter has detected that an event handler is taking more than " + timeLimit + "ms to execute!");
                builder.append("Problematic EventHandler found whilst firing an event!\n");
                builder.append("Handler is taking longer than " + timeLimit + "ms to execute.\n");
                builder.append("Event Class: " + event.getClass().getCanonicalName()+"\n");

                String lString = listener.toString();

                builder.append("EventHandler class: " + lString.substring(5, lString.indexOf("@"))+"\n");
                lString = lString.substring(lString.lastIndexOf(" ") + 1);
                builder.append("EventHandler method: " + lString.substring(0, lString.indexOf("("))+"\n");
                builder.append("    " + lString.substring(lString.indexOf("("))+"\n");

                builder.append(  "---------------------------------------------------------------------------------------------------------------------------------------\n");

                StackTraceElement[] stackTrace = monitoring.getStackTrace();
                for (StackTraceElement element : stackTrace) {
                    builder.append(element+"\n");
                }

                builder.append(  "======================================================================================================================================\n");
                builder.append(  "+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=\n");
                LogHelper.warn(builder.toString());
                repeatTime = System.currentTimeMillis();
            }
        }

        public synchronized void startMonitoring(Thread monitoring, Event event, IEventListener listener) {
            this.monitoring = monitoring;
            this.event = event;
            this.listener = listener;
            startTime = System.currentTimeMillis();
            isMonitoring = true;
            repeatTime = -1;
        }

        public synchronized void stopMonitoring() {
            isMonitoring = false;
            repeatTime = -1;
        }
    }
}
