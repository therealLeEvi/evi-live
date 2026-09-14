package com.evi.live;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import javax.swing.SwingUtilities;

/** Observes GE state only. No menus, clicks, scripts, packets or trading actions. */
@PluginDescriptor(name="EVI Live (Local)",description="Passively sends GE snapshots to your local EVI bridge",tags={"grand exchange","evi"},enabledByDefault=false)
public class EviLivePlugin extends Plugin {
  @Inject private Client client;
  @Inject private ConfigManager configManager;
  @Inject private ClientToolbar toolbar;
  @Inject private ClientThread clientThread;
  private EviLivePanel panel;
  private NavigationButton navigation;
  private Path pairingPath;
  private volatile boolean running;
  private volatile long lifecycle;
  private long pairingRevision;
  private final Gson gson=new Gson();
  private final ArrayDeque<String> queue=new ArrayDeque<>();
  private LocalTransport transport=new LocalTransport.Http();
  private ScheduledExecutorService sender;
  private volatile String pluginKey;
  private String salt,profile,account,session,economy;
  private long seq;
  private int warmTicks,ticks;
  private boolean ready;
  private final Offer[] slots=new Offer[8];

  @Override protected void startUp() throws Exception {
    Path dir=RuneLite.RUNELITE_DIR.toPath().resolve("evi-live");
    Files.createDirectories(dir);
    pairingPath=dir.resolve("plugin-key.txt");
    pluginKey=null;
    String pairingStatus="Not paired. Paste your RuneLite plugin key below.";
    try {
      if(Files.exists(pairingPath))pluginKey=PairingKey.normalize(Files.readString(pairingPath,StandardCharsets.UTF_8));
      if(pluginKey!=null)pairingStatus="Paired locally. Log in to begin observing offers.";
    } catch(Exception ex){pairingStatus="The saved pairing key is empty, invalid, or unreadable. Paste a new key below.";}
    Path saltFile=dir.resolve("identity-salt.txt");
    if(!Files.exists(saltFile))Files.writeString(saltFile,UUID.randomUUID().toString(),StandardCharsets.UTF_8);
    salt=Files.readString(saltFile,StandardCharsets.UTF_8).trim();
    if(salt.isEmpty())throw new IllegalStateException("EVI identity salt is empty; restore it from your local backup.");
    Runnable createPanel=()->{
      panel=new EviLivePanel(this::pair);
      navigation=NavigationButton.builder().tooltip("EVI Live").icon(EviLivePanel.icon()).panel(panel).priority(8).build();
      toolbar.addNavigation(navigation);
    };
    if(SwingUtilities.isEventDispatchThread())createPanel.run();else SwingUtilities.invokeAndWait(createPanel);
    status(pairingStatus);
    reset();
    final long generation=++lifecycle;
    running=true;
    sender=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"evi-local-sender");t.setDaemon(true);return t;});
    sender.scheduleWithFixedDelay(()->flush(generation),0,1,TimeUnit.SECONDS);
  }
  @Override protected void shutDown() {
    running=false;
    ++lifecycle;
    if(sender!=null)sender.shutdownNow();
    if(navigation!=null)toolbar.removeNavigation(navigation);
    synchronized(queue){queue.clear();}
    reset();
  }
  private void status(String message){if(panel!=null)panel.status(message);}
  private void pair(String entered) {
    try {
      final long generation=lifecycle;
      String key=PairingKey.normalize(entered);
      Files.writeString(pairingPath,key,StandardCharsets.UTF_8);
      clientThread.invokeLater(()->{
        if(!running || generation!=lifecycle)return;
        synchronized(queue){pluginKey=key;++pairingRevision;queue.clear();}
        reset();
        status("Pairing saved. Log in; connection is checked when the first snapshot is sent.");
      });
    }catch(IllegalArgumentException ex){status(ex.getMessage());}
    catch(Exception ex){status("Could not save the key. Check write access to .runelite/evi-live.");}
  }
  private void reset(){ready=false;warmTicks=0;ticks=0;profile=null;account=null;economy=null;session=UUID.randomUUID().toString();seq=0;Arrays.fill(slots,null);}

  @Subscribe public void onGameStateChanged(GameStateChanged e) {
    if(e.getGameState()==GameState.LOGIN_SCREEN || e.getGameState()==GameState.HOPPING || e.getGameState()==GameState.CONNECTION_LOST) {
      if(ready)enqueue(false);
      reset();
    }
  }
  @Subscribe public void onGameTick(GameTick e) {
    if(!running || pluginKey==null || client.getGameState()!=GameState.LOGGED_IN)return;
    String current=configManager.getRSProfileKey();
    if(current==null)return;
    String currentEconomy=EconomyScope.of(client.getWorldType());
    if(profile!=null&&(!profile.equals(current)||!currentEconomy.equals(economy))){if(ready)enqueue(false);reset();}
    if(!ready) {
      if(++warmTicks<5)return; // Initial login emits temporary EMPTY slots. Never treat these as trades.
      try {
        profile=current;
        economy=currentEconomy;
        // Ordinary worlds retain the existing account pseudonym for compatibility.
        String identity=salt+":"+profile+(economy.isEmpty()?"":":economy:"+economy);
        byte[] hash=MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8));
        StringBuilder h=new StringBuilder();for(byte b:hash)h.append(String.format("%02x",b&255));account=h.toString();
        GrandExchangeOffer[] offers=client.getGrandExchangeOffers();
        if(offers==null || offers.length!=8)return;
        for(int i=0;i<8;i++)slots[i]=capture(i,offers[i],null,false);
        ready=true;enqueue(true);
      } catch(Exception ex){reset();}
    } else if(++ticks%15==0)enqueue(true);
  }
  @Subscribe public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged e) {
    if(!ready||client.getGameState()!=GameState.LOGGED_IN||!profile.equals(configManager.getRSProfileKey())||!EconomyScope.of(client.getWorldType()).equals(economy))return;
    int i=e.getSlot();if(i<0||i>=8||e.getOffer()==null)return;
    slots[i]=capture(i,e.getOffer(),slots[i],true);
    enqueue(true);
  }
  private Offer capture(int i,GrandExchangeOffer o,Offer old,boolean observing) {
    Offer n=new Offer();n.slot=i;n.state=o==null?"EMPTY":o.getState().name();
    if(!"EMPTY".equals(n.state)) {
      n.itemId=o.getItemId();n.price=o.getPrice();n.total=o.getTotalQuantity();n.filled=o.getQuantitySold();n.spent=o.getSpent();
      n.name=client.getItemDefinition(n.itemId).getName();
    }
    boolean same=old!=null&&old.itemId==n.itemId&&old.price==n.price&&old.total==n.total&&
      old.filled<=n.filled&&old.spent<=n.spent&&buy(old.state)==buy(n.state)&&
      !(terminal(old)&&!terminal(n));
    n.offerId=same?old.offerId:UUID.randomUUID().toString();
    n.knownStart=same?old.knownStart:(observing&&old!=null&&"EMPTY".equals(old.state)&&n.filled==0&&!"EMPTY".equals(n.state));
    return n;
  }
  private static boolean buy(String s){return s.equals("BUYING")||s.equals("BOUGHT")||s.equals("CANCELLED_BUY");}
  private static boolean terminal(Offer o){return o.state.equals("BOUGHT")||o.state.equals("SOLD")||o.state.startsWith("CANCELLED")||(o.total>0&&o.total==o.filled);}
  private void enqueue(boolean loggedIn) {
    if(account==null)return;
    Packet p=new Packet();p.session=session;p.account=account;p.seq=++seq;p.ts=System.currentTimeMillis();p.loggedIn=loggedIn;
    if(loggedIn)p.offers.addAll(Arrays.asList(slots));
    String json=gson.toJson(p); // immutable snapshot created on the client thread
    synchronized(queue) {
      if(queue.size()>=512) {
        queue.clear();reset(); // Conservative rebaseline after data loss; do not fabricate complete trades.
        System.err.println("EVI Live: local delivery queue overflow; observation restarted. Review missing trades manually.");
        status("Delivery queue filled while disconnected. Observation restarted; review missing trades manually.");
        return;
      }
      queue.add(json);
    }
  }
  private void flush(long generation) {
    for(int count=0;count<32&&running&&generation==lifecycle&&!Thread.currentThread().isInterrupted();count++) {
      String json,key;long revision;
      synchronized(queue){json=queue.peek();key=pluginKey;revision=pairingRevision;}if(json==null)return;
      try {
        int status=transport.send(key,json);
        if(!running || generation!=lifecycle)return;
        synchronized(queue){if(revision!=pairingRevision)return;}
        if(status!=200){status(status==401?"Bridge rejected the key. Paste this bridge's RuneLite plugin key.":"Bridge returned HTTP "+status+". Pending observations retained for retry.");return;}
        synchronized(queue){if(json.equals(queue.peek()))queue.remove();}
        status("Connected to the local bridge. Last delivery: "+java.time.LocalTime.now().withNano(0));
      } catch(Exception ignored){if(running&&generation==lifecycle)status("Bridge unavailable. Start EVI; queued observations will retry automatically.");return;}
    }
  }
  static class Offer {int slot,itemId,price,total,filled,spent;String offerId,state,name="";boolean knownStart;}
  static class Packet {int version=1;String session,account;long seq,ts;boolean loggedIn;List<Offer> offers=new ArrayList<>();}
}
