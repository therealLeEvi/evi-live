package com.evi.live;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Synthetic transport only. Does not connect to the user's bridge. */
public final class EviLiveDeliveryTest {
  static void set(Object target,String name,Object value)throws Exception {
    Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);
  }
  static Object get(Object target,String name)throws Exception {
    Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(target);
  }
  static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
  static final class Fake implements LocalTransport {
    int code=200;boolean fail;List<String> bodies=new ArrayList<>();
    public int send(String key,String json)throws IOException {
      bodies.add(json);if(fail)throw new IOException("synthetic disconnect");return code;
    }
  }
  @SuppressWarnings("unchecked")
  public static void main(String[] args)throws Exception {
    EviLivePlugin plugin=new EviLivePlugin();Fake fake=new Fake();
    set(plugin,"transport",fake);set(plugin,"running",true);set(plugin,"lifecycle",1L);
    set(plugin,"pluginKey","abcdef0123456789".repeat(4));
    ArrayDeque<String> queue=(ArrayDeque<String>)get(plugin,"queue");
    Method flush=EviLivePlugin.class.getDeclaredMethod("flush",long.class);flush.setAccessible(true);
    queue.add("synthetic-packet-one");fake.code=401;flush.invoke(plugin,1L);
    check(queue.size()==1,"Wrong key must retain pending packet");
    fake.code=200;fake.fail=true;flush.invoke(plugin,1L);
    check(queue.size()==1,"Disconnect must retain pending packet");
    fake.fail=false;flush.invoke(plugin,1L);
    check(queue.isEmpty(),"Successful retry must remove packet");
    check(fake.bodies.size()==3&&fake.bodies.stream().allMatch("synthetic-packet-one"::equals),"Retries must be byte-identical");
    queue.add("new-session-packet");set(plugin,"lifecycle",2L);flush.invoke(plugin,1L);
    check(queue.size()==1&&fake.bodies.size()==3,"Old sender generation must not send new session data");
    set(plugin,"running",false);flush.invoke(plugin,2L);
    check(queue.size()==1&&fake.bodies.size()==3,"Disabled plugin must not deliver");
    set(plugin,"running",true);
    set(plugin,"transport",(LocalTransport)(key,json)->{
      try {
        set(plugin,"pairingRevision",1L);
        queue.clear();queue.add("re-paired-session-packet");
      }catch(Exception ex){throw new IOException(ex);}
      return 200;
    });
    flush.invoke(plugin,2L);
    check(queue.size()==1&&"re-paired-session-packet".equals(queue.peek()),"Old pairing response must not consume replacement pairing queue");
    queue.clear();for(int i=0;i<512;i++)queue.add("queued-"+i);
    set(plugin,"account","synthetic-account");set(plugin,"session","old-session");
    Method enqueue=EviLivePlugin.class.getDeclaredMethod("enqueue",boolean.class);enqueue.setAccessible(true);enqueue.invoke(plugin,false);
    check(queue.isEmpty()&&get(plugin,"account")==null&&!"old-session".equals(get(plugin,"session")),"Queue overflow must clear unsafe coverage and create a new baseline");
    System.out.println("PASS: authentication failure, disconnect, exact retry, stale sender, disabled delivery, pairing replacement and overflow rebaseline");
  }
}
