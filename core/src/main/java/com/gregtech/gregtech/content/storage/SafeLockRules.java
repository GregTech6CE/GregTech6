package com.gregtech.gregtech.content.storage;
import java.util.List;import java.util.UUID;
/** Original key order, owner gate and key claim/copy/toggle shared across platforms. */
public final class SafeLockRules {
 private SafeLockRules(){} public static final int SLOTS=15;
 public static final List<String> KEY_MATERIALS=List.of("brass","bronze","copper","gold","iron","lead","plastic","platinum","silver","tin");
 public static boolean canOpen(boolean keyLocked,boolean opened,UUID owner,UUID player){return keyLocked?opened:owner==null||owner.equals(player);}
 public record Result(boolean accepted,long lockId,long itemId,boolean opened,boolean toggle){}
 public static Result useKey(long lockId,boolean opened,long supplied,long generated){
  if(supplied==0&&lockId!=0)return new Result(opened,lockId,opened?lockId:0,opened,false);
  if(supplied==0)supplied=generated;
  if(supplied==0)return new Result(false,lockId,0,opened,false);
  if(lockId==0)lockId=supplied;
  boolean matches=lockId==supplied;return new Result(matches,lockId,supplied,matches?!opened:opened,matches);
 }
}
